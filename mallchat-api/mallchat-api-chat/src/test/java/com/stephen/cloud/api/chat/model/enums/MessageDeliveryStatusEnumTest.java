package com.stephen.cloud.api.chat.model.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 消息投递状态枚举测试
 *
 * @author StephenQiu30
 */
class MessageDeliveryStatusEnumTest {

    @Test
    void shouldContainPendingStatus() {
        MessageDeliveryStatusEnum status = MessageDeliveryStatusEnum.getEnumByCode(0);
        assertNotNull(status);
        assertEquals(MessageDeliveryStatusEnum.PENDING, status);
        assertEquals("待投递", status.getDesc());
    }

    @Test
    void shouldContainDeliveredStatus() {
        MessageDeliveryStatusEnum status = MessageDeliveryStatusEnum.getEnumByCode(1);
        assertNotNull(status);
        assertEquals(MessageDeliveryStatusEnum.DELIVERED, status);
        assertEquals("已投递", status.getDesc());
    }

    @Test
    void shouldContainFailedStatus() {
        MessageDeliveryStatusEnum status = MessageDeliveryStatusEnum.getEnumByCode(2);
        assertNotNull(status);
        assertEquals(MessageDeliveryStatusEnum.FAILED, status);
        assertEquals("投递失败", status.getDesc());
    }

    @Test
    void shouldReturnNullForUnknownCode() {
        assertNull(MessageDeliveryStatusEnum.getEnumByCode(99));
    }

    @Test
    void shouldReturnNullForNullCode() {
        assertNull(MessageDeliveryStatusEnum.getEnumByCode(null));
    }

    @Test
    void shouldReturnAllValues() {
        assertEquals(3, MessageDeliveryStatusEnum.getValues().size());
    }
}
