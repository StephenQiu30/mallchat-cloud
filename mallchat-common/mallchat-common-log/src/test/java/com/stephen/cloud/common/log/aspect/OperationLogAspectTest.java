package com.stephen.cloud.common.log.aspect;

import com.stephen.cloud.common.log.annotation.OperationLog;
import com.stephen.cloud.common.log.model.OperationLogContext;
import com.stephen.cloud.common.log.service.OperationLogRecorder;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Map;

class OperationLogAspectTest {

    private RecordingOperationLogRecorder recorder;
    private OperationLogAspect aspect;

    @BeforeEach
    void setUp() {
        recorder = new RecordingOperationLogRecorder();
        aspect = new OperationLogAspect();
        ReflectionTestUtils.setField(aspect, "operationLogService", recorder);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldMaskSensitiveRequestParamsWhenAuditOperation() throws Throwable {
        Method method = AuditTarget.class.getMethod("sensitiveOperation", SensitiveRequest.class, HttpServletRequest.class);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/chat/friend/apply/add");
        request.addHeader("userId", "1001");
        request.addHeader("userName", "Stephen");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SensitiveRequest sensitiveRequest = new SensitiveRequest("abc123", "token-xyz", Map.of("password", "raw-password"));

        Object result = aspect.around(joinPoint(method, new Object[]{sensitiveRequest, request}, true));

        Assertions.assertEquals("ok", result);
        Assertions.assertNotNull(recorder.lastContext);
        Assertions.assertEquals("好友申请管理", recorder.lastContext.getModule());
        Assertions.assertEquals("申请好友", recorder.lastContext.getAction());
        Assertions.assertEquals(1001L, recorder.lastContext.getOperatorId());
        Assertions.assertEquals("Stephen", recorder.lastContext.getOperatorName());
        Assertions.assertTrue(recorder.lastContext.getSuccess());
        String requestParams = recorder.lastContext.getRequestParams();
        Assertions.assertFalse(requestParams.contains("abc123"));
        Assertions.assertFalse(requestParams.contains("token-xyz"));
        Assertions.assertFalse(requestParams.contains("raw-password"));
        Assertions.assertTrue(requestParams.contains("***"), requestParams);
    }

    @Test
    void shouldAuditFailedOperationWithoutSwallowingException() throws Throwable {
        Method method = AuditTarget.class.getMethod("sensitiveOperation", SensitiveRequest.class, HttpServletRequest.class);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/chat/room/dismiss");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class,
                () -> aspect.around(joinPoint(method, new Object[]{new SensitiveRequest("code", "token", Map.of()), request}, false)));

        Assertions.assertEquals("fail", exception.getMessage());
        Assertions.assertNotNull(recorder.lastContext);
        Assertions.assertFalse(recorder.lastContext.getSuccess());
        Assertions.assertEquals("fail", recorder.lastContext.getErrorMessage());
    }

    private ProceedingJoinPoint joinPoint(Method method, Object[] args, boolean success) throws Throwable {
        ProceedingJoinPoint joinPoint = Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature signature = Mockito.mock(MethodSignature.class);
        Mockito.when(signature.getMethod()).thenReturn(method);
        Mockito.when(joinPoint.getSignature()).thenReturn(signature);
        Mockito.when(joinPoint.getArgs()).thenReturn(args);
        if (success) {
            Mockito.when(joinPoint.proceed()).thenReturn("ok");
        } else {
            Mockito.when(joinPoint.proceed()).thenThrow(new IllegalStateException("fail"));
        }
        return joinPoint;
    }

    private static class RecordingOperationLogRecorder implements OperationLogRecorder {
        private OperationLogContext lastContext;

        @Override
        public void recordOperationLogAsync(OperationLogContext context) {
            this.lastContext = context;
        }
    }

    private static class AuditTarget {
        @OperationLog(module = "好友申请管理", action = "申请好友")
        public String sensitiveOperation(SensitiveRequest request, HttpServletRequest servletRequest) {
            return "ok";
        }
    }

    private record SensitiveRequest(String code, String accessToken, Map<String, Object> ext) {
    }
}
