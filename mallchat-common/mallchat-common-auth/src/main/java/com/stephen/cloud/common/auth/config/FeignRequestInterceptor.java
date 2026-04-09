package com.stephen.cloud.common.auth.config;

import com.stephen.cloud.common.constants.SecurityConstant;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign 请求拦截器
 * 用于在 Feign 调用时添加内部调用标识，并透传认证信息和分布式事务 XID
 *
 * @author StephenQiu30
 */
@Component
public class FeignRequestInterceptor implements RequestInterceptor {

    /**
     * Sa-Token 令牌请求头名称（与 Nacos 配置 token-name 保持一致）
     */
    private static final String AUTHORIZATION_HEADER = "Authorization";

    @Override
    public void apply(RequestTemplate template) {
        // 1. 添加内部调用来源标识 (From-Source: Inner)
        // 该标识配合 @InternalAuth 切面使用，用于识别并放行服务间的内部可信调用
        template.header(SecurityConstant.FROM_SOURCE, SecurityConstant.INNER);

        // 2. 透传当前请求的认证信息，确保下游服务能够识别用户身份
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            // 透传 JWT Token (Authorization)，保持登录态在服务间传递
            String token = request.getHeader(AUTHORIZATION_HEADER);
            if (token != null && !token.isEmpty()) {
                template.header(AUTHORIZATION_HEADER, token);
            }
            // 透传明文 UserID，方便下游服务直接通过 getHeader 获取用户 ID，减少二次解析
            String userId = request.getHeader(SecurityConstant.USER_ID_HEADER);
            if (userId != null && !userId.isEmpty()) {
                template.header(SecurityConstant.USER_ID_HEADER, userId);
            }
        }
    }
}
