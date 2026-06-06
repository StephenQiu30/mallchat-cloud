package com.stephen.cloud.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.stephen.cloud.chat.event.ChatMessageSentEvent;
import com.stephen.cloud.chat.listener.ChatSessionListener;
import com.stephen.cloud.chat.model.entity.ChatMessage;
import com.stephen.cloud.chat.model.entity.ChatRoomMember;
import com.stephen.cloud.chat.model.entity.ChatSession;
import com.stephen.cloud.chat.mq.producer.ChatMqProducer;
import com.stephen.cloud.chat.service.ChatRoomMemberService;
import com.stephen.cloud.chat.service.ChatSessionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * 群会话一致性 focused tests
 *
 * 验收标准：
 * 1. 群消息更新相关成员会话
 * 2. 成员退出后不再产生新的未读累积
 * 3. 重复群消息不重复增加未读
 * 4. 成员变化不得破坏历史消息事实
 */
class GroupSessionConsistencyTest {

    private ChatSessionListener listener;
    private List<ChatRoomMember> activeMembers;
    private List<Long> capturedBatchUserIds;
    private Long capturedRoomId;
    private Long capturedMessageId;
    private Long capturedSenderId;

    // TestableChatSessionServiceImpl to capture batch update behavior
    private TestableChatSessionServiceImpl chatSessionService;
    private FakeChatMqProducer chatMqProducer;

    @BeforeEach
    void setUp() {
        activeMembers = new ArrayList<>();
        capturedBatchUserIds = new ArrayList<>();
        chatMqProducer = new FakeChatMqProducer();

        listener = new ChatSessionListener();
        ReflectionTestUtils.setField(listener, "chatRoomMemberService", createChatRoomMemberService());
        ReflectionTestUtils.setField(listener, "chatSessionService", createChatSessionService());
        ReflectionTestUtils.setField(listener, "chatMqProducer", chatMqProducer);
    }

    /**
     * 目标 2：成员退出后不再产生新的未读累积
     *
     * 场景：成员 B 已退出群聊（ChatRoomMember 行已软删除），
     * 新消息到达时，listByRoomId 不返回已退出成员，
     * 因此 updateSessionBatch 不包含已退出成员。
     */
    @Test
    void shouldNotUpdateSessionForDepartedMember() {
        // Arrange: room has 3 members, but member 2 has left (soft-deleted)
        // listByRoomId only returns active members (respecting @TableLogic)
        activeMembers = List.of(buildMember(1L), buildMember(3L));

        ChatMessage message = new ChatMessage();
        message.setId(100L);
        message.setRoomId(10L);

        // Act: sender (user 1) sends a message
        listener.onChatMessageSent(new ChatMessageSentEvent(this, message, 1L));

        // Assert: departed member (user 2) is NOT in the batch update
        Assertions.assertEquals(List.of(1L, 3L), capturedBatchUserIds,
                "Departed member should not be included in session batch update");
        Assertions.assertFalse(capturedBatchUserIds.contains(2L),
                "Departed member (user 2) should not receive session update");
    }

    /**
     * 目标 3：重复群消息不重复增加未读
     *
     * 场景：同一条消息被重复投递（如 MQ retry），
     * 第二次投递时 session.lastMessageId 已等于 incomingMessageId，
     * updateSessionBatch 跳过该 session，不增加 unreadCount。
     */
    @Test
    void shouldNotDoubleIncrementUnreadOnDuplicateMessage() {
        // Arrange: user 2 has existing session with lastMessageId = 100
        ChatSession existingSession = new ChatSession();
        existingSession.setUserId(2L);
        existingSession.setRoomId(10L);
        existingSession.setLastMessageId(100L);
        existingSession.setUnreadCount(3);

        activeMembers = List.of(buildMember(1L), buildMember(2L));

        // Use a testable service that returns existing sessions
        TestableChatSessionServiceImpl testableService = new TestableChatSessionServiceImpl();
        testableService.listResult = List.of(existingSession);
        ReflectionTestUtils.setField(listener, "chatSessionService", testableService);

        ChatMessage message = new ChatMessage();
        message.setId(100L); // Same message ID as existing session's lastMessageId
        message.setRoomId(10L);

        // Act: same message delivered again
        listener.onChatMessageSent(new ChatMessageSentEvent(this, message, 1L));

        // Assert: unread count should NOT be incremented (dedup guard)
        // The session with lastMessageId=100 should be skipped by isDuplicateOrStaleMessage
        ChatSession savedReceiver = testableService.lastBatchSaved.stream()
                .filter(s -> s.getUserId().equals(2L))
                .findFirst()
                .orElse(null);
        Assertions.assertNotNull(savedReceiver, "Receiver session should be in batch");
        Assertions.assertEquals(3, savedReceiver.getUnreadCount(),
                "Unread count should not increase on duplicate message delivery");
        Assertions.assertEquals(100L, savedReceiver.getLastMessageId(),
                "lastMessageId should remain unchanged");
    }

    /**
     * 目标 4：成员变化不得破坏历史消息事实
     *
     * 场景：成员 B 退出后，新消息到达时 listener 只更新会话，
     * 不查询或修改 ChatMessage 表。
     * 验证方式：ChatSessionService proxy 只捕获 updateSessionBatch 调用，
     * 不捕获任何消息相关调用（因为 listener 不使用消息服务）。
     */
    @Test
    void shouldNotDestroyHistoricalMessagesWhenMemberChanges() {
        // Arrange: active members after B left
        activeMembers = List.of(buildMember(1L), buildMember(3L));

        ChatMessage message = new ChatMessage();
        message.setId(100L);
        message.setRoomId(10L);

        // Act
        listener.onChatMessageSent(new ChatMessageSentEvent(this, message, 1L));

        // Assert: only updateSessionBatch was called (no message queries)
        // The proxy will throw UnsupportedOperationException for unexpected calls,
        // so reaching this point proves the listener doesn't touch message services.
        Assertions.assertEquals(List.of(1L, 3L), capturedBatchUserIds,
                "Only active members should be in session update");
        Assertions.assertEquals(100L, capturedMessageId,
                "Message ID should be passed correctly");
    }

    /**
     * 目标 1：群消息更新相关成员会话
     *
     * 场景：群消息到达时，所有活跃成员的会话都应被更新。
     */
    @Test
    void shouldUpdateAllActiveMemberSessions() {
        activeMembers = List.of(buildMember(1L), buildMember(2L), buildMember(3L));

        ChatMessage message = new ChatMessage();
        message.setId(50L);
        message.setRoomId(10L);

        listener.onChatMessageSent(new ChatMessageSentEvent(this, message, 1L));

        Assertions.assertEquals(List.of(1L, 2L, 3L), capturedBatchUserIds,
                "All active members should be included in session update");
        Assertions.assertEquals(10L, capturedRoomId);
        Assertions.assertEquals(50L, capturedMessageId);
        Assertions.assertEquals(1L, capturedSenderId);
    }

    /**
     * 补充：退出成员的历史会话数据不被清除
     *
     * 场景：成员 B 退出后，其 ChatSession 行仍然存在（不被删除），
     * 只是不再收到新的未读更新。
     * 下次 B 重新加入时，历史会话仍然可用。
     */
    @Test
    void shouldPreserveDepartedMemberSessionData() {
        // Arrange: departed member's session still exists in DB
        ChatSession departedMemberSession = new ChatSession();
        departedMemberSession.setUserId(2L);
        departedMemberSession.setRoomId(10L);
        departedMemberSession.setLastMessageId(90L);
        departedMemberSession.setUnreadCount(5);

        // Only active members are returned by listByRoomId
        activeMembers = List.of(buildMember(1L), buildMember(3L));

        TestableChatSessionServiceImpl testableService = new TestableChatSessionServiceImpl();
        testableService.listResult = List.of(departedMemberSession);
        ReflectionTestUtils.setField(listener, "chatSessionService", testableService);

        ChatMessage message = new ChatMessage();
        message.setId(100L);
        message.setRoomId(10L);

        // Act: message sent after member B left
        listener.onChatMessageSent(new ChatMessageSentEvent(this, message, 1L));

        // Assert: departed member's session is NOT in the update batch
        // (because listByRoomId excludes them)
        boolean departedInBatch = testableService.lastBatchSaved.stream()
                .anyMatch(s -> s.getUserId().equals(2L));
        Assertions.assertFalse(departedInBatch,
                "Departed member's session should not be in update batch");

        // Verify: departed member's original session data is untouched
        Assertions.assertEquals(90L, departedMemberSession.getLastMessageId(),
                "Departed member's lastMessageId should not change");
        Assertions.assertEquals(5, departedMemberSession.getUnreadCount(),
                "Departed member's unreadCount should not change");
    }

    // --- Helper methods ---

    private ChatRoomMember buildMember(Long userId) {
        ChatRoomMember member = new ChatRoomMember();
        member.setRoomId(10L);
        member.setUserId(userId);
        member.setIsDelete(0);
        return member;
    }

    private ChatRoomMemberService createChatRoomMemberService() {
        return (ChatRoomMemberService) Proxy.newProxyInstance(
                ChatRoomMemberService.class.getClassLoader(),
                new Class[]{ChatRoomMemberService.class},
                (proxy, method, args) -> {
                    if ("listByRoomId".equals(method.getName())) {
                        return new ArrayList<>(activeMembers);
                    }
                    throw new UnsupportedOperationException("Unexpected ChatRoomMemberService call: " + method.getName());
                }
        );
    }

    private ChatSessionService createChatSessionService() {
        return (ChatSessionService) Proxy.newProxyInstance(
                ChatSessionService.class.getClassLoader(),
                new Class[]{ChatSessionService.class},
                (proxy, method, args) -> {
                    if ("updateSessionBatch".equals(method.getName())) {
                        @SuppressWarnings("unchecked")
                        List<Long> userIds = (List<Long>) args[0];
                        capturedBatchUserIds = new ArrayList<>(userIds);
                        capturedRoomId = (Long) args[1];
                        capturedMessageId = (Long) args[2];
                        capturedSenderId = (Long) args[3];
                        return null;
                    }
                    if ("getSessionVO".equals(method.getName())) {
                        return new com.stephen.cloud.api.chat.model.vo.ChatSessionVO();
                    }
                    throw new UnsupportedOperationException("Unexpected ChatSessionService call: " + method.getName());
                }
        );
    }

    private static class TestableChatSessionServiceImpl extends ChatSessionServiceImpl {
        private List<ChatSession> listResult = new ArrayList<>();
        private List<ChatSession> lastBatchSaved = new ArrayList<>();

        @Override
        public List<ChatSession> list(Wrapper<ChatSession> queryWrapper) {
            return new ArrayList<>(listResult);
        }

        @Override
        public boolean saveOrUpdateBatch(java.util.Collection<ChatSession> entityList) {
            this.lastBatchSaved = new ArrayList<>(entityList);
            return true;
        }

        @Override
        public com.stephen.cloud.api.chat.model.vo.ChatSessionVO getSessionVO(Long roomId, Long userId) {
            return new com.stephen.cloud.api.chat.model.vo.ChatSessionVO();
        }
    }

    private static class FakeChatMqProducer extends ChatMqProducer {
        @Override
        public void sendSessionUpdate(Long userId, Long roomId, Object data, String bizId) {
            // no-op
        }
    }
}
