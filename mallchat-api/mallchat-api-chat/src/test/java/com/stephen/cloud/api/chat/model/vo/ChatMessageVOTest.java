package com.stephen.cloud.api.chat.model.vo;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 聊天消息视图对象测试
 *
 * @author StephenQiu30
 */
class ChatMessageVOTest {

    @Test
    void shouldBuildWithAllFields() {
        ReplyMsgVO replyMsg = ReplyMsgVO.builder()
                .id(50L)
                .userName("Alice")
                .content("original message")
                .type(1)
                .build();

        ChatMessageVO vo = ChatMessageVO.builder()
                .id(1L)
                .roomId(1L)
                .fromUserId(1L)
                .clientMsgId("pc-1710000000000-1")
                .fromUserName("Stephen")
                .fromUserAvatar("https://example.com/avatar.png")
                .content("Hello")
                .type(1)
                .extra(null)
                .replyMsg(replyMsg)
                .status(0)
                .createTime(new Date())
                .build();

        assertEquals(1L, vo.getId());
        assertEquals(1L, vo.getRoomId());
        assertEquals(1L, vo.getFromUserId());
        assertEquals("pc-1710000000000-1", vo.getClientMsgId());
        assertEquals("Stephen", vo.getFromUserName());
        assertEquals("Hello", vo.getContent());
        assertEquals(1, vo.getType());
        assertEquals(0, vo.getStatus());
        assertNotNull(vo.getReplyMsg());
        assertEquals(50L, vo.getReplyMsg().getId());
    }

    @Test
    void shouldSupportDeliveryStatusField() {
        ChatMessageVO vo = ChatMessageVO.builder()
                .id(1L)
                .roomId(1L)
                .fromUserId(1L)
                .clientMsgId("pc-1710000000000-1")
                .content("Hello")
                .type(1)
                .status(0)
                .deliveryStatus(0)
                .createTime(new Date())
                .build();

        assertEquals(0, vo.getDeliveryStatus(), "应支持投递状态字段");
    }

    @Test
    void shouldHaveNoArgsConstructor() {
        ChatMessageVO vo = new ChatMessageVO();
        assertNotNull(vo);
    }

    @Test
    void shouldHaveAllArgsConstructor() {
        ChatMessageVO vo = new ChatMessageVO(
                1L, 1L, 1L, "client-1", "User", "avatar",
                "content", 1, "extra", null, 0, 0, new Date()
        );
        assertEquals(1L, vo.getId());
    }
}
