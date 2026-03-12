package com.stephen.cloud.user.service.impl;

import com.stephen.cloud.api.log.client.LogFeignClient;
import com.stephen.cloud.api.log.model.dto.operation.OperationLogAddRequest;
import com.stephen.cloud.common.log.model.OperationLogContext;
import com.stephen.cloud.common.log.service.OperationLogRecorder;
import com.stephen.cloud.common.utils.IpUtils;
import com.stephen.cloud.user.model.entity.User;
import com.stephen.cloud.user.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 用户服务操作日志记录器实现
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class UserOperationLogRecorderImpl implements OperationLogRecorder {

    @Resource
    private LogFeignClient logFeignClient;

    @Lazy
    @Resource
    private UserService userService;

    /**
     * 异步记录操作日志
     *
     * @param context 操作日志上下文
     */
    @Async
    @Override
    public void recordOperationLogAsync(OperationLogContext context) {
        try {
            // 构建操作日志创建请求
            OperationLogAddRequest request = new OperationLogAddRequest();
            request.setModule(context.getModule());
            request.setAction(context.getAction());
            request.setMethod(context.getMethod());
            request.setPath(context.getPath());
            request.setRequestParams(context.getRequestParams());
            // 转换 Boolean 为 Integer：true -> 1, false -> 0
            request.setSuccess(Boolean.TRUE.equals(context.getSuccess()) ? 1 : 0);
            request.setErrorMessage(context.getErrorMessage());

            // 获取操作人信息
            HttpServletRequest httpRequest = context.getHttpRequest();
            if (httpRequest != null) {
                // 优先从上下文中获取操作人信息（由 AOP 设置）
                if (context.getOperatorId() != null) {
                    request.setOperatorId(context.getOperatorId());
                }
                if (cn.hutool.core.util.StrUtil.isNotBlank(context.getOperatorName())) {
                    request.setOperatorName(context.getOperatorName());
                }

                // 如果上下文没有，或者只有 ID 没有名字，尝试兜底获取
                if (request.getOperatorId() == null || cn.hutool.core.util.StrUtil.isBlank(request.getOperatorName())) {
                    try {
                        // 尝试获取当前登录用户
                        User loginUser = userService.getLoginUserPermitNull(httpRequest);
                        if (loginUser != null) {
                            if (request.getOperatorId() == null) {
                                request.setOperatorId(loginUser.getId());
                            }
                            if (cn.hutool.core.util.StrUtil.isBlank(request.getOperatorName())) {
                                request.setOperatorName(loginUser.getUserName());
                            }
                        }
                    } catch (Exception e) {
                        log.warn("获取操作人信息失败: {}", e.getMessage());
                    }
                }

                // 获取IP地址
                String clientIp = IpUtils.getClientIp(httpRequest);
                request.setClientIp(clientIp);
                request.setLocation(IpUtils.getRegion(clientIp));
            }

            // 调用日志服务记录操作日志
            logFeignClient.addOperationLog(request);
            log.debug("操作日志记录成功: module={}, action={}, success={}",
                    context.getModule(), context.getAction(), context.getSuccess());
        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }
    }
}
