package com.stephen.cloud.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.api.log.client.LogFeignClient;
import com.stephen.cloud.api.log.model.dto.login.UserLoginLogAddRequest;
import com.stephen.cloud.api.log.model.enums.LoginStatusEnum;
import com.stephen.cloud.api.log.model.enums.LoginTypeEnum;
import com.stephen.cloud.api.user.model.dto.UserQueryRequest;
import com.stephen.cloud.api.user.model.enums.UserRoleEnum;
import com.stephen.cloud.api.user.model.vo.LoginUserVO;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.common.cache.model.TimeModel;
import com.stephen.cloud.common.cache.utils.CacheUtils;
import com.stephen.cloud.common.cache.utils.lock.LockUtils;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.constants.CommonConstant;
import com.stephen.cloud.common.constants.UserConstant;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.mysql.utils.SqlUtils;
import com.stephen.cloud.common.auth.utils.SecurityUtils;
import com.stephen.cloud.common.utils.IpUtils;
import com.stephen.cloud.common.utils.RegexUtils;
import com.stephen.cloud.user.config.WxAppProperties;
import com.stephen.cloud.user.convert.UserConvert;
import com.stephen.cloud.user.mapper.UserMapper;
import com.stephen.cloud.user.model.dto.UserLoginLogRecordRequest;
import com.stephen.cloud.user.model.entity.User;
import com.stephen.cloud.user.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.stephen.cloud.user.config.AppleProperties;
import com.stephen.cloud.api.user.model.dto.UserAppleLoginRequest;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.http.HttpUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import cn.hutool.core.util.RandomUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Resource
    private WxMaService wxMaService;

    @Resource
    private CacheUtils cacheUtils;

    @Resource
    private LogFeignClient logFeignClient;


    @Resource
    private LockUtils lockUtils;

    @Resource
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Resource
    private WxAppProperties wxAppProperties;

    @Resource
    private AppleProperties appleProperties;

    /**
     * 校验数据
     *
     * @param user user
     * @param add  对创建的数据进行校验
     */
    @Override
    public void validUser(User user, boolean add) {
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userName = user.getUserName();
        String userPhone = user.getUserPhone();
        String userProfile = user.getUserProfile();
        String userEmail = user.getUserEmail();

        if (add) {
            ThrowUtils.throwIf(StringUtils.isBlank(userName), ErrorCode.PARAMS_ERROR, "用户名称不能为空");
        }
        if (StringUtils.isNotBlank(userPhone)) {
            ThrowUtils.throwIf(!RegexUtils.checkPhone(userPhone), ErrorCode.PARAMS_ERROR, "用户手机号格式有误");
        }
        if (StringUtils.isNotBlank(userEmail)) {
            ThrowUtils.throwIf(!RegexUtils.checkEmail(userEmail), ErrorCode.PARAMS_ERROR, "用户邮箱格式有误");
            ThrowUtils.throwIf(userEmail.length() > 256, ErrorCode.PARAMS_ERROR, "用户邮箱过长");
        }
        if (StringUtils.isNotBlank(userProfile)) {
            ThrowUtils.throwIf(userProfile.length() > 500, ErrorCode.PARAMS_ERROR, "用户简介过长");
        }
    }

    /**
     * 获取当前登录用户
     *
     * @param request request
     * @return {@link User}
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        Long userId = SecurityUtils.getLoginUserId();
        User currentUser = this.getById(userId);
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 获取当前登录用户（允许未登录）
     *
     * @param request request
     * @return {@link User}
     */
    @Override
    public User getLoginUserPermitNull(HttpServletRequest request) {
        Long userId = SecurityUtils.getLoginUserIdPermitNull();
        if (userId == null) {
            return null;
        }
        return this.getById(userId);
    }

    /**
     * 是否为管理员
     *
     * @param request request
     * @return boolean 是否为管理员
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        return SecurityUtils.isAdmin();
    }

    @Override
    public boolean isAdmin(User user) {
        return UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    /**
     * 用户注销
     *
     * @param request request
     * @return boolean 是否退出成功
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        StpUtil.checkLogin();
        StpUtil.logout();
        return true;
    }

    /**
     * 获取登录用户视图类
     *
     * @param user user
     * @return {@link LoginUserVO}
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        loginUserVO.setToken(StpUtil.getTokenInfo().getTokenValue());
        return loginUserVO;
    }

    /**
     * 获取用户 VO 封装类
     *
     * @param user    user
     * @param request request
     * @return {@link UserVO}
     */
    @Override
    public UserVO getUserVO(User user, HttpServletRequest request) {
        return UserConvert.objToVo(user);
    }

    /**
     * 获取用户 VO 视图类列表
     *
     * @param userList 用户列表
     * @param request  HTTP 请求
     * @return 用户视图类列表
     */
    @Override
    public List<UserVO> getUserVO(List<User> userList, HttpServletRequest request) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(user -> getUserVO(user, request)).collect(Collectors.toList());
    }

    /**
     * 分页获取用户视图类
     *
     * @param userPage 用户分页数据
     * @param request  HTTP 请求
     * @return 用户视图类分页对象
     */
    @Override
    public Page<UserVO> getUserVOPage(Page<User> userPage, HttpServletRequest request) {
        List<User> userList = userPage.getRecords();
        Page<UserVO> userVOPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        if (CollUtil.isEmpty(userList)) {
            return userVOPage;
        }
        List<UserVO> userVOList = userList.stream().map(UserConvert::objToVo).collect(Collectors.toList());
        userVOPage.setRecords(userVOList);

        return userVOPage;
    }

    /**
     * 获取查询封装类
     *
     * @param userQueryRequest userQueryRequest
     * @return {@link LambdaQueryWrapper<User>}
     */
    @Override
    public LambdaQueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        Long notId = userQueryRequest.getNotId();
        String wxUnionId = userQueryRequest.getWxUnionId();
        String userName = userQueryRequest.getUserName();
        String userRole = userQueryRequest.getUserRole();
        String userPhone = userQueryRequest.getUserPhone();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        String searchText = userQueryRequest.getSearchText();

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .eq(id != null, User::getId, id)
                .ne(ObjectUtils.isNotEmpty(notId), User::getId, notId)
                .eq(StringUtils.isNotBlank(wxUnionId), User::getWxUnionId, wxUnionId)
                .eq(StringUtils.isNotBlank(userRole), User::getUserRole, userRole)
                .like(StringUtils.isNotBlank(userName), User::getUserName, userName)
                .like(StringUtils.isNotBlank(userPhone), User::getUserPhone, userPhone);

        if (StringUtils.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw
                    .like(User::getUserName, searchText)
                    .or()
                    .like(User::getUserProfile, searchText));
        }

        if (SqlUtils.validSortField(sortField)) {
            boolean isAsc = CommonConstant.SORT_ORDER_ASC.equalsIgnoreCase(sortOrder);
            switch (sortField) {
                case "createTime" -> queryWrapper.orderBy(true, isAsc, User::getCreateTime);
                case "updateTime" -> queryWrapper.orderBy(true, isAsc, User::getUpdateTime);
                case "userName" -> queryWrapper.orderBy(true, isAsc, User::getUserName);
                default -> {
                }
            }
        }

        return queryWrapper;
    }



    /**
     * 异步记录登录日志
     *
     * @param logRecordRequest 登录日志记录请求
     */
    @Async
    public void recordLoginLogAsync(UserLoginLogRecordRequest logRecordRequest) {
        try {
            UserLoginLogAddRequest request = new UserLoginLogAddRequest();
            request.setUserId(logRecordRequest.getUser().getId());
            request.setAccount(logRecordRequest.getAccount());
            request.setLoginType(logRecordRequest.getLoginType().getValue());
            request.setStatus(LoginStatusEnum.SUCCESS.getValue());

            // 提取客户端信息
            HttpServletRequest httpRequest = logRecordRequest.getHttpRequest();
            if (httpRequest != null) {
                String clientIp = IpUtils.getClientIp(httpRequest);
                request.setClientIp(clientIp);
                request.setLocation(IpUtils.getRegion(clientIp));
                request.setUserAgent(httpRequest.getHeader("User-Agent"));
            }

            logFeignClient.addUserLoginLog(request);
        } catch (Exception e) {
            log.error("记录登录日志失败", e);
        }
    }




    @Override
    public void sendEmailCode(String email) {
        ThrowUtils.throwIf(StringUtils.isBlank(email), ErrorCode.PARAMS_ERROR, "邮箱地址不能为空");
        ThrowUtils.throwIf(!RegexUtils.checkEmail(email), ErrorCode.PARAMS_ERROR, "邮箱格式不正确");

        // 生成 6 位验证码
        String code = RandomUtil.randomNumbers(6);
        String redisKey = UserConstant.USER_LOGIN_EMAIL_CODE + email;
        // 缓存 5 分钟
        cacheUtils.put(redisKey, code);

        // 发送邮件
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(email);
            message.setSubject("【MallChat】登录验证码");
            message.setText("您的登录验证码为：" + code + "，有效期 5 分钟。请勿泄露给他人。");
            mailSender.send(message);
            log.info("向邮箱 {} 发送验证码成功", email);
        } catch (Exception e) {
            log.error("发送邮件验证码失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "发送邮件验证码失败");
        }
    }

    @Override
    public LoginUserVO userLoginByEmail(String email, String code) {
        ThrowUtils.throwIf(StringUtils.isAnyBlank(email, code), ErrorCode.PARAMS_ERROR, "邮箱或验证码不能为空");

        String redisKey = UserConstant.USER_LOGIN_EMAIL_CODE + email;
        String cachedCode = cacheUtils.get(redisKey);
        ThrowUtils.throwIf(!code.equals(cachedCode), ErrorCode.PARAMS_ERROR, "验证码错误或已过期");

        // 验证成功后移除验证码
        cacheUtils.remove(redisKey);

        String lockKey = "user:register:email:" + email;
        return lockUtils.lockEvent(lockKey, new TimeModel(5L, TimeUnit.SECONDS), () -> {
            User user = matchUserByEmail(email);
            if (user == null) {
                user = createEmailUser(email);
            }

            // 更新登录状态
            user.setLastLoginTime(new Date());
            this.updateById(user);

            // Sa-Token 登录
            StpUtil.login(user.getId());

            // 记录日志与 Session
            recordLoginProcess(user, email, LoginTypeEnum.EMAIL);

            return this.getLoginUserVO(user);
        }, () -> {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "登录人数过多，请稍后再试");
        });
    }

    private User matchUserByEmail(String email) {
        return this.getOne(new LambdaQueryWrapper<User>().eq(User::getUserEmail, email));
    }

    private User createEmailUser(String email) {
        User user = new User();
        user.setUserEmail(email);
        // 默认用户名取邮箱前缀
        String defaultUserName = email.split("@")[0];
        user.setUserName(defaultUserName);
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean result = this.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "用户注册失败");
        return user;
    }



    @Override
    public LoginUserVO userLoginByMa(String code) {
        ThrowUtils.throwIf(StringUtils.isBlank(code), ErrorCode.PARAMS_ERROR, "code 不能为空");
        try {
            WxMaJscode2SessionResult sessionInfo = wxMaService.getUserService().getSessionInfo(code);
            return registerOrMatchWxUser(sessionInfo.getUnionid(), sessionInfo.getOpenid(), LoginTypeEnum.WECHAT_MA);
        } catch (Exception e) {
            log.error("userLoginByMa error", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "小程序登录失败: " + e.getMessage());
        }
    }

    @Override
    public LoginUserVO userLoginByApp(String code) {
        ThrowUtils.throwIf(StringUtils.isBlank(code), ErrorCode.PARAMS_ERROR, "code 不能为空");
        if (StringUtils.isAnyBlank(wxAppProperties.getAppId(), wxAppProperties.getAppSecret())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未配置微信 App 登录信息");
        }
        try {
            // 通过 HttpUtil 调用微信 OAuth2 接口
            String url = String.format("https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code",
                    wxAppProperties.getAppId(), wxAppProperties.getAppSecret(), code);
            String response = HttpUtil.get(url);
            JSONObject jsonObject = JSONUtil.parseObj(response);
            String openId = jsonObject.getStr("openid");
            String unionId = jsonObject.getStr("unionid");
            if (StringUtils.isBlank(openId)) {
                log.error("微信 App 登录失败, response: {}", response);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "微信授权失败");
            }
            return registerOrMatchWxUser(unionId, openId, LoginTypeEnum.WECHAT_APP);
        } catch (Exception e) {
            log.error("userLoginByApp error", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "App 登录失败: " + e.getMessage());
        }
    }

    /**
     * 统一注册或匹配微信用户逻辑 (UnionID 优先)
     */
    @Override
    public LoginUserVO userLoginByApple(UserAppleLoginRequest request) {
        String identityToken = request.getIdentityToken();
        String userIdentifier = request.getUserIdentifier();

        // 1. 简单校验 Identity Token
        JWT jwt = JWTUtil.parseToken(identityToken);
        // 校验签发者
        String iss = (String) jwt.getPayload("iss");
        ThrowUtils.throwIf(!"https://appleid.apple.com".equals(iss), ErrorCode.PARAMS_ERROR, "非法的 Apple Token 签发者");
        // 校验受众 (Bundle ID)
        String aud = (String) jwt.getPayload("aud");
        ThrowUtils.throwIf(!appleProperties.getClientId().equals(aud), ErrorCode.PARAMS_ERROR, "非法的 Apple Token 受众");
        // 校验用户标识
        String sub = (String) jwt.getPayload("sub");
        ThrowUtils.throwIf(!userIdentifier.equals(sub), ErrorCode.PARAMS_ERROR, "Apple 用户标识不匹配");

        // 2. 匹配或注册用户
        return registerOrMatchAppleUser(sub);
    }

    private LoginUserVO registerOrMatchAppleUser(String appleId) {
        String lockKey = "user:register:apple:" + appleId;
        return lockUtils.lockEvent(lockKey, new TimeModel(5L, TimeUnit.SECONDS), () -> {
            User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getAppleId, appleId));
            if (user == null) {
                user = new User();
                user.setAppleId(appleId);
                user.setUserName("苹果用户_" + RandomUtil.randomString(6));
                user.setUserRole(UserRoleEnum.USER.getValue());
                this.save(user);
            }

            user.setLastLoginTime(new Date());
            this.updateById(user);
            StpUtil.login(user.getId());

            recordLoginProcess(user, appleId, LoginTypeEnum.APPLE);

            return this.getLoginUserVO(user);
        }, () -> {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "系统繁忙，请稍后再试");
        });
    }

    private LoginUserVO registerOrMatchWxUser(String unionId, String openId, LoginTypeEnum loginType) {

        String lockKey = "user:register:wx:" + (StringUtils.isNotBlank(unionId) ? unionId : openId);
        return lockUtils.lockEvent(lockKey, new TimeModel(5L, TimeUnit.SECONDS), () -> {
            User user = matchUser(unionId, openId, loginType);
            boolean isNewUser = (user == null);

            if (isNewUser) {
                user = createWxUser(unionId, openId, loginType);
            } else {
                syncWxUserInfo(user, unionId, openId, loginType);
            }

            // 更新登录状态
            user.setLastLoginTime(new Date());
            this.updateById(user);
            
            // Sa-Token 登录
            StpUtil.login(user.getId());

            // 记录日志与 Session
            recordLoginProcess(user, openId, loginType);

            return this.getLoginUserVO(user);
        }, () -> {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "登录人数过多，请稍后再试");
        });
    }

    private User matchUser(String unionId, String openId, LoginTypeEnum loginType) {
        User user = null;
        if (StringUtils.isNotBlank(unionId)) {
            user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getWxUnionId, unionId));
        }
        if (user == null) {
            if (loginType == LoginTypeEnum.WECHAT_MA) {
                user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getMaOpenId, openId));
            } else if (loginType == LoginTypeEnum.WECHAT_APP) {
                user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getWxOpenId, openId));
            }
        }
        return user;
    }

    private User createWxUser(String unionId, String openId, LoginTypeEnum loginType) {
        User user = new User();
        user.setWxUnionId(unionId);
        if (loginType == LoginTypeEnum.WECHAT_MA) {
            user.setMaOpenId(openId);
        } else {
            user.setWxOpenId(openId);
        }
        user.setUserName("微信用户_" + RandomUtil.randomString(6));
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean result = this.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "用户注册失败");
        return user;
    }

    private void syncWxUserInfo(User user, String unionId, String openId, LoginTypeEnum loginType) {
        boolean updated = false;
        if (loginType == LoginTypeEnum.WECHAT_MA && StringUtils.isBlank(user.getMaOpenId())) {
            user.setMaOpenId(openId);
            updated = true;
        } else if (loginType == LoginTypeEnum.WECHAT_APP && StringUtils.isBlank(user.getWxOpenId())) {
            user.setWxOpenId(openId);
            updated = true;
        }
        if (StringUtils.isBlank(user.getWxUnionId()) && StringUtils.isNotBlank(unionId)) {
            user.setWxUnionId(unionId);
            updated = true;
        }
        if (updated) {
            this.updateById(user);
        }
    }

    private void recordLoginProcess(User user, String openId, LoginTypeEnum loginType) {
        UserLoginLogRecordRequest logRecordRequest = new UserLoginLogRecordRequest();
        logRecordRequest.setUser(user);
        logRecordRequest.setLoginType(loginType);
        logRecordRequest.setAccount(openId);
        recordLoginLogAsync(logRecordRequest);

        StpUtil.getSession().set(UserConstant.USER_LOGIN_STATE, UserConvert.objToVo(user));
    }
}
