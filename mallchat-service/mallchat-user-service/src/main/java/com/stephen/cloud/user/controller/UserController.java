package com.stephen.cloud.user.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.user.model.dto.*;
import com.stephen.cloud.api.user.model.vo.LoginUserVO;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.common.common.*;
import com.stephen.cloud.common.constants.UserConstant;
import com.stephen.cloud.common.log.annotation.OperationLog;
import com.stephen.cloud.user.convert.UserConvert;
import com.stephen.cloud.user.model.entity.User;
import com.stephen.cloud.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户接口
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/user")
@Slf4j
@Tag(name = "UserController", description = "用户管理")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注销
     * <p>
     * 退出当前登录状态。
     *
     * @param request HTTP 请求
     * @return 是否成功退出
     */
    @PostMapping("/logout")
    @Operation(summary = "用户注销", description = "退出当前登录状态")
    @OperationLog(module = "用户认证", action = "用户注销")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 获取当前登录用户
     * <p>
     * 获取系统当前登录的用户信息。
     *
     * @param request HTTP 请求
     * @return 当前登录的用户信息
     */
    @GetMapping("/get/login")
    @Operation(summary = "获取当前登录用户", description = "获取系统当前登录的用户信息")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(user));
    }


    /**
     * 微信小程序登录
     *
     * @param loginRequest 登录请求
     * @return 登录用户信息
     */
    @PostMapping("/login/ma")
    @Operation(summary = "微信小程序登录", description = "通过微信小程序 code 进行登录或注册")
    @OperationLog(module = "用户认证", action = "微信小程序登录")
    public BaseResponse<LoginUserVO> userLoginByMa(@RequestBody UserMaLoginRequest loginRequest) {
        if (loginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LoginUserVO loginUserVO = userService.userLoginByMa(loginRequest.getCode());
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 微信 App 登录
     *
     * @param loginRequest 登录请求
     * @return 登录用户信息
     */
    @PostMapping("/login/app")
    @Operation(summary = "微信 App 登录", description = "通过微信 App code 进行登录或注册")
    @OperationLog(module = "用户认证", action = "微信 App 登录")
    public BaseResponse<LoginUserVO> userLoginByApp(@RequestBody UserAppLoginRequest loginRequest) {
        if (loginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LoginUserVO loginUserVO = userService.userLoginByApp(loginRequest.getCode());
        return ResultUtils.success(loginUserVO);
    }

    /**
     * Apple 登录
     *
     * @param loginRequest 登录请求
     * @return 登录用户信息
     */
    @PostMapping("/login/apple")
    @Operation(summary = "Apple 登录", description = "通过 Apple 授权信息进行登录或注册")
    @OperationLog(module = "用户认证", action = "Apple 登录")
    public BaseResponse<LoginUserVO> userLoginByApple(@RequestBody UserAppleLoginRequest loginRequest) {
        if (loginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LoginUserVO loginUserVO = userService.userLoginByApple(loginRequest);
        return ResultUtils.success(loginUserVO);
    }

    // endregion

    /**
     * 创建用户
     *
     * @param userAddRequest userAddRequest
     * @param request        request
     * @return BaseResponse<Long>
     */
    @PostMapping("/add")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @OperationLog(module = "用户管理", action = "创建用户")
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest,
            HttpServletRequest request) {
        if (userAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = UserConvert.addRequestToObj(userAddRequest);
        // 数据校验
        userService.validUser(user, true);
        // 写入数据库
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回新写入的数据 id
        long newUserId = user.getId();

        return ResultUtils.success(newUserId);
    }

    /**
     * 删除用户
     *
     * @param deleteRequest deleteRequest
     * @param request       request
     * @return BaseResponse<Boolean>
     */
    @PostMapping("/delete")
    @OperationLog(module = "用户管理", action = "删除用户")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest,
            HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        User oldUser = userService.getById(id);
        if (oldUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可删除
        if (!oldUser.getId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = userService.removeById(id);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }

        return ResultUtils.success(true);
    }

    /**
     * 更新用户
     *
     * @param userUpdateRequest userUpdateRequest
     * @param request           request
     * @return BaseResponse<Boolean>
     */
    @PostMapping("/update")
    @OperationLog(module = "用户管理", action = "更新用户")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest,
            HttpServletRequest request) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null || userUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = UserConvert.updateRequestToObj(userUpdateRequest);
        userService.validUser(user, false);
        long id = userUpdateRequest.getId();
        User oldUser = userService.getById(id);
        if (oldUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        boolean result = userService.updateById(user);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }

        return ResultUtils.success(true);
    }

    /**
     * 是否管理员（Feign 调用）
     *
     * @param request request
     * @return 是否管理员
     */
    @GetMapping("/is/admin")
    @Operation(summary = "是否管理员", description = "返回当前登录用户是否为管理员")
    public BaseResponse<Boolean> isAdmin(HttpServletRequest request) {
        try {
            return ResultUtils.success(userService.isAdmin(request));
        } catch (Exception e) {
            log.warn("获取管理员标记失败，按非管理员处理", e);
            return ResultUtils.success(false);
        }
    }

    /**
     * 根据 id 获取用户（仅管理员）
     *
     * @param id      用户id
     * @param request request
     * @return BaseResponse<User>
     */
    @GetMapping("/get")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(@RequestParam("id") long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取包装类
     *
     * @param id      用户id
     * @param request request
     * @return 查询得到的用户包装类
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(@RequestParam("id") long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(userService.getUserVO(user, request));
    }

    /**
     * 批量根据 id 获取用户包装类（Feign 调用）
     *
     * @param ids     用户id列表
     * @return 查询得到的用户包装类列表
     */
    @GetMapping("/get/vo/batch")
    public BaseResponse<List<UserVO>> getUserVOByIds(@RequestParam("ids") List<Long> ids) {
        ThrowUtils.throwIf(ids == null || ids.isEmpty(), ErrorCode.PARAMS_ERROR);
        List<User> userList = userService.listByIds(ids);
        // 批量接口主要用于内部 Feign 调用（如 ES 同步），不依赖 HttpServletRequest 上下文
        List<UserVO> userVOList = userList.stream()
                .map(UserConvert::objToVo)
                .collect(java.util.stream.Collectors.toList());
        return ResultUtils.success(userVOList);
    }

    /**
     * 分页获取用户列表（仅管理员）
     *
     * @param userQueryRequest userQueryRequest
     * @param request          request
     * @return BaseResponse<Page < User>>
     */
    @PostMapping("/list/page")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<User>> listUserByPage(@RequestBody UserQueryRequest userQueryRequest,
            HttpServletRequest request) {
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(current, size),
                userService.getQueryWrapper(userQueryRequest));
        return ResultUtils.success(userPage);
    }

    /**
     * 分页获取用户封装列表
     *
     * @param userQueryRequest 用户查询请求
     * @param request          request
     * @return BaseResponse<Page < UserVO>>
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest,
            HttpServletRequest request) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        if (size > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Page<User> userPage = userService.page(new Page<>(current, size),
                userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, size, userPage.getTotal());
        List<UserVO> userVOList = userService.getUserVO(userPage.getRecords(), request);
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }

    /**
     * 编辑个人信息
     *
     * @param userEditRequest userEditRequest
     * @param request         request
     * @return BaseResponse<Boolean>
     */
    @PostMapping("/edit")
    @OperationLog(module = "用户管理", action = "编辑个人信息")
    public BaseResponse<Boolean> editUser(@RequestBody UserEditRequest userEditRequest,
            HttpServletRequest request) {
        if (userEditRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        User user = UserConvert.editRequestToObj(userEditRequest);
        user.setId(loginUser.getId());
        userService.validUser(user, false);
        boolean result = userService.updateById(user);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }

        return ResultUtils.success(true);
    }

}
