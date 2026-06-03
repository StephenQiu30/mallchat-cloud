package com.stephen.cloud.api.chat.model.vo;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 消息投递状态视图对象测试
 *
 * @author StephenQiu30
 */
class ChatMessageDeliveryVOTest {

    @Test
    void shouldBuildWithAllFields() {
        ChatMessageDeliveryVO vo = ChatMessageDeliveryVO.builder()
                .id(1L)
                .messageId(100L)
                .userId(1L)
                .status(0)
                .statusDesc("待投递")
                .retryCount(0)
                .lastRetryAt(null)
                .createTime(new Date())
                .build();

        assertEquals(1L, vo.getId());
        assertEquals(100L, vo.getMessageId());
        assertEquals(1L, vo.getUserId());
        assertEquals(0, vo.getStatus());
        assertEquals("待投递", vo.getStatusDesc());
        assertEquals(0, vo.getRetryCount());
        assertNull(vo.getLastRetryAt());
        assertNotNull(vo.getCreateTime());
    }

    @Test
    void shouldSupportFailedStatus() {
        ChatMessageDeliveryVO vo = ChatMessageDeliveryVO.builder()
                .id(2L)
                .messageId(100L)
                .userId(2L)
                .status(2)
                .statusDesc("投递失败")
                .retryCount(3)
                .lastRetryAt(new Date())
                .createTime(new Date())
                .build();

        assertEquals(2, vo.getStatus());
        assertEquals(3, vo.getRetryCount());
        assertNotNull(vo.getLastRetryAt());
    }

    @Test
    void shouldHaveNoArgsConstructor() {
        ChatMessageDeliveryVO vo = new ChatMessageDeliveryVO();
        assertNotNull(vo);
    }
}
