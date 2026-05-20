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

import java.io.InputStream;
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
        SensitiveRequest sensitiveRequest = new SensitiveRequest(1L, "abc123", "token-xyz", Map.of("password", "raw-password"));

        Object result = aspect.around(joinPoint(method, new Object[]{sensitiveRequest, request}, true));

        Assertions.assertEquals("ok", result);
        Assertions.assertNotNull(recorder.lastContext);
        Assertions.assertEquals("好友申请管理", recorder.lastContext.getModule());
        Assertions.assertEquals("申请好友", recorder.lastContext.getAction());
        Assertions.assertEquals(1001L, recorder.lastContext.getOperatorId());
        Assertions.assertEquals("Stephen", recorder.lastContext.getOperatorName());
        Assertions.assertEquals("1", recorder.lastContext.getBizId());
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
                () -> aspect.around(joinPoint(method, new Object[]{new SensitiveRequest(1L, "code", "token", Map.of()), request}, false)));

        Assertions.assertEquals("fail", exception.getMessage());
        Assertions.assertNotNull(recorder.lastContext);
        Assertions.assertFalse(recorder.lastContext.getSuccess());
        Assertions.assertEquals("fail", recorder.lastContext.getErrorMessage());
    }

    @Test
    void shouldNotLeakRawParamWhenMaskingFallbackIsUsed() throws Throwable {
        Method method = AuditTarget.class.getMethod("unsafeOperation", UnsafeSensitiveRequest.class);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/chat/message/send");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        aspect.around(joinPoint(method, new Object[]{new UnsafeSensitiveRequest()}, true));

        Assertions.assertNotNull(recorder.lastContext);
        String requestParams = recorder.lastContext.getRequestParams();
        Assertions.assertFalse(requestParams.contains("raw-secret"), requestParams);
        Assertions.assertFalse(requestParams.contains("token=raw-secret"), requestParams);
        Assertions.assertTrue(requestParams.contains("Unserializable"), requestParams);
    }

    @Test
    void shouldPreferSpecificBizIdFieldOverGenericId() throws Throwable {
        Method method = AuditTarget.class.getMethod("sensitiveOperation", SensitiveRequest.class, HttpServletRequest.class);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/chat/moment/comment");
        request.addParameter("id", "999");
        request.addParameter("momentId", "123");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        aspect.around(joinPoint(method, new Object[]{new SensitiveRequest(1L, "code", "token", Map.of()), request}, true));

        Assertions.assertEquals("123", recorder.lastContext.getBizId());
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

        @OperationLog(module = "消息管理", action = "发送消息")
        public String unsafeOperation(UnsafeSensitiveRequest request) {
            return "ok";
        }
    }

    private record SensitiveRequest(Long roomId, String code, String accessToken, Map<String, Object> ext) {
    }

    private static class UnsafeSensitiveRequest extends InputStream {
        public String getToken() {
            throw new IllegalStateException("boom");
        }

        @Override
        public int read() {
            return -1;
        }

        @Override
        public String toString() {
            return "token=raw-secret";
        }
    }
}
