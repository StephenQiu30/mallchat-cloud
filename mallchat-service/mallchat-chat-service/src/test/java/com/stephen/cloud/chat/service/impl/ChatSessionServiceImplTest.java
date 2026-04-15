package com.stephen.cloud.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.stephen.cloud.api.chat.model.enums.ChatRoomTypeEnum;
import com.stephen.cloud.api.chat.model.enums.MessageStatusEnum;
import com.stephen.cloud.api.chat.model.vo.ChatSessionVO;
import com.stephen.cloud.api.user.client.UserFeignClient;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.chat.model.entity.ChatGroupInfo;
import com.stephen.cloud.chat.model.entity.ChatMessage;
import com.stephen.cloud.chat.model.entity.ChatPrivateRoom;
import com.stephen.cloud.chat.model.entity.ChatRoom;
import com.stephen.cloud.chat.model.entity.ChatSession;
import com.stephen.cloud.chat.mq.producer.ChatMqProducer;
import com.stephen.cloud.chat.service.ChatGroupInfoService;
import com.stephen.cloud.chat.service.ChatMessageService;
import com.stephen.cloud.chat.service.ChatOnlineStatusService;
import com.stephen.cloud.chat.service.ChatPrivateRoomService;
import com.stephen.cloud.chat.service.ChatRoomService;
import com.stephen.cloud.common.common.BaseResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

class ChatSessionServiceImplTest {

    private TestableChatSessionServiceImpl chatSessionService;
    private FakeChatMqProducer chatMqProducer;
    private List<ChatRoom> rooms;
    private List<ChatMessage> messages;
    private List<ChatGroupInfo> groups;
    private List<ChatPrivateRoom> privateRooms;
    private List<UserVO> users;
    private Map<Long, Integer> onlineStatusMap;

    @BeforeEach
    void setUp() {
        chatSessionService = new TestableChatSessionServiceImpl();
        chatMqProducer = new FakeChatMqProducer();
        rooms = new ArrayList<>();
        messages = new ArrayList<>();
        groups = new ArrayList<>();
        privateRooms = new ArrayList<>();
        users = new ArrayList<>();
        onlineStatusMap = Map.of();

        ReflectionTestUtils.setField(chatSessionService, "chatRoomService", createChatRoomService());
        ReflectionTestUtils.setField(chatSessionService, "chatMessageService", createChatMessageService());
        ReflectionTestUtils.setField(chatSessionService, "userFeignClient", createUserFeignClient());
        ReflectionTestUtils.setField(chatSessionService, "chatGroupInfoService", createChatGroupInfoService());
        ReflectionTestUtils.setField(chatSessionService, "chatPrivateRoomService", createChatPrivateRoomService());
        ReflectionTestUtils.setField(chatSessionService, "chatOnlineStatusService", createChatOnlineStatusService());
        ReflectionTestUtils.setField(chatSessionService, "chatMqProducer", chatMqProducer);
    }

    @Test
    void shouldBuildPrivateSessionDisplayWithPeerAndPreview() {
        ChatSession session = createSession(1L, 10L, 2, 0);
        chatSessionService.listResult = List.of(session);
        rooms = List.of(createRoom(1L, ChatRoomTypeEnum.PRIVATE.getCode(), null));
        privateRooms = List.of(createPrivateRoom(1L, 1L, 2L));
        messages = List.of(createMessage(10L, 1L, "hello"));
        users = List.of(createUser(2L, "peer", "avatar-2"));
        onlineStatusMap = Map.of(2L, 1);

        List<ChatSessionVO> sessions = chatSessionService.listMySessions(1L);

        Assertions.assertEquals(1, sessions.size());
        ChatSessionVO sessionVO = sessions.get(0);
        Assertions.assertEquals("peer", sessionVO.getName());
        Assertions.assertEquals("avatar-2", sessionVO.getAvatar());
        Assertions.assertEquals(1, sessionVO.getOnlineStatus());
        Assertions.assertEquals("hello", sessionVO.getLastMessage());
        Assertions.assertEquals(2, sessionVO.getUnreadCount());
    }

    @Test
    void shouldPushSessionUpdateAfterTopOperation() {
        chatSessionService.getOneResult = createSession(1L, 10L, 1, 0);
        rooms = List.of(createRoom(1L, ChatRoomTypeEnum.PRIVATE.getCode(), null));
        privateRooms = List.of(createPrivateRoom(1L, 1L, 2L));
        messages = List.of(createMessage(10L, 1L, "top"));
        users = List.of(createUser(2L, "peer", "avatar-2"));
        onlineStatusMap = Map.of(2L, 1);

        boolean result = chatSessionService.topSession(1L, 1L, 1);

        Assertions.assertTrue(result);
        Assertions.assertEquals(1, chatSessionService.getOneResult.getTopStatus());
        Assertions.assertEquals(1L, chatMqProducer.lastSessionUpdateUserId);
        Assertions.assertEquals(1L, chatMqProducer.lastSessionUpdateRoomId);
        Assertions.assertTrue(chatMqProducer.lastSessionUpdatePayload instanceof ChatSessionVO);
    }

    @Test
    void shouldPushSessionDeleteAfterDeleteOperation() {
        chatSessionService.removeResult = true;

        boolean result = chatSessionService.deleteSession(1L, 1L);

        Assertions.assertTrue(result);
        Assertions.assertEquals(1L, chatMqProducer.lastSessionDeleteUserId);
        Assertions.assertEquals(1L, chatMqProducer.lastSessionDeleteRoomId);
    }

    @Test
    void shouldIncrementUnreadOnlyForReceiversInBatchUpdate() {
        ChatSession senderSession = createSession(1L, 9L, 3, 0);
        senderSession.setUserId(1L);
        ChatSession receiverSession = createSession(1L, 8L, 2, 0);
        receiverSession.setUserId(2L);
        chatSessionService.listResult = List.of(senderSession, receiverSession);

        chatSessionService.updateSessionBatch(List.of(1L, 2L), 1L, 11L, 1L);

        Assertions.assertEquals(2, chatSessionService.lastBatchSaved.size());
        ChatSession savedSender = chatSessionService.lastBatchSaved.stream()
                .filter(item -> item.getUserId().equals(1L))
                .findFirst()
                .orElseThrow();
        ChatSession savedReceiver = chatSessionService.lastBatchSaved.stream()
                .filter(item -> item.getUserId().equals(2L))
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals(3, savedSender.getUnreadCount());
        Assertions.assertEquals(3, savedReceiver.getUnreadCount());
        Assertions.assertEquals(11L, savedSender.getLastMessageId());
        Assertions.assertEquals(11L, savedReceiver.getLastMessageId());
    }

    private ChatSession createSession(Long roomId, Long lastMessageId, Integer unreadCount, Integer topStatus) {
        ChatSession session = new ChatSession();
        session.setRoomId(roomId);
        session.setUserId(1L);
        session.setLastMessageId(lastMessageId);
        session.setUnreadCount(unreadCount);
        session.setTopStatus(topStatus);
        session.setActiveTime(new Date());
        return session;
    }

    private ChatRoom createRoom(Long roomId, Integer type, String name) {
        ChatRoom room = new ChatRoom();
        room.setId(roomId);
        room.setType(type);
        room.setName(name);
        return room;
    }

    private ChatPrivateRoom createPrivateRoom(Long roomId, Long userLow, Long userHigh) {
        ChatPrivateRoom room = new ChatPrivateRoom();
        room.setRoomId(roomId);
        room.setUserLow(userLow);
        room.setUserHigh(userHigh);
        return room;
    }

    private ChatMessage createMessage(Long id, Long roomId, String content) {
        ChatMessage message = new ChatMessage();
        message.setId(id);
        message.setRoomId(roomId);
        message.setFromUserId(2L);
        message.setContent(content);
        message.setStatus(MessageStatusEnum.NORMAL.getCode());
        message.setType(1);
        message.setCreateTime(new Date());
        return message;
    }

    private UserVO createUser(Long userId, String userName, String avatar) {
        UserVO userVO = new UserVO();
        userVO.setId(userId);
        userVO.setUserName(userName);
        userVO.setUserAvatar(avatar);
        return userVO;
    }

    private ChatRoomService createChatRoomService() {
        return (ChatRoomService) Proxy.newProxyInstance(
                ChatRoomService.class.getClassLoader(),
                new Class[]{ChatRoomService.class},
                (proxy, method, args) -> {
                    if ("listByIds".equals(method.getName())) {
                        return rooms;
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private ChatMessageService createChatMessageService() {
        return (ChatMessageService) Proxy.newProxyInstance(
                ChatMessageService.class.getClassLoader(),
                new Class[]{ChatMessageService.class},
                (proxy, method, args) -> {
                    if ("listByIds".equals(method.getName())) {
                        return messages;
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private UserFeignClient createUserFeignClient() {
        return (UserFeignClient) Proxy.newProxyInstance(
                UserFeignClient.class.getClassLoader(),
                new Class[]{UserFeignClient.class},
                (proxy, method, args) -> {
                    if ("getUserVOByIds".equals(method.getName())) {
                        return new BaseResponse<>(0, users);
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private ChatGroupInfoService createChatGroupInfoService() {
        return (ChatGroupInfoService) Proxy.newProxyInstance(
                ChatGroupInfoService.class.getClassLoader(),
                new Class[]{ChatGroupInfoService.class},
                (proxy, method, args) -> {
                    if ("list".equals(method.getName())) {
                        return groups;
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
                    if ("list".equals(method.getName())) {
                        return privateRooms;
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private ChatOnlineStatusService createChatOnlineStatusService() {
        return (ChatOnlineStatusService) Proxy.newProxyInstance(
                ChatOnlineStatusService.class.getClassLoader(),
                new Class[]{ChatOnlineStatusService.class},
                (proxy, method, args) -> {
                    if ("getOnlineStatusMap".equals(method.getName())) {
                        return onlineStatusMap;
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private Object defaultValue(Class<?> returnType) {
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

    private static class TestableChatSessionServiceImpl extends ChatSessionServiceImpl {
        private List<ChatSession> listResult = new ArrayList<>();
        private ChatSession getOneResult;
        private List<ChatSession> lastBatchSaved = new ArrayList<>();
        private boolean removeResult;

        @Override
        public List<ChatSession> list(Wrapper<ChatSession> queryWrapper) {
            return new ArrayList<>(listResult);
        }

        @Override
        public ChatSession getOne(Wrapper<ChatSession> queryWrapper) {
            return getOneResult;
        }

        @Override
        public boolean updateById(ChatSession entity) {
            this.getOneResult = entity;
            return true;
        }

        @Override
        public boolean remove(Wrapper<ChatSession> queryWrapper) {
            return removeResult;
        }

        @Override
        public boolean saveOrUpdateBatch(java.util.Collection<ChatSession> entityList) {
            this.lastBatchSaved = new ArrayList<>(entityList);
            return true;
        }
    }

    private static class FakeChatMqProducer extends ChatMqProducer {
        private Long lastSessionUpdateUserId;
        private Long lastSessionUpdateRoomId;
        private Object lastSessionUpdatePayload;
        private Long lastSessionDeleteUserId;
        private Long lastSessionDeleteRoomId;

        @Override
        public void sendSessionUpdate(Long userId, Long roomId, Object data, String bizId) {
            this.lastSessionUpdateUserId = userId;
            this.lastSessionUpdateRoomId = roomId;
            this.lastSessionUpdatePayload = data;
        }

        @Override
        public void sendSessionDelete(Long userId, Long roomId, String bizId) {
            this.lastSessionDeleteUserId = userId;
            this.lastSessionDeleteRoomId = roomId;
        }
    }
}
