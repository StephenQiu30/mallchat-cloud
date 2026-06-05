package com.stephen.cloud.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.stephen.cloud.chat.model.entity.ChatRoomMember;
import com.stephen.cloud.chat.model.entity.ChatSession;
import com.stephen.cloud.chat.service.ChatRoomMemberService;
import com.stephen.cloud.chat.service.ChatSessionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GroupSessionConsistencyTest {

    private TestableChatSessionServiceImpl chatSessionService;

    @BeforeEach
    void setUp() {
        chatSessionService = new TestableChatSessionServiceImpl();
    }

    @Test
    void shouldNotIncrementUnreadWhenDuplicateGroupMessageSent() {
        // Setup: user 2 is in group 1, already received message 100
        ChatSession session = new ChatSession();
        session.setUserId(2L);
        session.setRoomId(1L);
        session.setLastMessageId(100L);
        session.setUnreadCount(1);
        chatSessionService.listResult = List.of(session);

        // Action: duplicate message 100 is processed
        chatSessionService.updateSessionBatch(List.of(2L), 1L, 100L, 1L);

        // Verify: unread count is still 1
        ChatSession updatedSession = chatSessionService.lastBatchSaved.stream()
                .filter(s -> s.getUserId().equals(2L))
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals(1, updatedSession.getUnreadCount());
    }

    @Test
    void shouldNotAccumulateUnreadForMemberWhoLeft() {
        // Setup: user 3 left the group 1.
        // Therefore, listByRoomId will NOT return user 3.
        // The listener will only pass the current members (e.g. user 2) to updateSessionBatch.
        
        ChatSession user3Session = new ChatSession();
        user3Session.setUserId(3L);
        user3Session.setRoomId(1L);
        user3Session.setLastMessageId(100L);
        user3Session.setUnreadCount(1);
        
        ChatSession user2Session = new ChatSession();
        user2Session.setUserId(2L);
        user2Session.setRoomId(1L);
        user2Session.setLastMessageId(100L);
        user2Session.setUnreadCount(1);
        
        chatSessionService.listResult = List.of(user2Session, user3Session);

        // Action: message 101 is sent, but listener only passes user 2
        chatSessionService.updateSessionBatch(List.of(2L), 1L, 101L, 1L);

        // Verify: user 2 unread incremented, user 3 not updated
        ChatSession updatedUser2 = chatSessionService.lastBatchSaved.stream()
                .filter(s -> s.getUserId().equals(2L))
                .findFirst()
                .orElse(null);
        ChatSession updatedUser3 = chatSessionService.lastBatchSaved.stream()
                .filter(s -> s.getUserId().equals(3L))
                .findFirst()
                .orElse(null);

        Assertions.assertNotNull(updatedUser2);
        Assertions.assertEquals(2, updatedUser2.getUnreadCount());
        
        // user 3 session should not be in the update batch
        Assertions.assertNull(updatedUser3);
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
    }
}
