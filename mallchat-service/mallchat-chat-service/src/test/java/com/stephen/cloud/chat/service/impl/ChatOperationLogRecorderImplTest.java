package com.stephen.cloud.chat.service.impl;

import com.stephen.cloud.api.log.client.LogFeignClient;
import com.stephen.cloud.api.log.model.dto.operation.OperationLogAddRequest;
import com.stephen.cloud.common.log.model.OperationLogContext;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

class ChatOperationLogRecorderImplTest {

    @Test
    void shouldForwardChatOperationLogContextToLogService() {
        LogFeignClient logFeignClient = Mockito.mock(LogFeignClient.class);
        ChatOperationLogRecorderImpl recorder = new ChatOperationLogRecorderImpl();
        ReflectionTestUtils.setField(recorder, "logFeignClient", logFeignClient);
        OperationLogContext context = context();

        recorder.recordOperationLogAsync(context);

        ArgumentCaptor<OperationLogAddRequest> captor = ArgumentCaptor.forClass(OperationLogAddRequest.class);
        Mockito.verify(logFeignClient).addOperationLog(captor.capture());
        OperationLogAddRequest request = captor.getValue();
        Assertions.assertEquals("聊天室管理", request.getModule());
        Assertions.assertEquals("解散群聊", request.getAction());
        Assertions.assertEquals("POST", request.getMethod());
        Assertions.assertEquals("/chat/room/dismiss", request.getPath());
        Assertions.assertEquals("{\"roomId\":1}", request.getRequestParams());
        Assertions.assertEquals(0, request.getSuccess());
        Assertions.assertEquals("无权操作", request.getErrorMessage());
        Assertions.assertEquals(1001L, request.getOperatorId());
        Assertions.assertEquals("Stephen", request.getOperatorName());
        Assertions.assertEquals("127.0.0.1", request.getClientIp());
        Assertions.assertEquals("MallChat-Test", request.getUserAgent());
    }

    @Test
    void shouldNotThrowWhenLogServiceFails() {
        LogFeignClient logFeignClient = Mockito.mock(LogFeignClient.class);
        Mockito.doThrow(new IllegalStateException("log down")).when(logFeignClient).addOperationLog(Mockito.any());
        ChatOperationLogRecorderImpl recorder = new ChatOperationLogRecorderImpl();
        ReflectionTestUtils.setField(recorder, "logFeignClient", logFeignClient);

        Assertions.assertDoesNotThrow(() -> recorder.recordOperationLogAsync(context()));
    }

    private OperationLogContext context() {
        HttpServletRequest request = new MockHttpServletRequest("POST", "/chat/room/dismiss");
        OperationLogContext context = new OperationLogContext();
        context.setModule("聊天室管理");
        context.setAction("解散群聊");
        context.setMethod("POST");
        context.setPath("/chat/room/dismiss");
        context.setRequestParams("{\"roomId\":1}");
        context.setSuccess(false);
        context.setErrorMessage("无权操作");
        context.setOperatorId(1001L);
        context.setOperatorName("Stephen");
        context.setClientIp("127.0.0.1");
        context.setUserAgent("MallChat-Test");
        context.setHttpRequest(request);
        return context;
    }
}
