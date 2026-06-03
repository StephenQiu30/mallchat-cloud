package com.stephen.cloud.api.chat.model.vo;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 消息撤回记录视图对象测试
 *
 * @author StephenQiu30
 */
class ChatMessageRevokeVOTest {

    @Test
    void shouldBuildWithAllFields() {
        ChatMessageRevokeVO vo = ChatMessageRevokeVO.builder()
                .id(1L)
                .messageId(100L)
                .revokerId(1L)
                .revokedAt(new Date())
                .reason("发错了")
                .build();

        assertEquals(1L, vo.getId());
        assertEquals(100L, vo.getMessageId());
        assertEquals(1L, vo.getRevokerId());
        assertNotNull(vo.getRevokedAt());
        assertEquals("发错了", vo.getReason());
    }

    @Test
    void shouldAllowNullReason() {
        ChatMessageRevokeVO vo = ChatMessageRevokeVO.builder()
                .id(1L)
                .messageId(100L)
                .revokerId(1L)
                .revokedAt(new Date())
                .build();

        assertNull(vo.getReason());
    }

    @Test
    void shouldHaveNoArgsConstructor() {
        ChatMessageRevokeVO vo = new ChatMessageRevokeVO();
        assertNotNull(vo);
    }
}
