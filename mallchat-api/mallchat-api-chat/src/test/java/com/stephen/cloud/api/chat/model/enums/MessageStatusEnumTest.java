package com.stephen.cloud.api.chat.model.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 消息状态枚举测试
 *
 * @author StephenQiu30
 */
class MessageStatusEnumTest {

    @Test
    void shouldContainNormalStatus() {
        MessageStatusEnum status = MessageStatusEnum.getEnumByCode(0);
        assertNotNull(status);
        assertEquals(MessageStatusEnum.NORMAL, status);
    }

    @Test
    void shouldContainRecallStatus() {
        MessageStatusEnum status = MessageStatusEnum.getEnumByCode(1);
        assertNotNull(status);
        assertEquals(MessageStatusEnum.RECALL, status);
    }

    @Test
    void shouldContainDeleteStatus() {
        MessageStatusEnum status = MessageStatusEnum.getEnumByCode(2);
        assertNotNull(status);
        assertEquals(MessageStatusEnum.DELETE, status);
    }

    @Test
    void shouldReturnNullForUnknownCode() {
        assertNull(MessageStatusEnum.getEnumByCode(99));
    }

    @Test
    void shouldReturnNullForNullCode() {
        assertNull(MessageStatusEnum.getEnumByCode(null));
    }

    @Test
    void shouldReturnAllValues() {
        assertEquals(3, MessageStatusEnum.getValues().size());
    }
}
