package com.stephen.cloud.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.stephen.cloud.api.chat.model.enums.ChatMessageTypeEnum;
import com.stephen.cloud.api.chat.model.enums.ChatRoomTypeEnum;
import com.stephen.cloud.api.chat.model.enums.MessageStatusEnum;
import com.stephen.cloud.api.chat.model.vo.ChatMessageVO;
import com.stephen.cloud.api.chat.model.vo.ChatSessionVO;
import com.stephen.cloud.chat.model.entity.ChatMessage;
import com.stephen.cloud.chat.model.entity.ChatPrivateRoom;
import com.stephen.cloud.chat.model.entity.ChatRoom;
import com.stephen.cloud.chat.model.entity.ChatRoomMember;
import com.stephen.cloud.chat.mq.producer.ChatMqProducer;
import com.stephen.cloud.chat.service.ChatPrivateRoomService;
import com.stephen.cloud.chat.service.ChatRoomMemberService;
import com.stephen.cloud.chat.service.ChatRoomService;
import com.stephen.cloud.chat.service.ChatSessionService;
import com.stephen.cloud.chat.service.UserFriendService;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.exception.BusinessException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class ChatMessageServiceImplTest {

    private TestableChatMessageServiceImpl chatMessageService;
    private FakeChatMqProducer chatMqProducer;
    private ChatRoom room;
    private boolean member;
    private boolean mutualFriend;
    private ChatPrivateRoom privateRoom;
    private ChatRoomMember roomMember;
    private ChatSessionVO sessionVO;

    @BeforeEach
    void setUp() {
        chatMessageService = new TestableChatMessageServiceImpl();
        chatMqProducer = new FakeChatMqProducer();
        room = new ChatRoom();
        room.setId(1L);
        room.setType(ChatRoomTypeEnum.PRIVATE.getCode());
        member = true;
        mutualFriend = true;
        privateRoom = new ChatPrivateRoom();
        privateRoom.setRoomId(1L);
        privateRoom.setUserLow(1L);
        privateRoom.setUserHigh(2L);
        roomMember = new ChatRoomMember();
        roomMember.setRoomId(1L);
        roomMember.setUserId(1L);
        sessionVO = new ChatSessionVO();
        sessionVO.setRoomId(1L);
        sessionVO.setUnreadCount(0);

        ReflectionTestUtils.setField(chatMessageService, "chatRoomMemberService", createChatRoomMemberService());
        ReflectionTestUtils.setField(chatMessageService, "chatMqProducer", chatMqProducer);
        ReflectionTestUtils.setField(chatMessageService, "chatRoomService", createChatRoomService());
        ReflectionTestUtils.setField(chatMessageService, "chatPrivateRoomService", createChatPrivateRoomService());
        ReflectionTestUtils.setField(chatMessageService, "chatSessionService", createChatSessionService());
        ReflectionTestUtils.setField(chatMessageService, "userFriendService", createUserFriendService());
    }

    @Test
    void shouldRejectPrivateMessageWhenUsersAreNotFriends() {
        mutualFriend = false;
        ChatMessage message = createTextMessage(1L, "c1", "hello");

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatMessageService.sendMessage(message, 1L));

        Assertions.assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
    }

    @Test
    void shouldReturnHistoryMessagesInChronologicalOrder() {
        chatMessageService.listResult = List.of(createStoredMessage(5L, 1L), createStoredMessage(4L, 1L));

        List<ChatMessageVO> history = chatMessageService.listHistoryMessages(1L, null, 20, 1L);

        Assertions.assertEquals(List.of(4L, 5L), history.stream().map(ChatMessageVO::getId).toList());
    }

    @Test
    void shouldRejectReadWhenMessageDoesNotBelongToRoom() {
        ChatMessage stored = createStoredMessage(8L, 1L);
        stored.setRoomId(2L);
        chatMessageService.messageById = stored;

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatMessageService.markMessageRead(1L, 8L, 1L));

        Assertions.assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        Assertions.assertNull(chatMqProducer.lastReadPayload);
        Assertions.assertNull(chatMqProducer.lastSessionUpdateUserId);
    }

    @Test
    void shouldRejectRecallByDifferentSender() {
        ChatMessage stored = createStoredMessage(9L, 1L);
        stored.setFromUserId(2L);
        chatMessageService.messageById = stored;

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatMessageService.recallMessage(9L, 1L));

        Assertions.assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
    }

    private ChatMessage createTextMessage(Long roomId, String clientMsgId, String content) {
        ChatMessage message = new ChatMessage();
        message.setRoomId(roomId);
        message.setClientMsgId(clientMsgId);
        message.setType(ChatMessageTypeEnum.TEXT.getCode());
        message.setContent(content);
        return message;
    }

    private ChatMessage createStoredMessage(Long id, Long roomId) {
        ChatMessage message = createTextMessage(roomId, "c" + id, "hello-" + id);
        message.setId(id);
        message.setFromUserId(1L);
        message.setStatus(MessageStatusEnum.NORMAL.getCode());
        message.setCreateTime(new Date());
        return message;
    }

    private ChatRoomService createChatRoomService() {
        return (ChatRoomService) Proxy.newProxyInstance(
                ChatRoomService.class.getClassLoader(),
                new Class[]{ChatRoomService.class},
                (proxy, method, args) -> {
                    if ("getById".equals(method.getName())) {
                        return room;
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private ChatPrivateRoomService createChatPrivateRoomService() {
        return (ChatPrivateRoomService) Proxy.newProxyInstance(
                ChatPrivateRoomService.class.getClassLoader(),
                new Class[]{ChatPrivateRoomService.class},
                (proxy, method, args) -> {
                    if ("getOne".equals(method.getName())) {
                        return privateRoom;
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private ChatRoomMemberService createChatRoomMemberService() {
        return (ChatRoomMemberService) Proxy.newProxyInstance(
                ChatRoomMemberService.class.getClassLoader(),
                new Class[]{ChatRoomMemberService.class},
                (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "isMember" -> member;
                        case "getMember" -> roomMember;
                        case "listByRoomId" -> List.of(roomMember);
                        case "updateById" -> true;
                        default -> defaultValue(method.getReturnType());
                    };
                }
        );
    }

    private ChatSessionService createChatSessionService() {
        return (ChatSessionService) Proxy.newProxyInstance(
                ChatSessionService.class.getClassLoader(),
                new Class[]{ChatSessionService.class},
                (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "getSessionVO" -> sessionVO;
                        case "update" -> {
                            chatMessageService.sessionUpdateInvoked = true;
                            yield true;
                        }
                        default -> defaultValue(method.getReturnType());
                    };
                }
        );
    }

    private UserFriendService createUserFriendService() {
        return (UserFriendService) Proxy.newProxyInstance(
                UserFriendService.class.getClassLoader(),
                new Class[]{UserFriendService.class},
                (proxy, method, args) -> {
                    if ("isMutualFriend".equals(method.getName())) {
                        return mutualFriend;
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private static class TestableChatMessageServiceImpl extends ChatMessageServiceImpl {
        private ChatMessage existingByClient;
        private ChatMessage messageById;
        private List<ChatMessage> listResult = new ArrayList<>();
        private boolean saveResult = true;
        private boolean updateResult = true;
        private boolean sessionUpdateInvoked;

        @Override
        public ChatMessage getOne(Wrapper<ChatMessage> queryWrapper) {
            return existingByClient;
        }

        @Override
        public ChatMessage getById(java.io.Serializable id) {
            return messageById;
        }

        @Override
        public boolean save(ChatMessage entity) {
            if (saveResult && entity.getId() == null) {
                entity.setId(100L);
                entity.setCreateTime(new Date());
            }
            this.messageById = entity;
            return saveResult;
        }

        @Override
        public boolean updateById(ChatMessage entity) {
            this.messageById = entity;
            return updateResult;
        }

        @Override
        public List<ChatMessage> list(Wrapper<ChatMessage> queryWrapper) {
            return new ArrayList<>(listResult);
        }

        @Override
        public List<ChatMessageVO> getChatMessageVO(List<ChatMessage> chatMessageList, jakarta.servlet.http.HttpServletRequest request) {
            return chatMessageList.stream()
                    .map(item -> ChatMessageVO.builder()
                            .id(item.getId())
                            .roomId(item.getRoomId())
                            .fromUserId(item.getFromUserId())
                            .content(item.getContent())
                            .type(item.getType())
                            .status(item.getStatus())
                            .build())
                    .toList();
        }

        @Override
        public ChatMessageVO getChatMessageVO(ChatMessage chatMessage, jakarta.servlet.http.HttpServletRequest request) {
            if (chatMessage == null) {
                return null;
            }
            return ChatMessageVO.builder()
                    .id(chatMessage.getId())
                    .roomId(chatMessage.getRoomId())
                    .fromUserId(chatMessage.getFromUserId())
                    .content(chatMessage.getContent())
                    .type(chatMessage.getType())
                    .status(chatMessage.getStatus())
                    .build();
        }
    }

    private static class FakeChatMqProducer extends ChatMqProducer {
        private Object lastReadPayload;
        private Long lastSessionUpdateUserId;

        @Override
        public void sendMessageRead(Long roomId, Object data, String bizId) {
            this.lastReadPayload = data;
        }

        @Override
        public void sendSessionUpdate(Long userId, Long roomId, Object data, String bizId) {
            this.lastSessionUpdateUserId = userId;
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
