package com.stephen.cloud.chat.listener;

import com.stephen.cloud.api.chat.model.vo.ChatSessionVO;
import com.stephen.cloud.chat.event.ChatMessageSentEvent;
import com.stephen.cloud.chat.model.entity.ChatMessage;
import com.stephen.cloud.chat.model.entity.ChatRoomMember;
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

class ChatSessionListenerTest {

    private ChatSessionListener listener;
    private FakeChatMqProducer chatMqProducer;
    private List<ChatRoomMember> roomMembers;
    private List<Long> batchUpdatedUsers;
    private Long batchUpdatedRoomId;
    private Long batchUpdatedMessageId;

    @BeforeEach
    void setUp() {
        listener = new ChatSessionListener();
        chatMqProducer = new FakeChatMqProducer();
        roomMembers = new ArrayList<>();
        batchUpdatedUsers = new ArrayList<>();

        ReflectionTestUtils.setField(listener, "chatRoomMemberService", createChatRoomMemberService());
        ReflectionTestUtils.setField(listener, "chatSessionService", createChatSessionService());
        ReflectionTestUtils.setField(listener, "chatMqProducer", chatMqProducer);
    }

    @Test
    void shouldKeepSessionBatchUpdateAndContinueWhenSessionPushThrows() {
        roomMembers = List.of(buildMember(1L), buildMember(2L));
        chatMqProducer.sessionUpdateFailureUserId = 1L;
        ChatMessage message = new ChatMessage();
        message.setId(100L);
        message.setRoomId(10L);

        Assertions.assertDoesNotThrow(() ->
                listener.onChatMessageSent(new ChatMessageSentEvent(this, message, 1L)));

        Assertions.assertEquals(List.of(1L, 2L), batchUpdatedUsers);
        Assertions.assertEquals(10L, batchUpdatedRoomId);
        Assertions.assertEquals(100L, batchUpdatedMessageId);
        Assertions.assertEquals(List.of(1L, 2L), chatMqProducer.sessionUpdateAttemptUsers);
        Assertions.assertEquals(2L, chatMqProducer.lastSessionUpdateUserId);
    }

    private ChatRoomMember buildMember(Long userId) {
        ChatRoomMember member = new ChatRoomMember();
        member.setRoomId(10L);
        member.setUserId(userId);
        return member;
    }

    private ChatRoomMemberService createChatRoomMemberService() {
        return (ChatRoomMemberService) Proxy.newProxyInstance(
                ChatRoomMemberService.class.getClassLoader(),
                new Class[]{ChatRoomMemberService.class},
                (proxy, method, args) -> {
                    if ("listByRoomId".equals(method.getName())) {
                        return roomMembers;
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private ChatSessionService createChatSessionService() {
        return (ChatSessionService) Proxy.newProxyInstance(
                ChatSessionService.class.getClassLoader(),
                new Class[]{ChatSessionService.class},
                (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "updateSessionBatch" -> {
                            @SuppressWarnings("unchecked")
                            List<Long> userIds = (List<Long>) args[0];
                            batchUpdatedUsers = new ArrayList<>(userIds);
                            batchUpdatedRoomId = (Long) args[1];
                            batchUpdatedMessageId = (Long) args[2];
                            yield null;
                        }
                        case "getSessionVO" -> {
                            ChatSessionVO vo = new ChatSessionVO();
                            vo.setRoomId((Long) args[0]);
                            yield vo;
                        }
                        default -> defaultValue(method.getReturnType());
                    };
                }
        );
    }

    private static class FakeChatMqProducer extends ChatMqProducer {
        private List<Long> sessionUpdateAttemptUsers = new ArrayList<>();
        private Long sessionUpdateFailureUserId;
        private Long lastSessionUpdateUserId;

        @Override
        public void sendSessionUpdate(Long userId, Long roomId, Object data, String bizId) {
            sessionUpdateAttemptUsers.add(userId);
            if (sessionUpdateFailureUserId != null && sessionUpdateFailureUserId.equals(userId)) {
                throw new RuntimeException("session update failed");
            }
            lastSessionUpdateUserId = userId;
        }
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        return null;
    }
}
