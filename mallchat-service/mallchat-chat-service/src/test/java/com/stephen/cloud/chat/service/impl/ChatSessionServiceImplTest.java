package com.stephen.cloud.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.stephen.cloud.api.chat.model.enums.ChatMessageTypeEnum;
import com.stephen.cloud.api.chat.model.enums.ChatRoomTypeEnum;
import com.stephen.cloud.api.chat.model.enums.MessageStatusEnum;
import com.stephen.cloud.api.chat.model.vo.ChatSessionVO;
import com.stephen.cloud.api.user.client.UserFeignClient;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.chat.mapper.ChatSessionMapper;
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
    private boolean member;

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
        member = true;

        ReflectionTestUtils.setField(chatSessionService, "chatRoomService", createChatRoomService());
        ReflectionTestUtils.setField(chatSessionService, "chatMessageService", createChatMessageService());
        ReflectionTestUtils.setField(chatSessionService, "userFeignClient", createUserFeignClient());
        ReflectionTestUtils.setField(chatSessionService, "chatGroupInfoService", createChatGroupInfoService());
        ReflectionTestUtils.setField(chatSessionService, "chatPrivateRoomService", createChatPrivateRoomService());
        ReflectionTestUtils.setField(chatSessionService, "chatOnlineStatusService", createChatOnlineStatusService());
        ReflectionTestUtils.setField(chatSessionService, "chatRoomMemberService", createChatRoomMemberService());
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
    void shouldBuildRichMessageSessionPreviewPlaceholders() {
        ChatSession imageSession = createSession(1L, 10L, 0, 0);
        ChatSession fileSession = createSession(2L, 11L, 0, 0);
        ChatSession voiceSession = createSession(3L, 12L, 0, 0);
        ChatSession videoSession = createSession(4L, 13L, 0, 0);
        ChatSession stickerSession = createSession(5L, 14L, 0, 0);
        chatSessionService.listResult = List.of(imageSession, fileSession, voiceSession, videoSession, stickerSession);
        rooms = List.of(
                createRoom(1L, ChatRoomTypeEnum.GROUP.getCode(), "group-1"),
                createRoom(2L, ChatRoomTypeEnum.GROUP.getCode(), "group-2"),
                createRoom(3L, ChatRoomTypeEnum.GROUP.getCode(), "group-3"),
                createRoom(4L, ChatRoomTypeEnum.GROUP.getCode(), "group-4"),
                createRoom(5L, ChatRoomTypeEnum.GROUP.getCode(), "group-5")
        );
        messages = List.of(
                createMessage(10L, 1L, "", ChatMessageTypeEnum.IMAGE),
                createMessage(11L, 2L, "", ChatMessageTypeEnum.FILE),
                createMessage(12L, 3L, "", ChatMessageTypeEnum.VOICE),
                createMessage(13L, 4L, "", ChatMessageTypeEnum.VIDEO),
                createMessage(14L, 5L, "", ChatMessageTypeEnum.STICKER)
        );

        List<ChatSessionVO> sessions = chatSessionService.listMySessions(1L);

        Assertions.assertEquals(List.of("[图片]", "[文件]", "[语音]", "[视频]", "[表情]"),
                sessions.stream().map(ChatSessionVO::getLastMessage).toList());
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
    void shouldKeepTopStatusWhenSessionUpdatePushThrows() {
        chatSessionService.getOneResult = createSession(1L, 10L, 1, 0);
        rooms = List.of(createRoom(1L, ChatRoomTypeEnum.PRIVATE.getCode(), null));
        privateRooms = List.of(createPrivateRoom(1L, 1L, 2L));
        messages = List.of(createMessage(10L, 1L, "top"));
        users = List.of(createUser(2L, "peer", "avatar-2"));
        chatMqProducer.sessionUpdateThrows = true;

        boolean result = Assertions.assertDoesNotThrow(() -> chatSessionService.topSession(1L, 1L, 1));

        Assertions.assertTrue(result);
        Assertions.assertEquals(1, chatSessionService.getOneResult.getTopStatus());
        Assertions.assertEquals(List.of(1L), chatMqProducer.sessionUpdateAttemptUsers);
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
    void shouldKeepDeleteResultWhenSessionDeletePushThrows() {
        chatSessionService.removeResult = true;
        chatMqProducer.sessionDeleteThrows = true;

        boolean result = Assertions.assertDoesNotThrow(() -> chatSessionService.deleteSession(1L, 1L));

        Assertions.assertTrue(result);
        Assertions.assertEquals(List.of(1L), chatMqProducer.sessionDeleteAttemptUsers);
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

    @Test
    void shouldNotIncrementUnreadWhenSameMessageBatchIsAppliedTwice() {
        ChatSession senderSession = createSession(1L, 11L, 3, 0);
        senderSession.setUserId(1L);
        ChatSession receiverSession = createSession(1L, 11L, 2, 0);
        receiverSession.setUserId(2L);
        chatSessionService.listResult = List.of(senderSession, receiverSession);

        chatSessionService.updateSessionBatch(List.of(1L, 2L), 1L, 11L, 1L);

        ChatSession savedSender = chatSessionService.lastBatchSaved.stream()
                .filter(item -> item.getUserId().equals(1L))
                .findFirst()
                .orElseThrow();
        ChatSession savedReceiver = chatSessionService.lastBatchSaved.stream()
                .filter(item -> item.getUserId().equals(2L))
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals(3, savedSender.getUnreadCount());
        Assertions.assertEquals(2, savedReceiver.getUnreadCount());
        Assertions.assertEquals(11L, savedSender.getLastMessageId());
        Assertions.assertEquals(11L, savedReceiver.getLastMessageId());
    }

    @Test
    void shouldNotSaveSingleSessionWhenSameMessageIsAppliedTwice() {
        ChatSession receiverSession = createSession(1L, 11L, 2, 0);
        receiverSession.setUserId(2L);
        chatSessionService.getOneResult = receiverSession;

        chatSessionService.updateSession(2L, 1L, 11L, true);

        Assertions.assertEquals(0, chatSessionService.saveOrUpdateCount);
        Assertions.assertEquals(2, receiverSession.getUnreadCount());
        Assertions.assertEquals(11L, receiverSession.getLastMessageId());
    }

    @Test
    void shouldUpdateSessionMuteStatusAndPushRefresh() {
        chatSessionService.getOneResult = createSession(1L, 10L, 1, 0);
        rooms = List.of(createRoom(1L, ChatRoomTypeEnum.GROUP.getCode(), "group"));

        boolean result = chatSessionService.muteSession(1L, 1L, 1);

        Assertions.assertTrue(result);
        Assertions.assertEquals(1, chatSessionService.getOneResult.getMuteStatus());
        Assertions.assertEquals(1L, chatMqProducer.lastSessionUpdateUserId);
        Assertions.assertTrue(chatMqProducer.lastSessionUpdatePayload instanceof ChatSessionVO);
        ChatSessionVO vo = (ChatSessionVO) chatMqProducer.lastSessionUpdatePayload;
        Assertions.assertEquals(1, vo.getMuteStatus());
    }

    @Test
    void shouldKeepMuteStatusWhenSessionUpdatePushThrows() {
        chatSessionService.getOneResult = createSession(1L, 10L, 1, 0);
        rooms = List.of(createRoom(1L, ChatRoomTypeEnum.GROUP.getCode(), "group"));
        chatMqProducer.sessionUpdateThrows = true;

        boolean result = Assertions.assertDoesNotThrow(() -> chatSessionService.muteSession(1L, 1L, 1));

        Assertions.assertTrue(result);
        Assertions.assertEquals(1, chatSessionService.getOneResult.getMuteStatus());
    }

    @Test
    void shouldRejectMuteStatusForNonMember() {
        member = false;

        com.stephen.cloud.common.exception.BusinessException exception = Assertions.assertThrows(
                com.stephen.cloud.common.exception.BusinessException.class,
                () -> chatSessionService.muteSession(1L, 1L, 1));

        Assertions.assertEquals(com.stephen.cloud.common.common.ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
    }

    @Test
    void shouldFilterMutedPushUsersButKeepSender() {
        ChatSession sender = createSession(1L, 10L, 0, 0);
        sender.setUserId(1L);
        sender.setMuteStatus(1);
        ChatSession mutedReceiver = createSession(1L, 10L, 0, 0);
        mutedReceiver.setUserId(2L);
        mutedReceiver.setMuteStatus(1);
        ChatSession normalReceiver = createSession(1L, 10L, 0, 0);
        normalReceiver.setUserId(3L);
        normalReceiver.setMuteStatus(0);
        chatSessionService.listResult = List.of(sender, mutedReceiver, normalReceiver);

        List<Long> result = chatSessionService.filterPushUserIds(1L, List.of(1L, 2L, 3L), 1L);

        Assertions.assertEquals(List.of(1L, 3L), result);
    }

    // ========== RED Tests for Session Consistency ==========

    /**
     * RED Test: Duplicate message should NOT increase unread count
     *
     * Scenario: Same message (same lastMessageId) is applied twice to a session.
     * Expected: Unread count should NOT increase on second application.
     *
     * Current behavior may fail if: read-modify-write race condition exists
     */
    @Test
    void shouldNotIncrementUnreadOnDuplicateMessageApplication() {
        // Session already has lastMessageId = 11 and unreadCount = 2
        ChatSession session = createSession(1L, 11L, 2, 0);
        session.setUserId(2L);
        chatSessionService.getOneResult = session;

        // Apply same message again (duplicate)
        chatSessionService.updateSession(2L, 1L, 11L, true);

        // Should NOT increment unread - should remain 2
        Assertions.assertEquals(2, session.getUnreadCount(),
            "Duplicate message should NOT increment unread count");
    }

    /**
     * RED Test: Out-of-order message should NOT overwrite newer lastMessageId
     *
     * Scenario: Older message (lower ID) arrives after newer message (higher ID).
     * Expected: lastMessageId should NOT be downgraded to older message.
     *
     * Current behavior: Should be protected by isDuplicateOrStaleMessage check.
     * This test ensures that protection works correctly.
     */
    @Test
    void shouldNotDowngradeLastMessageIdWithOutOfOrderMessage() {
        // Session already has lastMessageId = 15 (newer message)
        ChatSession session = createSession(1L, 15L, 3, 0);
        session.setUserId(2L);
        chatSessionService.getOneResult = session;

        // Try to apply older message (messageId = 10)
        chatSessionService.updateSession(2L, 1L, 10L, true);

        // Should NOT change lastMessageId - should remain 15
        Assertions.assertEquals(15L, session.getLastMessageId(),
            "Out-of-order (older) message should NOT overwrite newer lastMessageId");
    }

    /**
     * RED Test: Stale message (same ID) should not trigger any update
     *
     * Scenario: Same message ID is applied to session that already has it as lastMessage.
     * Expected: No changes to unread count or lastMessageId.
     */
    @Test
    void shouldNotModifySessionForStaleSameIdMessage() {
        ChatSession session = createSession(1L, 20L, 5, 0);
        session.setUserId(2L);
        chatSessionService.getOneResult = session;

        // Reset tracking to verify no saves happen
        chatSessionService.saveOrUpdateCount = 0;

        // Apply exact same messageId
        chatSessionService.updateSession(2L, 1L, 20L, true);

        // saveOrUpdate should NOT be called for stale message
        Assertions.assertEquals(0, chatSessionService.saveOrUpdateCount,
            "Stale message (same ID) should not trigger saveOrUpdate");
    }

    /**
     * RED Test: Batch update should handle duplicate message correctly
     *
     * Scenario: Batch update with messageId that matches current lastMessageId.
     * Expected: Only update lastMessageId (same value is fine), but don't increment unread.
     */
    @Test
    void batchUpdateShouldNotIncrementUnreadForDuplicateMessage() {
        // Receiver already has messageId = 11 as lastMessage
        ChatSession receiverSession = createSession(1L, 11L, 2, 0);
        receiverSession.setUserId(2L);

        chatSessionService.listResult = List.of(receiverSession);

        // Batch update with same messageId - this is a duplicate/redundant update
        chatSessionService.updateSessionBatch(List.of(2L), 1L, 11L, 1L);

        ChatSession savedReceiver = chatSessionService.lastBatchSaved.stream()
                .filter(item -> item.getUserId().equals(2L))
                .findFirst()
                .orElseThrow();

        // Unread should remain 2, not become 3
        Assertions.assertEquals(2, savedReceiver.getUnreadCount(),
            "Batch duplicate should NOT increment receiver unread count");
    }

    /**
     * RED Test: Concurrent-like batch update should be idempotent
     *
     * Scenario: Simulate what would happen if batch update is called twice rapidly.
     * The second call with same messageId should not cause issues.
     */
    @Test
    void batchUpdateShouldBeIdempotentForSameMessageId() {
        ChatSession receiverSession = createSession(1L, 11L, 2, 0);
        receiverSession.setUserId(2L);
        chatSessionService.listResult = List.of(receiverSession);

        // First batch update
        chatSessionService.updateSessionBatch(List.of(2L), 1L, 11L, 1L);

        // Get the saved state
        ChatSession firstSaved = chatSessionService.lastBatchSaved.stream()
                .filter(item -> item.getUserId().equals(2L))
                .findFirst()
                .orElseThrow();

        // Simulate second batch update with same messageId
        chatSessionService.listResult = List.of(firstSaved); // Use saved state
        chatSessionService.updateSessionBatch(List.of(2L), 1L, 11L, 1L);

        ChatSession secondSaved = chatSessionService.lastBatchSaved.stream()
                .filter(item -> item.getUserId().equals(2L))
                .findFirst()
                .orElseThrow();

        // Unread count should be the same, not double incremented
        Assertions.assertEquals(firstSaved.getUnreadCount(), secondSaved.getUnreadCount(),
            "Double batch update should be idempotent - unread should not double");
    }

    /**
     * RED Test: Verify isDuplicateOrStaleMessage logic is correct
     *
     * Test the edge case: lastMessageId = null should allow any message update
     */
    @Test
    void shouldAllowUpdateWhenLastMessageIdIsNull() {
        ChatSession session = new ChatSession();
        session.setRoomId(1L);
        session.setUserId(2L);
        session.setLastMessageId(null); // No previous message
        session.setUnreadCount(0);
        session.setTopStatus(0);
        session.setMuteStatus(0);
        chatSessionService.getOneResult = session;
        chatSessionService.saveOrUpdateCount = 0;

        // Apply a new message
        chatSessionService.updateSession(2L, 1L, 100L, true);

        // Atomic update succeeds when lastMessageId is null — no fallback saveOrUpdate needed
        Assertions.assertEquals(0, chatSessionService.saveOrUpdateCount,
            "Atomic update should handle null lastMessageId without fallback saveOrUpdate");
        Assertions.assertEquals(100L, session.getLastMessageId(),
            "lastMessageId should be updated to new message");
        Assertions.assertEquals(1, session.getUnreadCount(),
            "Unread should be incremented for new message");
    }

    /**
     * RED Test: Batch update should not break when messageId is null
     */
    @Test
    void batchUpdateShouldHandleNullMessageIdGracefully() {
        ChatSession receiverSession = createSession(1L, null, 0, 0);
        receiverSession.setUserId(2L);
        chatSessionService.listResult = List.of(receiverSession);

        // Batch update with null messageId - should not crash
        Assertions.assertDoesNotThrow(() ->
            chatSessionService.updateSessionBatch(List.of(2L), 1L, null, 1L));
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
        return createMessage(id, roomId, content, ChatMessageTypeEnum.TEXT);
    }

    private ChatMessage createMessage(Long id, Long roomId, String content, ChatMessageTypeEnum type) {
        ChatMessage message = new ChatMessage();
        message.setId(id);
        message.setRoomId(roomId);
        message.setFromUserId(2L);
        message.setContent(content);
        message.setStatus(MessageStatusEnum.NORMAL.getCode());
        message.setType(type.getCode());
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

    private com.stephen.cloud.chat.service.ChatRoomMemberService createChatRoomMemberService() {
        return (com.stephen.cloud.chat.service.ChatRoomMemberService) Proxy.newProxyInstance(
                com.stephen.cloud.chat.service.ChatRoomMemberService.class.getClassLoader(),
                new Class[]{com.stephen.cloud.chat.service.ChatRoomMemberService.class},
                (proxy, method, args) -> {
                    if ("isMember".equals(method.getName())) {
                        return member;
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
        private int saveOrUpdateCount;
        private int saveBatchCalls = 0;
        private int saveCalls = 0;
        private ChatSessionMapper mockMapper;

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

        @Override
        public boolean saveOrUpdate(ChatSession entity) {
            this.getOneResult = entity;
            this.saveOrUpdateCount++;
            return true;
        }

        @Override
        public boolean saveBatch(java.util.Collection<ChatSession> entityList) {
            this.lastBatchSaved = new ArrayList<>(entityList);
            this.saveBatchCalls++;
            return true;
        }

        @Override
        public boolean save(ChatSession entity) {
            this.getOneResult = entity;
            this.saveCalls++;
            return true;
        }

        @Override
        public ChatSessionMapper getBaseMapper() {
            if (mockMapper == null) {
                mockMapper = (ChatSessionMapper) Proxy.newProxyInstance(
                        ChatSessionMapper.class.getClassLoader(),
                        new Class[]{ChatSessionMapper.class},
                        (proxy, method, args) -> {
                            if ("atomicUpdateSession".equals(method.getName())) {
                                Long lastMessageId = (Long) args[2];
                                boolean incrementUnread = (boolean) args[3];
                                if (getOneResult != null
                                        && (getOneResult.getLastMessageId() == null
                                            || getOneResult.getLastMessageId() < lastMessageId)) {
                                    getOneResult.setLastMessageId(lastMessageId);
                                    getOneResult.setActiveTime(new Date());
                                    if (incrementUnread) {
                                        Integer cur = getOneResult.getUnreadCount();
                                        getOneResult.setUnreadCount((cur == null ? 0 : cur) + 1);
                                    }
                                    return 1;
                                }
                                return 0;
                            }
                            if ("atomicUpdateSessionBatch".equals(method.getName())) {
                                Long lastMessageId = (Long) args[1];
                                Long senderId = (Long) args[2];
                                int affected = 0;
                                for (ChatSession s : listResult) {
                                    if (s.getLastMessageId() == null || s.getLastMessageId() < lastMessageId) {
                                        s.setLastMessageId(lastMessageId);
                                        s.setActiveTime(new Date());
                                        if (!s.getUserId().equals(senderId)) {
                                            Integer cur = s.getUnreadCount();
                                            s.setUnreadCount((cur == null ? 0 : cur) + 1);
                                        }
                                        affected++;
                                    }
                                }
                                lastBatchSaved = new ArrayList<>(listResult);
                                return affected;
                            }
                            // Return default value for other methods
                            Class<?> returnType = method.getReturnType();
                            if (returnType == boolean.class) return false;
                            if (returnType == int.class) return 0;
                            if (returnType == long.class) return 0L;
                            return null;
                        });
            }
            return mockMapper;
        }
    }

    private static class FakeChatMqProducer extends ChatMqProducer {
        private Long lastSessionUpdateUserId;
        private Long lastSessionUpdateRoomId;
        private Object lastSessionUpdatePayload;
        private Long lastSessionDeleteUserId;
        private Long lastSessionDeleteRoomId;
        private List<Long> sessionUpdateAttemptUsers = new ArrayList<>();
        private List<Long> sessionDeleteAttemptUsers = new ArrayList<>();
        private boolean sessionUpdateThrows;
        private boolean sessionDeleteThrows;

        @Override
        public void sendSessionUpdate(Long userId, Long roomId, Object data, String bizId) {
            this.sessionUpdateAttemptUsers.add(userId);
            if (sessionUpdateThrows) {
                throw new RuntimeException("session update failed");
            }
            this.lastSessionUpdateUserId = userId;
            this.lastSessionUpdateRoomId = roomId;
            this.lastSessionUpdatePayload = data;
        }

        @Override
        public void sendSessionDelete(Long userId, Long roomId, String bizId) {
            this.sessionDeleteAttemptUsers.add(userId);
            if (sessionDeleteThrows) {
                throw new RuntimeException("session delete failed");
            }
            this.lastSessionDeleteUserId = userId;
            this.lastSessionDeleteRoomId = roomId;
        }
    }
}
