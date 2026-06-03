package com.stephen.cloud.chat.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.stephen.cloud.api.chat.model.enums.ChatMessageTypeEnum;
import com.stephen.cloud.api.chat.model.enums.ChatRoomRoleEnum;
import com.stephen.cloud.api.chat.model.enums.ChatRoomTypeEnum;
import com.stephen.cloud.api.chat.model.enums.MessageStatusEnum;
import com.stephen.cloud.api.chat.model.vo.ChatMessageVO;
import com.stephen.cloud.api.chat.model.vo.ChatSessionVO;
import com.stephen.cloud.api.user.client.UserFeignClient;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.chat.model.entity.ChatAuditEvent;
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
import com.stephen.cloud.chat.support.ChatAuditEventRecorder;
import com.stephen.cloud.chat.support.ChatBusinessMetricsRecorder;
import com.stephen.cloud.common.cache.model.TimeModel;
import com.stephen.cloud.common.cache.utils.ratelimit.RateLimitUtils;
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

/**
 * STE-103: 安全权限增强测试
 * 覆盖频控、拉黑收敛、撤回权限、管理员权限和审计事件
 */
class ChatSecurityEnhancementTest {

    private TestableChatMessageServiceImpl chatMessageService;
    private FakeChatMqProducer chatMqProducer;
    private FakeRateLimitUtils rateLimitUtils;
    private FakeChatAuditEventRecorder auditEventRecorder;
    private ChatRoom room;
    private boolean member;
    private boolean mutualFriend;
    private boolean blockedBetween;
    private ChatPrivateRoom privateRoom;
    private ChatRoomMember roomMember;
    private List<ChatRoomMember> roomMembers;
    private Map<Long, ChatRoom> roomsById;
    private Map<Long, UserVO> usersById;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ChatMessage.class);
        chatMessageService = new TestableChatMessageServiceImpl();
        chatMqProducer = new FakeChatMqProducer();
        rateLimitUtils = new FakeRateLimitUtils();
        auditEventRecorder = new FakeChatAuditEventRecorder();
        room = new ChatRoom();
        room.setId(1L);
        room.setType(ChatRoomTypeEnum.GROUP.getCode());
        roomsById = new HashMap<>();
        roomsById.put(1L, room);
        member = true;
        mutualFriend = true;
        blockedBetween = false;
        privateRoom = new ChatPrivateRoom();
        privateRoom.setRoomId(1L);
        privateRoom.setUserLow(1L);
        privateRoom.setUserHigh(2L);
        roomMember = new ChatRoomMember();
        roomMember.setRoomId(1L);
        roomMember.setUserId(1L);
        roomMember.setRole(ChatRoomRoleEnum.MEMBER.getCode());
        roomMembers = new ArrayList<>(List.of(roomMember));
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
        ReflectionTestUtils.setField(chatMessageService, "rateLimitUtils", rateLimitUtils);
        ReflectionTestUtils.setField(chatMessageService, "auditEventRecorder", auditEventRecorder);
    }

    // ========== FR-1: 频控测试 ==========

    @Test
    void shouldRejectSendMessageWhenRateLimited() {
        rateLimitUtils.shouldThrow = true;
        ChatMessage message = createTextMessage(1L, "rate-limit-1", "hello");

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatMessageService.sendMessage(message, 1L));

        Assertions.assertEquals(ErrorCode.OPERATION_ERROR.getCode(), exception.getCode());
        // 频控触发时消息不应落库
        Assertions.assertNull(chatMessageService.savedMessage);
    }

    // ========== FR-2: 拉黑收敛测试 ==========

    @Test
    void shouldRejectRecallWhenBlockedByTargetUser() {
        // 设置消息属于用户2，用户1尝试撤回
        ChatMessage stored = createStoredMessage(10L, 2L);
        stored.setRoomId(1L);
        stored.setStatus(MessageStatusEnum.NORMAL.getCode());
        stored.setCreateTime(new Date(System.currentTimeMillis() - 30000));
        chatMessageService.messageById = stored;
        // 用户1和用户2之间存在拉黑关系
        blockedBetween = true;

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatMessageService.recallMessage(10L, 1L));

        Assertions.assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
        Assertions.assertEquals("双方存在拉黑关系，无法撤回", exception.getMessage());
        // 消息状态不应改变
        Assertions.assertEquals(MessageStatusEnum.NORMAL.getCode(), stored.getStatus());
    }

    // ========== FR-3: 撤回权限增强测试 ==========

    @Test
    void shouldAllowOwnerToRecallAnyMessageInRoom() {
        // 用户1是群主
        roomMember.setRole(ChatRoomRoleEnum.OWNER.getCode());
        // 消息属于用户2，发送于 10 分钟前（超过 2 分钟限制）
        ChatMessage stored = createStoredMessage(10L, 2L);
        stored.setRoomId(1L);
        stored.setStatus(MessageStatusEnum.NORMAL.getCode());
        stored.setCreateTime(new Date(System.currentTimeMillis() - 10 * 60 * 1000));
        chatMessageService.messageById = stored;
        // 用户2是普通成员
        ChatRoomMember member2 = new ChatRoomMember();
        member2.setRoomId(1L);
        member2.setUserId(2L);
        member2.setRole(ChatRoomRoleEnum.MEMBER.getCode());
        roomMembers.add(member2);

        boolean result = Assertions.assertDoesNotThrow(
                () -> chatMessageService.recallMessage(10L, 1L));

        Assertions.assertTrue(result);
        Assertions.assertEquals(MessageStatusEnum.RECALL.getCode(), stored.getStatus());
        // 应记录审计事件
        Assertions.assertNotNull(auditEventRecorder.lastEvent);
        Assertions.assertEquals("MESSAGE_RECALL", auditEventRecorder.lastEvent.getAction());
    }

    @Test
    void shouldAllowAdminToRecallRegularMemberMessageInRoom() {
        // 用户1是管理员
        roomMember.setRole(ChatRoomRoleEnum.ADMIN.getCode());
        // 消息属于用户2（普通成员），发送于 10 分钟前
        ChatMessage stored = createStoredMessage(10L, 2L);
        stored.setRoomId(1L);
        stored.setStatus(MessageStatusEnum.NORMAL.getCode());
        stored.setCreateTime(new Date(System.currentTimeMillis() - 10 * 60 * 1000));
        chatMessageService.messageById = stored;
        // 用户2是普通成员
        ChatRoomMember member2 = new ChatRoomMember();
        member2.setRoomId(1L);
        member2.setUserId(2L);
        member2.setRole(ChatRoomRoleEnum.MEMBER.getCode());
        roomMembers.add(member2);

        boolean result = Assertions.assertDoesNotThrow(
                () -> chatMessageService.recallMessage(10L, 1L));

        Assertions.assertTrue(result);
        Assertions.assertEquals(MessageStatusEnum.RECALL.getCode(), stored.getStatus());
    }

    @Test
    void shouldRejectAdminRecallOfOtherAdminMessage() {
        // 用户1是管理员
        roomMember.setRole(ChatRoomRoleEnum.ADMIN.getCode());
        // 消息属于用户2（也是管理员）
        ChatMessage stored = createStoredMessage(10L, 2L);
        stored.setRoomId(1L);
        stored.setStatus(MessageStatusEnum.NORMAL.getCode());
        stored.setCreateTime(new Date(System.currentTimeMillis() - 10 * 60 * 1000));
        chatMessageService.messageById = stored;
        ChatRoomMember member2 = new ChatRoomMember();
        member2.setRoomId(1L);
        member2.setUserId(2L);
        member2.setRole(ChatRoomRoleEnum.ADMIN.getCode());
        roomMembers.add(member2);

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatMessageService.recallMessage(10L, 1L));

        Assertions.assertEquals(ErrorCode.FORBIDDEN_ERROR.getCode(), exception.getCode());
        Assertions.assertEquals("管理员不可撤回其他管理员的消息", exception.getMessage());
    }

    // ========== FR-5: 审计事件测试 ==========

    @Test
    void shouldRecordAuditEventOnMessageRecall() {
        // 用户1是群主
        roomMember.setRole(ChatRoomRoleEnum.OWNER.getCode());
        ChatMessage stored = createStoredMessage(10L, 2L);
        stored.setRoomId(1L);
        stored.setStatus(MessageStatusEnum.NORMAL.getCode());
        stored.setCreateTime(new Date(System.currentTimeMillis() - 30000));
        chatMessageService.messageById = stored;
        ChatRoomMember member2 = new ChatRoomMember();
        member2.setRoomId(1L);
        member2.setUserId(2L);
        member2.setRole(ChatRoomRoleEnum.MEMBER.getCode());
        roomMembers.add(member2);

        chatMessageService.recallMessage(10L, 1L);

        Assertions.assertNotNull(auditEventRecorder.lastEvent);
        Assertions.assertEquals("MESSAGE_RECALL", auditEventRecorder.lastEvent.getAction());
        Assertions.assertEquals(1L, auditEventRecorder.lastEvent.getUserId());
        Assertions.assertEquals(10L, auditEventRecorder.lastEvent.getTargetId());
        Assertions.assertEquals(1L, auditEventRecorder.lastEvent.getRoomId());
    }

    // ========== Helper methods ==========

    private ChatMessage createTextMessage(Long roomId, String clientMsgId, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setRoomId(roomId);
        msg.setClientMsgId(clientMsgId);
        msg.setContent(content);
        msg.setType(ChatMessageTypeEnum.TEXT.getCode());
        return msg;
    }

    private ChatMessage createStoredMessage(Long id, Long fromUserId) {
        ChatMessage msg = new ChatMessage();
        msg.setId(id);
        msg.setFromUserId(fromUserId);
        msg.setRoomId(1L);
        msg.setContent("test content");
        msg.setType(ChatMessageTypeEnum.TEXT.getCode());
        msg.setStatus(MessageStatusEnum.NORMAL.getCode());
        msg.setCreateTime(new Date());
        return msg;
    }

    private UserVO buildUser(Long id, String name) {
        UserVO vo = new UserVO();
        vo.setId(id);
        vo.setUserName(name);
        return vo;
    }

    // ========== Fake implementations ==========

    private ChatRoomMemberService createChatRoomMemberService() {
        return (ChatRoomMemberService) Proxy.newProxyInstance(
                ChatRoomMemberService.class.getClassLoader(),
                new Class[]{ChatRoomMemberService.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isMember" -> member;
                    case "isOwner" -> roomMember.getRole() != null
                            && roomMember.getRole().equals(ChatRoomRoleEnum.OWNER.getCode());
                    case "getMember" -> {
                        Long roomId = (Long) args[0];
                        Long userId = (Long) args[1];
                        yield roomMembers.stream()
                                .filter(m -> m.getRoomId().equals(roomId) && m.getUserId().equals(userId))
                                .findFirst()
                                .orElse(null);
                    }
                    case "listByRoomId" -> new ArrayList<>(roomMembers);
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private ChatRoomService createChatRoomService() {
        return (ChatRoomService) Proxy.newProxyInstance(
                ChatRoomService.class.getClassLoader(),
                new Class[]{ChatRoomService.class},
                (proxy, method, args) -> {
                    if ("getById".equals(method.getName())) {
                        return roomsById.get(args[0]);
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

    private ChatSessionService createChatSessionService() {
        return (ChatSessionService) Proxy.newProxyInstance(
                ChatSessionService.class.getClassLoader(),
                new Class[]{ChatSessionService.class},
                (proxy, method, args) -> {
                    if ("getSessionVO".equals(method.getName())) {
                        ChatSessionVO vo = new ChatSessionVO();
                        vo.setRoomId((Long) args[0]);
                        vo.setUnreadCount(0);
                        return vo;
                    }
                    if ("filterPushUserIds".equals(method.getName())) {
                        @SuppressWarnings("unchecked")
                        List<Long> userIds = (List<Long>) args[1];
                        return userIds;
                    }
                    return defaultValue(method.getReturnType());
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

    private static class TestableChatMessageServiceImpl extends ChatMessageServiceImpl {
        private ChatMessage messageById;
        private ChatMessage savedMessage;
        private List<ChatMessage> listResult = new ArrayList<>();
        private boolean saveResult = true;
        private boolean updateResult = true;

        @Override
        public ChatMessage getOne(Wrapper<ChatMessage> queryWrapper) {
            return null;
        }

        @Override
        public ChatMessage getById(Serializable id) {
            return messageById;
        }

        @Override
        public boolean save(ChatMessage entity) {
            if (saveResult && entity.getId() == null) {
                entity.setId(100L);
                entity.setCreateTime(new Date());
            }
            this.savedMessage = entity;
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
        public List<ChatMessage> listByIds(Collection<? extends Serializable> idList) {
            return List.of();
        }

        @Override
        public long count(Wrapper<ChatMessage> queryWrapper) {
            return 0;
        }
    }

    private static class FakeChatMqProducer extends ChatMqProducer {
        private Long lastGroupPushRoomId;
        private List<Long> lastGroupPushUserIds;

        @Override
        public void sendChatMessageGroupPush(Long roomId, ChatMessageVO chatMessageVO, List<Long> userIds) {
            this.lastGroupPushRoomId = roomId;
            this.lastGroupPushUserIds = userIds;
        }

        @Override
        public void sendMessageRead(Long roomId, Object data, String bizId) {
        }

        @Override
        public void sendMessageRead(Long roomId, Object data, String bizId, List<Long> userIds) {
        }

        @Override
        public void sendMessageRecall(Long roomId, ChatMessageVO chatMessageVO, List<Long> userIds) {
        }

        @Override
        public void sendSessionUpdate(Long userId, Long roomId, Object data, String bizId) {
        }
    }

    private static class FakeRateLimitUtils extends RateLimitUtils {
        boolean shouldThrow = false;

        @Override
        public void doRateLimit(String key, TimeModel rateInterval, Long rate, Long permit) {
            if (shouldThrow) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作过于频繁");
            }
        }

        @Override
        public void doRateLimitAndExpire(String key, TimeModel rateInterval, Long rate, Long permit,
                                         TimeModel expire) {
            if (shouldThrow) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作过于频繁");
            }
        }
    }

    private static class FakeChatAuditEventRecorder extends ChatAuditEventRecorder {
        ChatAuditEvent lastEvent;

        @Override
        public void record(ChatAuditEvent event) {
            this.lastEvent = event;
        }
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) return false;
        if (returnType == int.class) return 0;
        if (returnType == long.class) return 0L;
        return null;
    }
}
