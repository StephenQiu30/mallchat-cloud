package com.stephen.cloud.common.auth.aspect;

import cn.hutool.core.util.StrUtil;
import com.stephen.cloud.common.auth.annotation.InternalAuth;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.constants.SecurityConstant;
import com.stephen.cloud.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 内部调用认证切面
 * 检查被 @InternalAuth 注解标识的方法，验证其请求头中是否包含正确的内部调用标识
 *
 * @author StephenQiu30
 */
@Aspect
@Component
@Slf4j
public class InternalAuthAspect {

    /**
     * 环绕通知，执行内部调用校验
     *
     * @param point        切入点
     * @param internalAuth 内部调用认证注解
     * @return 原方法的执行结果
     * @throws Throwable 原方法可能抛出的异常或由于无权访问抛出的 BusinessException
     */
    @Around("@annotation(internalAuth)")
    public Object around(ProceedingJoinPoint point, InternalAuth internalAuth) throws Throwable {
        // 1. 从 ThreadLocal 中获取当前的 Servlet 请求属性
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无法获取请求上下文");
        }

        HttpServletRequest request = attributes.getRequest();
        // 2. 从请求头中提取特定的内部调用来源标识
        String fromSource = request.getHeader(SecurityConstant.FROM_SOURCE);

        // 3. 核心校验逻辑：对比请求头中的 source 是否与安全常量中定义的内部标识一致
        if (!StrUtil.equals(fromSource, SecurityConstant.INNER)) {
            // 记录警告日志，便于追踪非法越权尝试
            log.warn("拦截到非内部调用访问 @InternalAuth 接口: {}", request.getRequestURI());
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅限内部调用");
        }

        // 4. 校验通过，允许方法继续执行
        return point.proceed();
    }
}
