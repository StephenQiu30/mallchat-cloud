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
        ErrorCode code = ErrorCode.MESSAGE_NOT_FOUND;
        assertEquals(40410, code.getCode());
        assertEquals("消息不存在", code.getMessage());
    }

    @Test
    void shouldContainMessageDuplicate() {
        ErrorCode code = ErrorCode.MESSAGE_DUPLICATE;
        assertEquals(40901, code.getCode());
        assertEquals("重复消息", code.getMessage());
    }

    @Test
    void shouldContainMessageRevokeTimeout() {
        ErrorCode code = ErrorCode.MESSAGE_REVOKE_TIMEOUT;
        assertEquals(40302, code.getCode());
        assertEquals("撤回超时", code.getMessage());
    }

    @Test
    void shouldContainMessageRevokeNoPermission() {
        ErrorCode code = ErrorCode.MESSAGE_REVOKE_NO_PERMISSION;
        assertEquals(40303, code.getCode());
        assertEquals("无权限撤回", code.getMessage());
    }

    @Test
    void shouldContainMessageReplyNotFound() {
        ErrorCode code = ErrorCode.MESSAGE_REPLY_NOT_FOUND;
        assertEquals(40411, code.getCode());
        assertEquals("被回复消息不存在", code.getMessage());
    }

    @Test
    void shouldContainMessageDeliveryFailed() {
        ErrorCode code = ErrorCode.MESSAGE_DELIVERY_FAILED;
        assertEquals(50003, code.getCode());
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
