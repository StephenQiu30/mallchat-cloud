package com.stephen.cloud.chat.service.impl;

import cn.hutool.core.util.StrUtil;
import com.stephen.cloud.api.log.client.LogFeignClient;
import com.stephen.cloud.api.log.model.dto.operation.OperationLogAddRequest;
import com.stephen.cloud.common.log.model.OperationLogContext;
import com.stephen.cloud.common.log.service.OperationLogRecorder;
import com.stephen.cloud.common.utils.IpUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 聊天服务操作日志记录器实现
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class ChatOperationLogRecorderImpl implements OperationLogRecorder {

    @Resource
    private LogFeignClient logFeignClient;

    @Async
    @Override
    public void recordOperationLogAsync(OperationLogContext context) {
        try {
            OperationLogAddRequest request = new OperationLogAddRequest();
            request.setModule(context.getModule());
            request.setAction(context.getAction());
            request.setBizId(context.getBizId());
            request.setMethod(context.getMethod());
            request.setPath(context.getPath());
            request.setRequestParams(context.getRequestParams());
            request.setSuccess(Boolean.TRUE.equals(context.getSuccess()) ? 1 : 0);
            request.setErrorMessage(context.getErrorMessage());
            request.setOperatorId(context.getOperatorId());
            request.setOperatorName(context.getOperatorName());
            request.setUserAgent(context.getUserAgent());

            if (StrUtil.isNotBlank(context.getClientIp())) {
                request.setClientIp(context.getClientIp());
                request.setLocation(IpUtils.getRegion(context.getClientIp()));
            }

            logFeignClient.addOperationLog(request);
            log.debug("操作日志记录成功: module={}, action={}, success={}",
                    context.getModule(), context.getAction(), context.getSuccess());
        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }
    }
}
