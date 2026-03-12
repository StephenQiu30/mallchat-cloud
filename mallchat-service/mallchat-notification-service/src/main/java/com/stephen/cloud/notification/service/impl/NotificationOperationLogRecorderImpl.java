package com.stephen.cloud.notification.service.impl;

import com.stephen.cloud.api.log.client.LogFeignClient;
import com.stephen.cloud.api.log.model.dto.operation.OperationLogAddRequest;
import com.stephen.cloud.common.log.model.OperationLogContext;
import com.stephen.cloud.common.log.service.OperationLogRecorder;
import com.stephen.cloud.common.utils.IpUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 通知服务操作日志记录器实现
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class NotificationOperationLogRecorderImpl implements OperationLogRecorder {

    @Lazy
    @Resource
    private LogFeignClient logFeignClient;

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
                // 从上下文中获取操作人ID和名称（由AOP设置）
                if (context.getOperatorId() != null) {
                    request.setOperatorId(context.getOperatorId());
                }
                if (context.getOperatorName() != null) {
                    request.setOperatorName(context.getOperatorName());
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
