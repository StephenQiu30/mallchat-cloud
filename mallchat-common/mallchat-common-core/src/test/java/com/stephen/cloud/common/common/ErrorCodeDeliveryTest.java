package com.stephen.cloud.common.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 消息投递相关错误码测试
 *
 * @author StephenQiu30
 */
class ErrorCodeDeliveryTest {

    @Test
    void shouldContainMessageNotFound() {
        ErrorCode code = ErrorCode.getEnumByCode(40401);
        assertNotNull(code, "MESSAGE_NOT_FOUND 错误码应存在");
        assertEquals("消息不存在", code.getMessage());
    }

    @Test
    void shouldContainMessageDuplicate() {
        ErrorCode code = ErrorCode.getEnumByCode(40901);
        assertNotNull(code, "MESSAGE_DUPLICATE 错误码应存在");
        assertEquals("重复消息", code.getMessage());
    }

    @Test
    void shouldContainMessageRevokeTimeout() {
        ErrorCode code = ErrorCode.getEnumByCode(40302);
        assertNotNull(code, "MESSAGE_REVOKE_TIMEOUT 错误码应存在");
        assertEquals("撤回超时", code.getMessage());
    }

    @Test
    void shouldContainMessageRevokeNoPermission() {
        ErrorCode code = ErrorCode.getEnumByCode(40303);
        assertNotNull(code, "MESSAGE_REVOKE_NO_PERMISSION 错误码应存在");
        assertEquals("无权限撤回", code.getMessage());
    }

    @Test
    void shouldContainMessageReplyNotFound() {
        ErrorCode code = ErrorCode.getEnumByCode(40402);
        assertNotNull(code, "MESSAGE_REPLY_NOT_FOUND 错误码应存在");
        assertEquals("被回复消息不存在", code.getMessage());
    }

    @Test
    void shouldContainMessageDeliveryFailed() {
        ErrorCode code = ErrorCode.getEnumByCode(50003);
        assertNotNull(code, "MESSAGE_DELIVERY_FAILED 错误码应存在");
        assertEquals("消息投递失败", code.getMessage());
    }

    @Test
    void shouldHaveUniqueErrorCodes() {
        ErrorCode[] codes = ErrorCode.values();
        long uniqueCount = java.util.Arrays.stream(codes)
                .map(ErrorCode::getCode)
                .distinct()
                .count();
        assertEquals(codes.length, uniqueCount, "所有错误码应唯一");
    }
}
