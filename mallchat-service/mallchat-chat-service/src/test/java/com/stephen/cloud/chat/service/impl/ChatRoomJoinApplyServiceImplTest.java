package com.stephen.cloud.chat.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.chat.model.dto.ChatRoomJoinApplyRequest;
import com.stephen.cloud.api.chat.model.dto.ChatRoomJoinApproveRequest;
import com.stephen.cloud.api.chat.model.enums.ChatRoomRoleEnum;
import com.stephen.cloud.api.chat.model.enums.ChatRoomTypeEnum;
import com.stephen.cloud.api.chat.model.vo.ChatRoomJoinApplyVO;
import com.stephen.cloud.api.notification.client.NotificationFeignClient;
import com.stephen.cloud.api.notification.model.dto.NotificationCreateRequest;
import com.stephen.cloud.chat.model.entity.ChatRoom;
import com.stephen.cloud.chat.model.entity.ChatRoomJoinApply;
import com.stephen.cloud.chat.model.entity.ChatRoomMember;
import com.stephen.cloud.chat.service.ChatRoomMemberService;
import com.stephen.cloud.chat.service.ChatRoomService;
import com.stephen.cloud.chat.service.ChatSessionService;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.exception.BusinessException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

class ChatRoomJoinApplyServiceImplTest {

    private TestableChatRoomJoinApplyServiceImpl service;
    private ChatRoom room;
    private boolean member;
    private ChatRoomMember reviewerMember;
    private ChatRoomJoinApply existingPending;
    private ChatRoomJoinApply storedApply;
    private List<Long> addedMembers;
    private List<Long> updatedSessions;
    private List<NotificationCreateRequest> notifications;
    private boolean notificationFails;
    private boolean conditionalUpdateResult;
    private boolean conditionalUpdateUsed;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                ChatRoomJoinApply.class);
        service = new TestableChatRoomJoinApplyServiceImpl();
        room = new ChatRoom();
        room.setId(90L);
        room.setType(ChatRoomTypeEnum.GROUP.getCode());
        room.setName("group");
        member = false;
        reviewerMember = buildMember(90L, 1L, ChatRoomRoleEnum.OWNER.getCode());
        addedMembers = new ArrayList<>();
        updatedSessions = new ArrayList<>();
        notifications = new ArrayList<>();
        conditionalUpdateResult = true;

        ReflectionTestUtils.setField(service, "chatRoomService", createChatRoomService());
        ReflectionTestUtils.setField(service, "chatRoomMemberService", createChatRoomMemberService());
        ReflectionTestUtils.setField(service, "chatSessionService", createChatSessionService());
        ReflectionTestUtils.setField(service, "notificationFeignClient", createNotificationFeignClient());
    }

    @Test
    void shouldCreatePendingJoinApplyAndNotifyManagers() {
        ChatRoomJoinApplyRequest request = new ChatRoomJoinApplyRequest();
        request.setRoomId(90L);
        request.setMsg("please");

        Long applyId = service.applyJoinRoom(request, 2L);

        Assertions.assertEquals(100L, applyId);
        Assertions.assertEquals(90L, storedApply.getRoomId());
        Assertions.assertEquals(2L, storedApply.getUserId());
        Assertions.assertEquals(1, storedApply.getStatus());
        Assertions.assertEquals("90:2", storedApply.getActiveKey());
        Assertions.assertEquals(1, notifications.size());
        Assertions.assertEquals(1L, notifications.get(0).getUserId());
        Assertions.assertEquals("chat_room_join_apply", notifications.get(0).getRelatedType());
    }

    @Test
    void shouldReturnExistingPendingApplyIdWhenApplyingRepeatedly() {
        existingPending = new ChatRoomJoinApply();
        existingPending.setId(88L);
        existingPending.setRoomId(90L);
        existingPending.setUserId(2L);
        existingPending.setStatus(1);

        ChatRoomJoinApplyRequest request = new ChatRoomJoinApplyRequest();
        request.setRoomId(90L);
        request.setMsg("again");

        Long applyId = service.applyJoinRoom(request, 2L);

        Assertions.assertEquals(88L, applyId);
        Assertions.assertNull(storedApply);
    }

    @Test
    void shouldRejectJoinApplyWhenUserAlreadyMember() {
        member = true;
        ChatRoomJoinApplyRequest request = new ChatRoomJoinApplyRequest();
        request.setRoomId(90L);

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> service.applyJoinRoom(request, 2L));

        Assertions.assertEquals(ErrorCode.OPERATION_ERROR.getCode(), exception.getCode());
    }

    @Test
    void shouldApproveJoinApplyByOwnerAndCreateMemberSession() {
        storedApply = buildPendingApply();
        ChatRoomJoinApproveRequest request = new ChatRoomJoinApproveRequest();
        request.setApplyId(100L);
        request.setStatus(2);

        boolean result = service.approveJoinRoom(request, 1L);

        Assertions.assertTrue(result);
        Assertions.assertTrue(service.conditionalUpdateSqlSegment.contains("id"));
        Assertions.assertTrue(service.conditionalUpdateSqlSegment.contains("status"));
        Assertions.assertEquals(2, storedApply.getStatus());
        Assertions.assertNull(storedApply.getActiveKey());
        Assertions.assertEquals(List.of(2L), addedMembers);
        Assertions.assertEquals(List.of(2L), updatedSessions);
        Assertions.assertEquals(1, notifications.size());
        Assertions.assertEquals(2L, notifications.get(0).getUserId());
    }

    @Test
    void shouldRejectApprovalByOrdinaryMember() {
        storedApply = buildPendingApply();
        reviewerMember = buildMember(90L, 1L, ChatRoomRoleEnum.MEMBER.getCode());
        ChatRoomJoinApproveRequest request = new ChatRoomJoinApproveRequest();
        request.setApplyId(100L);
        request.setStatus(2);

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> service.approveJoinRoom(request, 1L));

        Assertions.assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
        Assertions.assertTrue(addedMembers.isEmpty());
    }

    @Test
    void shouldRejectApprovalWhenPendingStatusUpdateLosesRace() {
        storedApply = buildPendingApply();
        conditionalUpdateResult = false;
        ChatRoomJoinApproveRequest request = new ChatRoomJoinApproveRequest();
        request.setApplyId(100L);
        request.setStatus(2);

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> service.approveJoinRoom(request, 1L));

        Assertions.assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        Assertions.assertTrue(conditionalUpdateUsed);
        Assertions.assertTrue(addedMembers.isEmpty());
        Assertions.assertTrue(updatedSessions.isEmpty());
        Assertions.assertTrue(notifications.isEmpty());
    }

    @Test
    void shouldRejectJoinApplyWithoutCreatingMember() {
        storedApply = buildPendingApply();
        ChatRoomJoinApproveRequest request = new ChatRoomJoinApproveRequest();
        request.setApplyId(100L);
        request.setStatus(3);

        boolean result = service.approveJoinRoom(request, 1L);

        Assertions.assertTrue(result);
        Assertions.assertEquals(3, storedApply.getStatus());
        Assertions.assertTrue(addedMembers.isEmpty());
        Assertions.assertEquals(1, notifications.size());
    }

    @Test
    void shouldKeepApplyFactWhenNotificationFails() {
        notificationFails = true;
        ChatRoomJoinApplyRequest request = new ChatRoomJoinApplyRequest();
        request.setRoomId(90L);

        Long applyId = Assertions.assertDoesNotThrow(() -> service.applyJoinRoom(request, 2L));

        Assertions.assertEquals(100L, applyId);
        Assertions.assertNotNull(storedApply);
        Assertions.assertTrue(notifications.isEmpty());
    }

    @Test
    void shouldListJoinAppliesForOwner() {
        storedApply = buildPendingApply();
        service.pageResult = new Page<>(1, 20, 1);
        service.pageResult.setRecords(List.of(storedApply));

        Page<ChatRoomJoinApplyVO> page = service.listRoomJoinApplyPage(90L, 1, 20, 1L);

        Assertions.assertEquals(1, page.getTotal());
        Assertions.assertEquals(100L, page.getRecords().get(0).getId());
    }

    @Test
    void shouldNotAddMemberWhenApprovingAlreadyApprovedApply() {
        storedApply = buildPendingApply();
        storedApply.setStatus(2);
        storedApply.setActiveKey(null);

        ChatRoomJoinApproveRequest request = new ChatRoomJoinApproveRequest();
        request.setApplyId(100L);
        request.setStatus(2);

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> service.approveJoinRoom(request, 1L));

        Assertions.assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        Assertions.assertTrue(addedMembers.isEmpty());
    }

    @Test
    void shouldCompleteJoinApprovalFlowWithMemberSessionAndNotification() {
        storedApply = buildPendingApply();

        ChatRoomJoinApproveRequest request = new ChatRoomJoinApproveRequest();
        request.setApplyId(100L);
        request.setStatus(2);

        boolean result = service.approveJoinRoom(request, 1L);

        Assertions.assertTrue(result);
        Assertions.assertEquals(2, storedApply.getStatus());
        Assertions.assertNull(storedApply.getActiveKey());
        Assertions.assertEquals(List.of(2L), addedMembers);
        Assertions.assertEquals(List.of(2L), updatedSessions);
        Assertions.assertEquals(1, notifications.size());
        Assertions.assertEquals(2L, notifications.get(0).getUserId());
    }

    @Test
    void shouldRejectJoinApplyAndNotifyApplicant() {
        storedApply = buildPendingApply();

        ChatRoomJoinApproveRequest request = new ChatRoomJoinApproveRequest();
        request.setApplyId(100L);
        request.setStatus(3);

        boolean result = service.approveJoinRoom(request, 1L);

        Assertions.assertTrue(result);
        Assertions.assertEquals(3, storedApply.getStatus());
        Assertions.assertNull(storedApply.getActiveKey());
        Assertions.assertTrue(addedMembers.isEmpty());
        Assertions.assertTrue(updatedSessions.isEmpty());
        Assertions.assertEquals(1, notifications.size());
        Assertions.assertEquals(2L, notifications.get(0).getUserId());
    }

    @Test
    void shouldNotifyManagersWhenNewJoinApplyCreated() {
        ChatRoomJoinApplyRequest request = new ChatRoomJoinApplyRequest();
        request.setRoomId(90L);
        request.setMsg("please let me in");

        Long applyId = service.applyJoinRoom(request, 2L);

        Assertions.assertEquals(100L, applyId);
        Assertions.assertEquals(1, notifications.size());
        Assertions.assertEquals(1L, notifications.get(0).getUserId());
        Assertions.assertEquals("入群申请", notifications.get(0).getTitle());
    }

    private ChatRoomJoinApply buildPendingApply() {
        ChatRoomJoinApply apply = new ChatRoomJoinApply();
        apply.setId(100L);
        apply.setRoomId(90L);
        apply.setUserId(2L);
        apply.setMsg("please");
        apply.setStatus(1);
        apply.setActiveKey("90:2");
        return apply;
    }

    private ChatRoomMember buildMember(Long roomId, Long userId, Integer role) {
        ChatRoomMember member = new ChatRoomMember();
        member.setRoomId(roomId);
        member.setUserId(userId);
        member.setRole(role);
        return member;
    }

    private ChatRoomService createChatRoomService() {
        return (ChatRoomService) Proxy.newProxyInstance(
                ChatRoomService.class.getClassLoader(),
                new Class[]{ChatRoomService.class},
                (proxy, method, args) -> "getById".equals(method.getName()) ? room : defaultValue(method.getReturnType())
        );
    }

    private ChatRoomMemberService createChatRoomMemberService() {
        return (ChatRoomMemberService) Proxy.newProxyInstance(
                ChatRoomMemberService.class.getClassLoader(),
                new Class[]{ChatRoomMemberService.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isMember" -> member;
                    case "getMember" -> reviewerMember;
                    case "listByRoomId" -> List.of(reviewerMember);
                    case "addMember" -> {
                        addedMembers.add((Long) args[1]);
                        yield null;
                    }
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private ChatSessionService createChatSessionService() {
        return (ChatSessionService) Proxy.newProxyInstance(
                ChatSessionService.class.getClassLoader(),
                new Class[]{ChatSessionService.class},
                (proxy, method, args) -> {
                    if ("updateSession".equals(method.getName())) {
                        updatedSessions.add((Long) args[0]);
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private NotificationFeignClient createNotificationFeignClient() {
        return (NotificationFeignClient) Proxy.newProxyInstance(
                NotificationFeignClient.class.getClassLoader(),
                new Class[]{NotificationFeignClient.class},
                (proxy, method, args) -> {
                    if ("addBusinessNotification".equals(method.getName())) {
                        if (notificationFails) {
                            throw new RuntimeException("notification unavailable");
                        }
                        notifications.add((NotificationCreateRequest) args[0]);
                        return new BaseResponse<>(0, 1L, "ok");
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private class TestableChatRoomJoinApplyServiceImpl extends ChatRoomJoinApplyServiceImpl {
        private Page<ChatRoomJoinApply> pageResult = new Page<>(1, 20, 0);
        private String conditionalUpdateSqlSegment;

        @Override
        public ChatRoomJoinApply getOne(Wrapper<ChatRoomJoinApply> queryWrapper) {
            return existingPending;
        }

        @Override
        public ChatRoomJoinApply getById(java.io.Serializable id) {
            return storedApply;
        }

        @Override
        public boolean save(ChatRoomJoinApply entity) {
            entity.setId(100L);
            storedApply = entity;
            return true;
        }

        @Override
        public boolean update(Wrapper<ChatRoomJoinApply> updateWrapper) {
            conditionalUpdateUsed = true;
            conditionalUpdateSqlSegment = updateWrapper.getSqlSegment();
            return conditionalUpdateResult;
        }

        @Override
        public <E extends IPage<ChatRoomJoinApply>> E page(E page, Wrapper<ChatRoomJoinApply> queryWrapper) {
            @SuppressWarnings("unchecked")
            E result = (E) pageResult;
            return result;
        }
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
}
