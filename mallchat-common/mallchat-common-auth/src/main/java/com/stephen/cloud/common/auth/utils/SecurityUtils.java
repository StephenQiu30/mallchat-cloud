package com.stephen.cloud.common.auth.utils;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.constants.SecurityConstant;
import com.stephen.cloud.common.constants.UserConstant;
import com.stephen.cloud.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 安全工具类
 * 提供稳健的用户身份识别方法
 *
 * @author stephen
 */
public class SecurityUtils {

    /**
     * 获取当前登录用户 ID (稳健版)
     * 1. 优先从请求头中获取（网关透传或 Feign 传递）
     * 2. 如果请求头中没有，则尝试从 Sa-Token 上下文中获取
     *
     * @return 用户 ID
     * @throws BusinessException 如果未获取到用户 ID，则抛出未登录异常
     */
    public static Long getLoginUserId() {
        Long userId = getLoginUserIdPermitNull();
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return userId;
    }

    /**
     * 获取当前登录用户 ID (稳健版，允许为空)
     *
     * @return 用户 ID，获取不到返回 null
     */
    public static Long getLoginUserIdPermitNull() {
        // 1. 优先尝试从请求头获取 (网关透传最为可靠，能直接反映原始用户身份)
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        if (request != null) {
            String userIdStr = request.getHeader(SecurityConstant.USER_ID_HEADER);
            if (StrUtil.isNotBlank(userIdStr)) {
                return Convert.toLong(userIdStr);
            }
        }

        // 2. 备选方案：尝试从 Sa-Token 的当前会话环境中获取已入驻的 LoginId
        if (StpUtil.isLogin()) {
            try {
                return StpUtil.getLoginIdAsLong();
            } catch (Exception e) {
                // 如果是某些特殊内部调用导致的身份切换（如 ID 为 0），则按默认值处理
                return 0L;
            }
        }

        return null;
    }

    /**
     * 判断当前用户是否为管理员
     *
     * @return true: 是管理员; false: 不是管理员
     */
    public static boolean isAdmin() {
        return StpUtil.hasRole(UserConstant.ADMIN_ROLE);
    }
}
