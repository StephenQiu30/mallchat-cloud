package com.stephen.cloud.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.api.log.client.LogFeignClient;
import com.stephen.cloud.api.log.model.dto.login.UserLoginLogAddRequest;
import com.stephen.cloud.api.log.model.enums.LoginStatusEnum;
import com.stephen.cloud.api.log.model.enums.LoginTypeEnum;
import com.stephen.cloud.api.user.model.dto.UserEmailLoginRequest;
import com.stephen.cloud.api.user.model.dto.UserQueryRequest;
import com.stephen.cloud.api.user.model.enums.EmailVerifiedEnum;
import com.stephen.cloud.api.user.model.enums.UserRoleEnum;
import com.stephen.cloud.api.user.model.vo.GitHubUserVO;
import com.stephen.cloud.api.user.model.vo.LoginUserVO;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.api.user.model.vo.WxLoginResponse;
import com.stephen.cloud.common.cache.model.TimeModel;
import com.stephen.cloud.common.cache.utils.CacheUtils;
import com.stephen.cloud.common.cache.utils.lock.LockUtils;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.constants.CommonConstant;
import com.stephen.cloud.common.constants.UserConstant;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.mysql.utils.SqlUtils;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.producer.RabbitMqSender;
import com.stephen.cloud.common.auth.utils.SecurityUtils;
import com.stephen.cloud.common.utils.IpUtils;
import com.stephen.cloud.common.utils.RegexUtils;
import com.stephen.cloud.user.convert.UserConvert;
import com.stephen.cloud.user.mapper.UserMapper;
import com.stephen.cloud.user.model.dto.UserLoginLogRecordRequest;
import com.stephen.cloud.user.model.entity.User;
import com.stephen.cloud.user.service.GitHubOAuthService;
import com.stephen.cloud.user.service.GitHubService;
import com.stephen.cloud.user.service.UserEmailService;
import com.stephen.cloud.user.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.result.WxMpQrCodeTicket;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
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
    private GitHubService gitHubService;

    @Resource
    private UserEmailService userEmailService;

    @Resource
    private WxMpService wxMpService;

    @Resource
    private CacheUtils cacheUtils;

    @Resource
    private GitHubOAuthService gitHubOAuthService;

    @Resource
    private LogFeignClient logFeignClient;

    @Resource
    private RabbitMqSender mqSender;

    @Resource
    private LockUtils lockUtils;

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
        String userEmail = user.getUserEmail();
        String userPhone = user.getUserPhone();
        String userProfile = user.getUserProfile();

        if (add) {
            ThrowUtils.throwIf(StringUtils.isBlank(userName), ErrorCode.PARAMS_ERROR, "用户名称不能为空");
            ThrowUtils.throwIf(StringUtils.isBlank(userEmail), ErrorCode.PARAMS_ERROR, "用户邮箱不能为空");
        }
        if (StringUtils.isNotBlank(userName)) {
            ThrowUtils.throwIf(userName.length() < 2 || userName.length() > 30, ErrorCode.PARAMS_ERROR, "用户昵称过短或过长");
        }
        if (StringUtils.isNotBlank(userEmail)) {
            ThrowUtils.throwIf(!RegexUtils.checkEmail(userEmail), ErrorCode.PARAMS_ERROR, "用户邮箱格式有误");
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getUserEmail, userEmail);
            queryWrapper.ne(user.getId() != null, User::getId, user.getId());
            long count = this.count(queryWrapper);
            ThrowUtils.throwIf(count > 0, ErrorCode.PARAMS_ERROR, "该邮箱已被占用");
        }
        if (StringUtils.isNotBlank(userPhone)) {
            ThrowUtils.throwIf(!RegexUtils.checkPhone(userPhone), ErrorCode.PARAMS_ERROR, "用户手机号格式有误");
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
        String mpOpenId = userQueryRequest.getMpOpenId();
        String userName = userQueryRequest.getUserName();
        String userRole = userQueryRequest.getUserRole();
        String userEmail = userQueryRequest.getUserEmail();
        String userPhone = userQueryRequest.getUserPhone();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        String searchText = userQueryRequest.getSearchText();

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .eq(id != null, User::getId, id)
                .ne(ObjectUtils.isNotEmpty(notId), User::getId, notId)
                .eq(StringUtils.isNotBlank(wxUnionId), User::getWxUnionId, wxUnionId)
                .eq(StringUtils.isNotBlank(mpOpenId), User::getMpOpenId, mpOpenId)
                .eq(StringUtils.isNotBlank(userRole), User::getUserRole, userRole)
                .like(StringUtils.isNotBlank(userName), User::getUserName, userName)
                .like(StringUtils.isNotBlank(userEmail), User::getUserEmail, userEmail)
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
     * GitHub 登录
     *
     * @param code    授权码
     * @param request HTTP请求
     * @return {@link LoginUserVO}
     */
    @Override
    public LoginUserVO userLoginByGitHub(String code, String state, HttpServletRequest request) {
        gitHubOAuthService.validateAndConsumeState(state);
        ThrowUtils.throwIf(StringUtils.isBlank(code), ErrorCode.PARAMS_ERROR, "授权码不能为空");

        String accessToken = gitHubService.getAccessToken(code);
        ThrowUtils.throwIf(StringUtils.isBlank(accessToken), ErrorCode.OPERATION_ERROR, "获取 GitHub Access Token 失败");

        GitHubUserVO gitHubUserVO = gitHubService.getUserInfo(accessToken);
        if (gitHubUserVO == null || StringUtils.isBlank(gitHubUserVO.getId())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取 GitHub 用户信息失败");
        }

        String githubId = gitHubUserVO.getId();
        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getGithubId, githubId));

        if (user == null && StringUtils.isNotBlank(gitHubUserVO.getEmail())) {
            user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getUserEmail, gitHubUserVO.getEmail()));
            if (user != null) {
                user.setGithubId(githubId);
                user.setGithubLogin(gitHubUserVO.getLogin());
                user.setGithubUrl(gitHubUserVO.getHtmlUrl());
                if (StringUtils.isNotBlank(gitHubUserVO.getAvatarUrl()) && StringUtils.isBlank(user.getUserAvatar())) {
                    user.setUserAvatar(gitHubUserVO.getAvatarUrl());
                }
                boolean result = this.updateById(user);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "关联 GitHub 账号失败");
            }
        }

        String lockKey = "user:register:github:" + githubId;
        return lockUtils.lockEvent(lockKey, new TimeModel(5L, TimeUnit.SECONDS), () -> {
            User lockedUser = this.getOne(new LambdaQueryWrapper<User>().eq(User::getGithubId, githubId));
            if (lockedUser == null) {
                lockedUser = new User();
                lockedUser.setGithubId(githubId);
                lockedUser
                        .setUserName(gitHubUserVO.getName() != null ? gitHubUserVO.getName() : gitHubUserVO.getLogin());
                lockedUser.setUserAvatar(gitHubUserVO.getAvatarUrl());
                lockedUser.setGithubLogin(gitHubUserVO.getLogin());
                lockedUser.setGithubUrl(gitHubUserVO.getHtmlUrl());
                if (StringUtils.isNotBlank(gitHubUserVO.getEmail())) {
                    lockedUser.setUserEmail(gitHubUserVO.getEmail());
                    lockedUser.setEmailVerified(EmailVerifiedEnum.VERIFIED.getValue());
                }
                lockedUser.setUserRole(UserRoleEnum.USER.getValue());
                boolean result = this.save(lockedUser);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "GitHub 注册失败");
            }
            User finalUser = lockedUser;

            finalUser.setLastLoginTime(new Date());
            finalUser.setLastLoginIp(IpUtils.getClientIp(request));
            this.updateById(finalUser);

            StpUtil.login(finalUser.getId());

            UserLoginLogRecordRequest logRecordRequest = new UserLoginLogRecordRequest();
            logRecordRequest.setUser(finalUser);
            logRecordRequest.setLoginType(LoginTypeEnum.GITHUB);
            logRecordRequest.setAccount(gitHubUserVO.getLogin());
            logRecordRequest.setHttpRequest(request);
            recordLoginLogAsync(logRecordRequest);

            LoginUserVO loginUserVO = getLoginUserVO(finalUser);
            UserVO userVO = UserConvert.objToVo(finalUser);
            StpUtil.getSession().set(UserConstant.USER_LOGIN_STATE, userVO);
            return loginUserVO;
        }, () -> {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "登录人数过多，请稍后再试");
        });
    }

    /**
     * 邮箱登录
     *
     * @param userEmailLoginRequest 邮箱登录请求
     * @param request               HTTP请求
     * @return {@link LoginUserVO}
     */
    @Override
    public LoginUserVO userLoginByEmail(UserEmailLoginRequest userEmailLoginRequest, HttpServletRequest request) {
        if (userEmailLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String email = StringUtils.trimToEmpty(userEmailLoginRequest.getEmail());
        String code = userEmailLoginRequest.getCode();
        if (StringUtils.isAnyBlank(email, code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ThrowUtils.throwIf(!RegexUtils.checkEmail(email), ErrorCode.PARAMS_ERROR, "用户邮箱格式有误");

        boolean verifyResult = userEmailService.verifyEmailCode(email, code);
        ThrowUtils.throwIf(!verifyResult, ErrorCode.PARAMS_ERROR, "验证码错误或已过期");

        String lockKey = "user:register:email:" + email;
        return lockUtils.lockEvent(lockKey, new TimeModel(5L, TimeUnit.SECONDS), () -> {
            User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getUserEmail, email));

            if (user == null) {
                user = new User();
                user.setUserEmail(email);
                user.setEmailVerified(EmailVerifiedEnum.VERIFIED.getValue());
                String userName = email.split("@")[0];
                user.setUserName(userName);
                user.setUserRole(UserRoleEnum.USER.getValue());
                boolean result = this.save(user);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "用户注册失败");
            } else {
                if (user.getEmailVerified() == null
                        || user.getEmailVerified().equals(EmailVerifiedEnum.UNVERIFIED.getValue())) {
                    user.setEmailVerified(EmailVerifiedEnum.VERIFIED.getValue());
                    this.updateById(user);
                }
            }

            user.setLastLoginTime(new Date());
            user.setLastLoginIp(IpUtils.getClientIp(request));
            this.updateById(user);

            StpUtil.login(user.getId());

            userEmailService.deleteEmailCode(email);

            UserLoginLogRecordRequest logRecordRequest = new UserLoginLogRecordRequest();
            logRecordRequest.setUser(user);
            logRecordRequest.setLoginType(LoginTypeEnum.EMAIL);
            logRecordRequest.setAccount(email);
            logRecordRequest.setHttpRequest(request);
            recordLoginLogAsync(logRecordRequest);

            LoginUserVO loginUserVO = getLoginUserVO(user);
            UserVO userVO = UserConvert.objToVo(user);
            StpUtil.getSession().set(UserConstant.USER_LOGIN_STATE, userVO);
            return loginUserVO;
        }, () -> {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "登录人数过多，请稍后再试");
        });
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
    public String getGitHubAuthorizeUrl() {
        return gitHubOAuthService.buildAuthorizeUrl();
    }

    @Override
    public WxLoginResponse getLoginQrCode() {
        try {
            String sceneId = UUID.randomUUID().toString();
            // 生成临时二维码，有效期 5 分钟
            WxMpQrCodeTicket ticket = wxMpService.getQrcodeService().qrCodeCreateTmpTicket(sceneId, 60 * 5);
            String qrCodeUrl = wxMpService.getQrcodeService().qrCodePictureUrl(ticket.getTicket());
            return WxLoginResponse.builder()
                    .qrCodeUrl(qrCodeUrl)
                    .sceneId(sceneId)
                    .build();
        } catch (Exception e) {
            log.error("getLoginQrCode error", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取二维码失败");
        }
    }

    @Override
    public LoginUserVO checkWxLoginStatus(String sceneId) {
        String redisKey = "user:login:wx:" + sceneId;
        LoginUserVO loginUserVO = cacheUtils.get(redisKey);
        if (loginUserVO != null) {
            cacheUtils.remove(redisKey);
        }
        return loginUserVO;
    }

    @Override
    public LoginUserVO userLoginByWxOpenId(String openId) {
        ThrowUtils.throwIf(StringUtils.isBlank(openId), ErrorCode.PARAMS_ERROR, "微信 OpenID 不能为空");

        String lockKey = "user:register:wechat:" + openId;
        return lockUtils.lockEvent(lockKey, new TimeModel(5L, TimeUnit.SECONDS), () -> {
            User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getMpOpenId, openId));

            if (user == null) {
                user = new User();
                user.setMpOpenId(openId);
                user.setUserName("微信用户_" + openId.substring(Math.max(0, openId.length() - 4)));
                user.setUserRole(UserRoleEnum.USER.getValue());
                boolean result = this.save(user);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "用户注册失败");
            }

            user.setLastLoginTime(new Date());
            this.updateById(user);

            StpUtil.login(user.getId());

            UserLoginLogRecordRequest logRecordRequest = new UserLoginLogRecordRequest();
            logRecordRequest.setUser(user);
            logRecordRequest.setLoginType(LoginTypeEnum.WECHAT_MP);
            logRecordRequest.setAccount(openId);
            logRecordRequest.setHttpRequest(null);
            recordLoginLogAsync(logRecordRequest);

            LoginUserVO loginUserVO = this.getLoginUserVO(user);
            UserVO userVO = UserConvert.objToVo(user);
            StpUtil.getSession().set(UserConstant.USER_LOGIN_STATE, userVO);
            return loginUserVO;
        }, () -> {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "登录人数过多，请稍后再试");
        });
    }

}
