package com.stephen.cloud.chat.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.chat.model.enums.ChatMessageTypeEnum;
import com.stephen.cloud.api.chat.model.enums.ChatRoomTypeEnum;
import com.stephen.cloud.api.chat.model.enums.MessageStatusEnum;
import com.stephen.cloud.api.chat.model.vo.ChatMessageVO;
import com.stephen.cloud.api.chat.model.vo.ChatMessageReadStatusVO;
import com.stephen.cloud.api.chat.model.vo.ChatSessionVO;
import com.stephen.cloud.api.user.client.UserFeignClient;
import com.stephen.cloud.api.user.model.vo.UserVO;
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
import com.stephen.cloud.chat.support.ChatBusinessMetricsRecorder;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ResultUtils;
import com.stephen.cloud.common.exception.BusinessException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ChatMessageServiceImplTest {

    private TestableChatMessageServiceImpl chatMessageService;
    private FakeChatMqProducer chatMqProducer;
    private ChatRoom room;
    private boolean member;
    private boolean mutualFriend;
    private boolean blockedBetween;
    private ChatPrivateRoom privateRoom;
    private ChatRoomMember roomMember;
    private List<ChatRoomMember> roomMembers;
    private ChatSessionVO sessionVO;
    private long unreadCountAfterBoundary;
    private List<Long> pushUserIds;
    private Map<Long, ChatMessage> replyMessagesById;
    private Map<Long, UserVO> usersById;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ChatMessage.class);
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
        roomMembers = List.of(roomMember);
        sessionVO = new ChatSessionVO();
        sessionVO.setRoomId(1L);
        sessionVO.setUnreadCount(0);
        unreadCountAfterBoundary = 0L;
        pushUserIds = new ArrayList<>();
        replyMessagesById = new HashMap<>();
        usersById = new HashMap<>();
        meterRegistry = new SimpleMeterRegistry();
        usersById.put(1L, buildUser(1L, "Alice"));
        usersById.put(2L, buildUser(2L, "Bob"));

        ReflectionTestUtils.setField(chatMessageService, "chatRoomMemberService", createChatRoomMemberService());
        ReflectionTestUtils.setField(chatMessageService, "chatMqProducer", chatMqProducer);
        ReflectionTestUtils.setField(chatMessageService, "chatRoomService", createChatRoomService());
        ReflectionTestUtils.setField(chatMessageService, "chatPrivateRoomService", createChatPrivateRoomService());
        ReflectionTestUtils.setField(chatMessageService, "chatSessionService", createChatSessionService());
        ReflectionTestUtils.setField(chatMessageService, "userFriendService", createUserFriendService());
        ReflectionTestUtils.setField(chatMessageService, "userFeignClient", createUserFeignClient());
        ReflectionTestUtils.setField(chatMessageService, "eventPublisher",
                (org.springframework.context.ApplicationEventPublisher) event -> {
                });
        ReflectionTestUtils.setField(chatMessageService, "businessMetricsRecorder",
                new ChatBusinessMetricsRecorder(meterRegistry));
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
    void shouldRejectPrivateMessageWhenEitherUserBlocked() {
        blockedBetween = true;
        ChatMessage message = createTextMessage(1L, "block-1", "hello");

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatMessageService.sendMessage(message, 1L));

        Assertions.assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
        Assertions.assertEquals("双方存在拉黑关系", exception.getMessage());
        Assertions.assertNull(chatMessageService.messageById);
        Assertions.assertNull(chatMqProducer.lastGroupPushRoomId);
    }

    @Test
    void shouldReturnHistoryMessagesInChronologicalOrder() {
        chatMessageService.listResult = List.of(createStoredMessage(5L, 1L), createStoredMessage(4L, 1L));

        List<ChatMessageVO> history = chatMessageService.listHistoryMessages(1L, null, 20, 1L);

        Assertions.assertEquals(List.of(4L, 5L), history.stream().map(ChatMessageVO::getId).toList());
    }

    @Test
    void shouldReturnReconnectCompensationMessagesAfterCursorInChronologicalOrder() {
        chatMessageService.listResult = List.of(createStoredMessage(6L, 1L), createStoredMessage(7L, 1L));

        List<ChatMessageVO> messages = chatMessageService.listMessagesAfter(1L, 5L, 100, 1L);

        Assertions.assertEquals(List.of(6L, 7L), messages.stream().map(ChatMessageVO::getId).toList());
    }

    @Test
    void shouldReturnEmptyReconnectCompensationMessagesWhenNoNewMessages() {
        chatMessageService.listResult = List.of();

        List<ChatMessageVO> messages = chatMessageService.listMessagesAfter(1L, 99L, 100, 1L);

        Assertions.assertTrue(messages.isEmpty());
    }

    @Test
    void shouldRejectReconnectCompensationWhenUserIsNotRoomMember() {
        member = false;

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatMessageService.listMessagesAfter(1L, 5L, 100, 1L));

        Assertions.assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
    }

    @Test
    void shouldCapReconnectCompensationLimit() {
        Assertions.assertEquals(200, ChatMessageServiceImpl.normalizeReconnectCompensationLimit(999));
    }

    @Test
    void shouldUseDefaultReconnectCompensationLimitWhenInvalid() {
        Assertions.assertEquals(100, ChatMessageServiceImpl.normalizeReconnectCompensationLimit(0));
        Assertions.assertEquals(100, ChatMessageServiceImpl.normalizeReconnectCompensationLimit(null));
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
    void shouldKeepUnreadCountAfterPartialRead() {
        ChatMessage stored = createStoredMessage(8L, 1L);
        chatMessageService.messageById = stored;
        roomMember.setLastReadMessageId(5L);
        unreadCountAfterBoundary = 2L;

        boolean result = chatMessageService.markMessageRead(1L, 8L, 1L);

        Assertions.assertTrue(result);
        Assertions.assertEquals(8L, roomMember.getLastReadMessageId());
        Assertions.assertEquals(2, chatMessageService.updatedUnreadCount);
        Assertions.assertEquals(8L, chatMessageService.updatedLastReadMessageId);
        Assertions.assertNotNull(chatMqProducer.lastReadPayload);
        Assertions.assertEquals(List.of(1L), chatMqProducer.lastReadUserIds);
        Assertions.assertEquals(1L, chatMqProducer.lastSessionUpdateUserId);
    }

    @Test
    void shouldKeepReadFactWhenReadPushThrows() {
        ChatMessage stored = createStoredMessage(8L, 1L);
        chatMessageService.messageById = stored;
        roomMember.setLastReadMessageId(5L);
        unreadCountAfterBoundary = 2L;
        chatMqProducer.readPushThrows = true;

        boolean result = Assertions.assertDoesNotThrow(() -> chatMessageService.markMessageRead(1L, 8L, 1L));

        Assertions.assertTrue(result);
        Assertions.assertEquals(8L, roomMember.getLastReadMessageId());
        Assertions.assertEquals(2, chatMessageService.updatedUnreadCount);
        Assertions.assertEquals(8L, chatMessageService.updatedLastReadMessageId);
        Assertions.assertEquals(List.of(1L), chatMqProducer.readAttemptRoomIds);
    }

    @Test
    void shouldKeepReadFactWhenSessionUpdatePushThrows() {
        ChatMessage stored = createStoredMessage(8L, 1L);
        chatMessageService.messageById = stored;
        roomMember.setLastReadMessageId(5L);
        unreadCountAfterBoundary = 2L;
        chatMqProducer.sessionUpdateThrows = true;

        boolean result = Assertions.assertDoesNotThrow(() -> chatMessageService.markMessageRead(1L, 8L, 1L));

        Assertions.assertTrue(result);
        Assertions.assertEquals(8L, roomMember.getLastReadMessageId());
        Assertions.assertEquals(2, chatMessageService.updatedUnreadCount);
        Assertions.assertEquals(8L, chatMessageService.updatedLastReadMessageId);
        Assertions.assertEquals(List.of(1L), chatMqProducer.sessionUpdateAttemptUsers);
    }

    @Test
    void shouldClearUnreadCountAfterReadingNewestMessage() {
        ChatMessage stored = createStoredMessage(10L, 1L);
        chatMessageService.messageById = stored;
        roomMember.setLastReadMessageId(8L);
        unreadCountAfterBoundary = 0L;

        boolean result = chatMessageService.markMessageRead(1L, 10L, 1L);

        Assertions.assertTrue(result);
        Assertions.assertEquals(0, chatMessageService.updatedUnreadCount);
        Assertions.assertEquals(10L, chatMessageService.updatedLastReadMessageId);
    }

    @Test
    void shouldIgnoreStaleReadBoundary() {
        ChatMessage stored = createStoredMessage(7L, 1L);
        chatMessageService.messageById = stored;
        roomMember.setLastReadMessageId(8L);

        boolean result = chatMessageService.markMessageRead(1L, 7L, 1L);

        Assertions.assertTrue(result);
        Assertions.assertFalse(chatMessageService.sessionUpdateInvoked);
        Assertions.assertNull(chatMqProducer.lastReadPayload);
        Assertions.assertNull(chatMqProducer.lastSessionUpdateUserId);
    }

    @Test
    void shouldReturnMessageReadStatusSummaryForMessageSender() {
        ChatRoomMember senderMember = buildRoomMember(1L, 1L, null);
        ChatRoomMember readMember = buildRoomMember(1L, 2L, 10L);
        ChatRoomMember laterReadMember = buildRoomMember(1L, 4L, 12L);
        ChatRoomMember unreadMember = buildRoomMember(1L, 3L, 7L);
        roomMembers = List.of(senderMember, readMember, laterReadMember, unreadMember);
        ChatMessage stored = createStoredMessage(10L, 1L);
        stored.setFromUserId(1L);
        chatMessageService.messageByRoomQuery = stored;

        ChatMessageReadStatusVO result = chatMessageService.getMessageReadStatus(1L, 10L, 1L);

        Assertions.assertEquals(1L, result.getRoomId());
        Assertions.assertEquals(10L, result.getMessageId());
        Assertions.assertEquals(4, result.getTotalCount());
        Assertions.assertEquals(3, result.getReadCount());
        Assertions.assertEquals(1, result.getUnreadCount());
    }

    @Test
    void shouldNotExposeMemberListsInMessageReadStatusSummary() {
        List<String> fieldNames = java.util.Arrays.stream(ChatMessageReadStatusVO.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName)
                .toList();

        Assertions.assertFalse(fieldNames.contains("readUserIds"));
        Assertions.assertFalse(fieldNames.contains("unreadUserIds"));
        Assertions.assertFalse(fieldNames.contains("readMembers"));
        Assertions.assertFalse(fieldNames.contains("unreadMembers"));
    }

    @Test
    void shouldRejectMessageReadStatusQueryByNonSender() {
        ChatMessage stored = createStoredMessage(10L, 1L);
        stored.setFromUserId(2L);
        chatMessageService.messageByRoomQuery = stored;

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatMessageService.getMessageReadStatus(1L, 10L, 1L));

        Assertions.assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
    }

    @Test
    void shouldRejectMessageReadStatusQueryByNonMember() {
        member = false;
        ChatMessage stored = createStoredMessage(10L, 1L);
        stored.setFromUserId(1L);
        chatMessageService.messageByRoomQuery = stored;

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatMessageService.getMessageReadStatus(1L, 10L, 1L));

        Assertions.assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
    }

    @Test
    void shouldRejectMessageReadStatusWhenMessageDoesNotExist() {
        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatMessageService.getMessageReadStatus(1L, 404L, 1L));

        Assertions.assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), exception.getCode());
    }

    @Test
    void shouldRejectMessageReadStatusWhenMessageBelongsToAnotherRoom() {
        ChatMessage stored = createStoredMessage(10L, 2L);
        stored.setFromUserId(1L);
        chatMessageService.messageById = stored;

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatMessageService.getMessageReadStatus(1L, 10L, 1L));

        Assertions.assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), exception.getCode());
    }

    @Test
    void shouldNotExposeCrossRoomMessageExistenceForNonSender() {
        ChatMessage stored = createStoredMessage(10L, 2L);
        stored.setFromUserId(2L);
        chatMessageService.messageById = stored;

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatMessageService.getMessageReadStatus(1L, 10L, 1L));

        Assertions.assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), exception.getCode());
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

    @Test
    void shouldSendRoomMemberSnapshotWhenGroupMessageIsCreated() {
        room.setType(ChatRoomTypeEnum.GROUP.getCode());
        ChatRoomMember peerMember = new ChatRoomMember();
        peerMember.setRoomId(1L);
        peerMember.setUserId(2L);
        roomMembers = List.of(roomMember, peerMember);
        pushUserIds = List.of(1L, 2L);

        ChatMessageVO result = chatMessageService.sendMessage(createTextMessage(1L, "c1", "hello"), 1L);

        Assertions.assertEquals(100L, result.getId());
        Assertions.assertEquals(1L, chatMqProducer.lastGroupPushRoomId);
        Assertions.assertEquals(List.of(1L, 2L), chatMqProducer.lastGroupPushUserIds);
        Assertions.assertEquals(1.0, businessCounter("message_send", "success"));
    }

    @Test
    void shouldFilterMutedUsersWhenGroupMessageIsCreated() {
        room.setType(ChatRoomTypeEnum.GROUP.getCode());
        ChatRoomMember peerMember = new ChatRoomMember();
        peerMember.setRoomId(1L);
        peerMember.setUserId(2L);
        roomMembers = List.of(roomMember, peerMember);
        pushUserIds = List.of(1L);

        ChatMessageVO result = chatMessageService.sendMessage(createTextMessage(1L, "mute-1", "hello"), 1L);

        Assertions.assertEquals(100L, result.getId());
        Assertions.assertEquals(List.of(1L), chatMqProducer.lastGroupPushUserIds);
    }

    @Test
    void shouldSearchTextMessagesForRoomMember() {
        ChatMessage first = createStoredMessage(10L, 1L);
        first.setContent("hello keyword");
        ChatMessage second = createStoredMessage(9L, 1L);
        second.setContent("another keyword");
        chatMessageService.searchPageResult = new Page<>(1, 20, 2);
        chatMessageService.searchPageResult.setRecords(List.of(first, second));

        Page<ChatMessageVO> result = chatMessageService.searchMessages(1L, "keyword", 1, 20, 1L);

        Assertions.assertEquals(2, result.getTotal());
        Assertions.assertEquals(List.of(10L, 9L), result.getRecords().stream().map(ChatMessageVO::getId).toList());
        Assertions.assertTrue(chatMessageService.searchSqlSegment.contains("room_id"));
        Assertions.assertTrue(chatMessageService.searchSqlSegment.contains("type"));
        Assertions.assertTrue(chatMessageService.searchSqlSegment.contains("status"));
        Assertions.assertTrue(chatMessageService.searchSqlSegment.contains("content"));
    }

    @Test
    void shouldRejectMessageSearchForNonMember() {
        member = false;

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatMessageService.searchMessages(1L, "keyword", 1, 20, 1L));

        Assertions.assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
    }

    @Test
    void shouldRejectMessageSearchWithBlankKeyword() {
        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatMessageService.searchMessages(1L, " ", 1, 20, 1L));

        Assertions.assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
    }

    @Test
    void shouldRecordDuplicateMetricWhenReturningExistingClientMessage() {
        ChatMessage existing = createStoredMessage(88L, 1L);
        existing.setClientMsgId("dup-1");
        chatMessageService.existingByClient = existing;

        ChatMessageVO result = chatMessageService.sendMessage(createTextMessage(1L, "dup-1", "hello"), 1L);

        Assertions.assertEquals(88L, result.getId());
        Assertions.assertEquals(1.0, businessCounter("message_send", "duplicate"));
    }

    @Test
    void shouldReturnExistingMessageWhenDuplicateClientMsgIdWinsDatabaseRace() {
        room.setType(ChatRoomTypeEnum.GROUP.getCode());
        chatMessageService.saveDuplicateKey = true;
        ChatMessage existing = createStoredMessage(88L, 1L);
        existing.setClientMsgId("dup-1");
        chatMessageService.duplicateExistingMessage = existing;

        ChatMessageVO result = chatMessageService.sendMessage(createTextMessage(1L, "dup-1", "hello"), 1L);

        Assertions.assertEquals(88L, result.getId());
        Assertions.assertNull(chatMqProducer.lastGroupPushRoomId);
        Assertions.assertEquals(1.0, businessCounter("message_send", "duplicate"));
    }

    @Test
    void shouldKeepMessageFactWhenGroupPushThrows() {
        room.setType(ChatRoomTypeEnum.GROUP.getCode());
        chatMqProducer.groupPushThrows = true;

        ChatMessageVO result = Assertions.assertDoesNotThrow(
                () -> chatMessageService.sendMessage(createTextMessage(1L, "c1", "hello"), 1L));

        Assertions.assertEquals(100L, result.getId());
        Assertions.assertEquals(100L, chatMessageService.messageById.getId());
        Assertions.assertEquals(List.of(1L), chatMqProducer.groupPushAttemptRoomIds);
    }

    @Test
    void shouldRejectInvalidImageMessageWithoutPersistenceOrPush() {
        ChatMessage message = createMediaMessage(ChatMessageTypeEnum.IMAGE,
                "{\"url\":\"https://example.com/a.png\",\"width\":100,\"height\":200,\"size\":0}");

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatMessageService.sendMessage(message, 1L));

        Assertions.assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        Assertions.assertNull(chatMessageService.messageById);
        Assertions.assertNull(chatMqProducer.lastGroupPushRoomId);
    }

    @Test
    void shouldRejectInvalidFileMessageWithoutPersistenceOrPush() {
        ChatMessage message = createMediaMessage(ChatMessageTypeEnum.FILE,
                "{\"url\":\"https://example.com/a.zip\",\"name\":\" \",\"size\":1024,\"ext\":\"zip\"}");

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatMessageService.sendMessage(message, 1L));

        Assertions.assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        Assertions.assertNull(chatMessageService.messageById);
        Assertions.assertNull(chatMqProducer.lastGroupPushRoomId);
    }

    @Test
    void shouldSendRoomMemberSnapshotWhenMessageReadIsCreated() {
        ChatMessage stored = createStoredMessage(8L, 1L);
        chatMessageService.messageById = stored;
        ChatRoomMember peerMember = new ChatRoomMember();
        peerMember.setRoomId(1L);
        peerMember.setUserId(2L);
        roomMembers = List.of(roomMember, peerMember);

        boolean result = chatMessageService.markMessageRead(1L, 8L, 1L);

        Assertions.assertTrue(result);
        Assertions.assertEquals(List.of(1L, 2L), chatMqProducer.lastReadUserIds);
    }

    @Test
    void shouldSendRoomMemberSnapshotWhenMessageIsRecalled() {
        ChatMessage stored = createStoredMessage(9L, 1L);
        chatMessageService.messageById = stored;
        ChatRoomMember peerMember = new ChatRoomMember();
        peerMember.setRoomId(1L);
        peerMember.setUserId(2L);
        roomMembers = List.of(roomMember, peerMember);

        boolean result = chatMessageService.recallMessage(9L, 1L);

        Assertions.assertTrue(result);
        Assertions.assertEquals(List.of(1L, 2L), chatMqProducer.lastRecallUserIds);
    }

    @Test
    void shouldKeepRecallFactWhenRecallPushThrows() {
        ChatMessage stored = createStoredMessage(9L, 1L);
        chatMessageService.messageById = stored;
        chatMqProducer.recallPushThrows = true;

        boolean result = Assertions.assertDoesNotThrow(() -> chatMessageService.recallMessage(9L, 1L));

        Assertions.assertTrue(result);
        Assertions.assertEquals(MessageStatusEnum.RECALL.getCode(), chatMessageService.messageById.getStatus());
        Assertions.assertEquals(List.of(1L), chatMqProducer.recallAttemptRoomIds);
    }

    @Test
    void shouldKeepRecallFactAndContinueSessionUpdatesWhenOneSessionUpdateThrows() {
        ChatMessage stored = createStoredMessage(9L, 1L);
        chatMessageService.messageById = stored;
        ChatRoomMember peerMember = new ChatRoomMember();
        peerMember.setRoomId(1L);
        peerMember.setUserId(2L);
        roomMembers = List.of(roomMember, peerMember);
        chatMqProducer.sessionUpdateFailureUserId = 1L;

        boolean result = Assertions.assertDoesNotThrow(() -> chatMessageService.recallMessage(9L, 1L));

        Assertions.assertTrue(result);
        Assertions.assertEquals(MessageStatusEnum.RECALL.getCode(), chatMessageService.messageById.getStatus());
        Assertions.assertEquals(List.of(1L, 2L), chatMqProducer.sessionUpdateAttemptUsers);
        Assertions.assertEquals(2L, chatMqProducer.lastSessionUpdateUserId);
    }

    @Test
    void shouldSendReplyMessageInSameRoomAndReturnReplyPreview() {
        ChatMessage replyMessage = createStoredMessage(10L, 1L);
        replyMessage.setFromUserId(2L);
        replyMessage.setContent("original message");
        chatMessageService.messageById = replyMessage;
        replyMessagesById.put(10L, replyMessage);
        ChatMessage message = createTextMessage(1L, "reply-1", "reply text");
        message.setReplyMsgId(10L);

        ChatMessageVO result = chatMessageService.sendMessage(message, 1L);

        Assertions.assertEquals(10L, chatMessageService.messageById.getReplyMsgId());
        Assertions.assertNotNull(result.getReplyMsg());
        Assertions.assertEquals(10L, result.getReplyMsg().getId());
        Assertions.assertEquals("original message", result.getReplyMsg().getContent());
        Assertions.assertEquals("Bob", result.getReplyMsg().getUserName());
    }

    @Test
    void shouldRejectReplyMessageFromAnotherRoom() {
        ChatMessage replyMessage = createStoredMessage(10L, 2L);
        chatMessageService.messageById = replyMessage;
        ChatMessage message = createTextMessage(1L, "reply-2", "reply text");
        message.setReplyMsgId(10L);

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatMessageService.sendMessage(message, 1L));

        Assertions.assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
    }

    @Test
    void shouldRejectReplyMessageWhenReferencedMessageDoesNotExist() {
        ChatMessage message = createTextMessage(1L, "reply-3", "reply text");
        message.setReplyMsgId(404L);

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatMessageService.sendMessage(message, 1L));

        Assertions.assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        Assertions.assertNull(chatMessageService.messageById);
        Assertions.assertNull(chatMqProducer.lastGroupPushRoomId);
    }

    @Test
    void shouldRejectReplyMessageWhenSenderHasNoSendPermission() {
        mutualFriend = false;
        ChatMessage message = createTextMessage(1L, "reply-4", "reply text");
        message.setReplyMsgId(10L);

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatMessageService.sendMessage(message, 1L));

        Assertions.assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
        Assertions.assertNull(chatMessageService.messageById);
        Assertions.assertNull(chatMqProducer.lastGroupPushRoomId);
    }

    @Test
    void shouldMaskRecalledReplyPreview() {
        ChatMessage current = createStoredMessage(20L, 1L);
        current.setReplyMsgId(10L);
        ChatMessage replyMessage = createStoredMessage(10L, 1L);
        replyMessage.setContent("secret original");
        replyMessage.setStatus(MessageStatusEnum.RECALL.getCode());
        replyMessagesById.put(10L, replyMessage);

        ChatMessageVO result = chatMessageService.getChatMessageVO(current, null);

        Assertions.assertNotNull(result.getReplyMsg());
        Assertions.assertEquals("该消息已被撤回", result.getReplyMsg().getContent());
    }

    @Test
    void shouldHideReplyPreviewWhenStoredReplyMessageBelongsToAnotherRoom() {
        ChatMessage current = createStoredMessage(20L, 1L);
        current.setReplyMsgId(10L);
        ChatMessage replyMessage = createStoredMessage(10L, 2L);
        replyMessage.setContent("cross room secret");
        replyMessagesById.put(10L, replyMessage);

        ChatMessageVO result = chatMessageService.getChatMessageVO(current, null);

        Assertions.assertNull(result.getReplyMsg());
    }

    private ChatMessage createTextMessage(Long roomId, String clientMsgId, String content) {
        ChatMessage message = new ChatMessage();
        message.setRoomId(roomId);
        message.setClientMsgId(clientMsgId);
        message.setType(ChatMessageTypeEnum.TEXT.getCode());
        message.setContent(content);
        return message;
    }

    private ChatMessage createMediaMessage(ChatMessageTypeEnum type, String extra) {
        ChatMessage message = new ChatMessage();
        message.setRoomId(1L);
        message.setClientMsgId(type.name().toLowerCase() + "-1");
        message.setType(type.getCode());
        message.setExtra(extra);
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

    private UserVO buildUser(Long id, String name) {
        UserVO user = new UserVO();
        user.setId(id);
        user.setUserName(name);
        return user;
    }

    private double businessCounter(String action, String result) {
        return meterRegistry.get("mallchat.im.business.total")
                .tag("action", action)
                .tag("result", result)
                .counter()
                .count();
    }

    private ChatRoomMember buildRoomMember(Long roomId, Long userId, Long lastReadMessageId) {
        ChatRoomMember member = new ChatRoomMember();
        member.setRoomId(roomId);
        member.setUserId(userId);
        member.setLastReadMessageId(lastReadMessageId);
        return member;
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
                        case "listByRoomId" -> roomMembers;
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
                        case "filterPushUserIds" -> pushUserIds;
                        case "update" -> {
                            chatMessageService.sessionUpdateInvoked = true;
                            UpdateWrapper<?> wrapper = (UpdateWrapper<?>) args[0];
                            Map<String, Object> values = wrapper.getParamNameValuePairs();
                            values.values().forEach(value -> {
                                if (value instanceof Integer integer) {
                                    chatMessageService.updatedUnreadCount = integer;
                                } else if (value instanceof Long longValue && !longValue.equals(1L)) {
                                    chatMessageService.updatedLastReadMessageId = longValue;
                                }
                            });
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
                    if ("isBlockedBetween".equals(method.getName())) {
                        return blockedBetween;
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
                        @SuppressWarnings("unchecked")
                        List<Long> ids = (List<Long>) args[0];
                        return ResultUtils.success(ids.stream()
                                .map(usersById::get)
                                .filter(java.util.Objects::nonNull)
                                .toList());
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private class TestableChatMessageServiceImpl extends ChatMessageServiceImpl {
        private ChatMessage existingByClient;
        private ChatMessage messageById;
        private ChatMessage messageByRoomQuery;
        private List<ChatMessage> listResult = new ArrayList<>();
        private boolean saveResult = true;
        private boolean saveDuplicateKey;
        private ChatMessage duplicateExistingMessage;
        private boolean updateResult = true;
        private boolean sessionUpdateInvoked;
        private int updatedUnreadCount = -1;
        private Long updatedLastReadMessageId;
        private Page<ChatMessage> searchPageResult = new Page<>(1, 20, 0);
        private String searchSqlSegment;

        @Override
        public ChatMessage getOne(Wrapper<ChatMessage> queryWrapper) {
            if (existingByClient != null) {
                return existingByClient;
            }
            return messageByRoomQuery;
        }

        @Override
        public ChatMessage getById(java.io.Serializable id) {
            return messageById;
        }

        @Override
        public boolean save(ChatMessage entity) {
            if (saveDuplicateKey) {
                existingByClient = duplicateExistingMessage;
                throw new org.springframework.dao.DuplicateKeyException("duplicate client msg id");
            }
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
        public <E extends IPage<ChatMessage>> E page(E page, Wrapper<ChatMessage> queryWrapper) {
            searchSqlSegment = queryWrapper.getSqlSegment();
            @SuppressWarnings("unchecked")
            E result = (E) searchPageResult;
            return result;
        }

        @Override
        public List<ChatMessage> listByIds(Collection<? extends Serializable> idList) {
            return idList.stream()
                    .map(id -> replyMessagesById.get((Long) id))
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }

        @Override
        public long count(Wrapper<ChatMessage> queryWrapper) {
            return unreadCountAfterBoundary;
        }

    }

    private static class FakeChatMqProducer extends ChatMqProducer {
        private Object lastReadPayload;
        private Long lastSessionUpdateUserId;
        private Long lastGroupPushRoomId;
        private List<Long> lastGroupPushUserIds;
        private List<Long> lastReadUserIds;
        private List<Long> lastRecallUserIds;
        private List<Long> groupPushAttemptRoomIds = new ArrayList<>();
        private List<Long> readAttemptRoomIds = new ArrayList<>();
        private List<Long> recallAttemptRoomIds = new ArrayList<>();
        private List<Long> sessionUpdateAttemptUsers = new ArrayList<>();
        private Long sessionUpdateFailureUserId;
        private boolean groupPushThrows;
        private boolean readPushThrows;
        private boolean recallPushThrows;
        private boolean sessionUpdateThrows;

        @Override
        public void sendChatMessageGroupPush(Long roomId, ChatMessageVO chatMessageVO, List<Long> userIds) {
            this.groupPushAttemptRoomIds.add(roomId);
            if (groupPushThrows) {
                throw new RuntimeException("group push failed");
            }
            this.lastGroupPushRoomId = roomId;
            this.lastGroupPushUserIds = userIds;
        }

        @Override
        public void sendMessageRead(Long roomId, Object data, String bizId) {
            this.lastReadPayload = data;
        }

        @Override
        public void sendMessageRead(Long roomId, Object data, String bizId, List<Long> userIds) {
            this.readAttemptRoomIds.add(roomId);
            if (readPushThrows) {
                throw new RuntimeException("read push failed");
            }
            this.lastReadPayload = data;
            this.lastReadUserIds = userIds;
        }

        @Override
        public void sendMessageRecall(Long roomId, ChatMessageVO chatMessageVO, List<Long> userIds) {
            this.recallAttemptRoomIds.add(roomId);
            if (recallPushThrows) {
                throw new RuntimeException("recall push failed");
            }
            this.lastRecallUserIds = userIds;
        }

        @Override
        public void sendSessionUpdate(Long userId, Long roomId, Object data, String bizId) {
            this.sessionUpdateAttemptUsers.add(userId);
            if (sessionUpdateThrows || (sessionUpdateFailureUserId != null && sessionUpdateFailureUserId.equals(userId))) {
                throw new RuntimeException("session update failed");
            }
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
