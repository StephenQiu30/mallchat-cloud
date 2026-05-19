package com.stephen.cloud.chat.service.impl;

import com.stephen.cloud.api.chat.model.enums.ChatRoomTypeEnum;
import com.stephen.cloud.api.chat.model.enums.ChatRoomRoleEnum;
import com.stephen.cloud.api.chat.model.vo.ChatSessionVO;
import com.stephen.cloud.chat.model.entity.ChatGroupInfo;
import com.stephen.cloud.chat.model.entity.ChatRoom;
import com.stephen.cloud.chat.model.entity.ChatPrivateRoom;
import com.stephen.cloud.chat.model.entity.ChatRoomMember;
import com.stephen.cloud.chat.mq.producer.ChatMqProducer;
import com.stephen.cloud.chat.service.ChatRoomMemberService;
import com.stephen.cloud.chat.service.ChatSessionService;
import com.stephen.cloud.chat.service.ChatPrivateRoomService;
import com.stephen.cloud.chat.service.ChatGroupInfoService;
import com.stephen.cloud.chat.service.UserFriendService;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.exception.BusinessException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

class ChatRoomServiceImplTest {

    private TestableChatRoomServiceImpl chatRoomService;
    private boolean mutualFriend;
    private ChatPrivateRoom existingRoom;
    private boolean privateRoomMappingSaved;
    private Long addedMemberOne;
    private Long addedMemberTwo;
    private boolean currentUserIsMember;
    private boolean currentUserIsOwner;
    private ChatGroupInfo stubGroupInfo;
    private ChatGroupInfo savedGroupInfo;
    private ChatGroupInfo updatedGroupInfo;
    private boolean groupInfoValidated;
    private List<ChatRoomMember> roomMembers;
    private List<Long> sessionUpdateUsers;
    private List<Object> sessionUpdatePayloads;
    private List<String> sessionUpdateBizIds;
    private boolean sessionPushThrows;
    private ChatRoomMember targetMember;
    private Long leftRoomId;
    private Long leftUserId;
    private Long removedSessionRoomId;
    private Long removedSessionUserId;
    private List<Long> sessionDeleteUsers;
    private boolean sessionDeleteThrows;
    private boolean sessionRemoveThrows;

    @BeforeEach
    void setUp() {
        chatRoomService = new TestableChatRoomServiceImpl();
        ReflectionTestUtils.setField(chatRoomService, "userFriendService", createUserFriendService());
        ReflectionTestUtils.setField(chatRoomService, "chatPrivateRoomService", createChatPrivateRoomService());
        ReflectionTestUtils.setField(chatRoomService, "chatRoomMemberService", createChatRoomMemberService());
        ReflectionTestUtils.setField(chatRoomService, "chatGroupInfoService", createChatGroupInfoService());
        ReflectionTestUtils.setField(chatRoomService, "chatSessionService", createChatSessionService());
        ReflectionTestUtils.setField(chatRoomService, "chatMqProducer", createChatMqProducer());
        roomMembers = new ArrayList<>();
        sessionUpdateUsers = new ArrayList<>();
        sessionUpdatePayloads = new ArrayList<>();
        sessionUpdateBizIds = new ArrayList<>();
        sessionDeleteUsers = new ArrayList<>();
    }

    @Test
    void shouldReuseExistingPrivateRoomForConfirmedFriends() {
        mutualFriend = true;
        existingRoom = new ChatPrivateRoom();
        existingRoom.setRoomId(88L);

        Long roomId = chatRoomService.getOrCreatePrivateRoom(2L, 1L);

        Assertions.assertEquals(88L, roomId);
    }

    @Test
    void shouldRejectPrivateRoomCreationForNonFriends() {
        mutualFriend = false;

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatRoomService.getOrCreatePrivateRoom(2L, 1L));

        Assertions.assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
    }

    @Test
    void shouldRejectManualJoinForExistingRoom() {
        chatRoomService.stubRoom = new ChatRoom();
        chatRoomService.stubRoom.setId(66L);

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatRoomService.joinChatRoom(66L, 1L));

        Assertions.assertEquals(ErrorCode.FORBIDDEN_ERROR.getCode(), exception.getCode());
    }

    @Test
    void shouldCreatePrivateRoomThroughControlledMembershipPath() {
        mutualFriend = true;
        existingRoom = null;

        Long roomId = chatRoomService.getOrCreatePrivateRoom(2L, 1L);

        Assertions.assertEquals(100L, roomId);
        Assertions.assertEquals(1L, addedMemberOne);
        Assertions.assertEquals(2L, addedMemberTwo);
        Assertions.assertTrue(privateRoomMappingSaved);
    }

    @Test
    void shouldRejectRoomDetailForNonMember() {
        chatRoomService.stubRoom = new ChatRoom();
        chatRoomService.stubRoom.setId(77L);
        currentUserIsMember = false;

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatRoomService.getRoomDetail(77L, 3L));

        Assertions.assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
    }

    @Test
    void shouldRejectGroupProfileUpdateForPrivateRoom() {
        chatRoomService.stubRoom = new ChatRoom();
        chatRoomService.stubRoom.setId(90L);
        chatRoomService.stubRoom.setType(ChatRoomTypeEnum.PRIVATE.getCode());

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatRoomService.updateGroupProfile(90L, "new-name", null, null, 1L));

        Assertions.assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
    }

    @Test
    void shouldRejectGroupProfileUpdateForNonOwner() {
        chatRoomService.stubRoom = buildGroupRoom();
        currentUserIsOwner = false;

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatRoomService.updateGroupProfile(90L, "new-name", null, null, 2L));

        Assertions.assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
    }

    @Test
    void shouldRejectGroupProfileUpdateWithoutPayload() {
        chatRoomService.stubRoom = buildGroupRoom();
        currentUserIsOwner = true;

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatRoomService.updateGroupProfile(90L, null, null, null, 1L));

        Assertions.assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
    }

    @Test
    void shouldUpdateGroupProfileForOwnerAndPushSessionRefresh() {
        chatRoomService.stubRoom = buildGroupRoom();
        currentUserIsOwner = true;
        stubGroupInfo = new ChatGroupInfo();
        stubGroupInfo.setId(30L);
        stubGroupInfo.setRoomId(90L);
        stubGroupInfo.setGroupName("old-name");
        stubGroupInfo.setGroupAvatar("old-avatar");
        roomMembers.add(buildMember(90L, 1L));
        roomMembers.add(buildMember(90L, 2L));

        chatRoomService.updateGroupProfile(90L, "new-name", "new-avatar", "new-announcement", 1L);

        Assertions.assertEquals("new-name", chatRoomService.stubRoom.getName());
        Assertions.assertEquals("new-avatar", chatRoomService.stubRoom.getAvatar());
        Assertions.assertSame(stubGroupInfo, updatedGroupInfo);
        Assertions.assertEquals("new-name", updatedGroupInfo.getGroupName());
        Assertions.assertEquals("new-avatar", updatedGroupInfo.getGroupAvatar());
        Assertions.assertEquals("new-announcement", updatedGroupInfo.getAnnouncement());
        Assertions.assertTrue(groupInfoValidated);
        Assertions.assertEquals(List.of(1L, 2L), sessionUpdateUsers);
        ChatSessionVO sessionVO = (ChatSessionVO) sessionUpdatePayloads.get(0);
        Assertions.assertEquals("new-name", sessionVO.getName());
        Assertions.assertEquals("new-avatar", sessionVO.getAvatar());
        Assertions.assertEquals("session_room_profile_update:90:1", sessionUpdateBizIds.get(0));
    }

    @Test
    void shouldCreateGroupInfoWithRoomDefaultsWhenAnnouncementUpdatedWithoutExistingInfo() {
        chatRoomService.stubRoom = buildGroupRoom();
        currentUserIsOwner = true;
        stubGroupInfo = null;

        chatRoomService.updateGroupProfile(90L, null, null, "announcement-only", 1L);

        Assertions.assertNotNull(savedGroupInfo);
        Assertions.assertEquals(90L, savedGroupInfo.getRoomId());
        Assertions.assertEquals("old-name", savedGroupInfo.getGroupName());
        Assertions.assertEquals("old-avatar", savedGroupInfo.getGroupAvatar());
        Assertions.assertEquals("announcement-only", savedGroupInfo.getAnnouncement());
        Assertions.assertEquals(1L, savedGroupInfo.getCreateUser());
        Assertions.assertTrue(groupInfoValidated);
    }

    @Test
    void shouldRepairBlankExistingGroupInfoWhenAnnouncementUpdated() {
        chatRoomService.stubRoom = buildGroupRoom();
        currentUserIsOwner = true;
        stubGroupInfo = new ChatGroupInfo();
        stubGroupInfo.setId(31L);
        stubGroupInfo.setRoomId(90L);
        stubGroupInfo.setGroupName("");
        stubGroupInfo.setGroupAvatar(null);

        chatRoomService.updateGroupProfile(90L, null, null, "new-announcement", 1L);

        Assertions.assertSame(stubGroupInfo, updatedGroupInfo);
        Assertions.assertEquals("old-name", updatedGroupInfo.getGroupName());
        Assertions.assertEquals("old-avatar", updatedGroupInfo.getGroupAvatar());
        Assertions.assertEquals("new-announcement", updatedGroupInfo.getAnnouncement());
    }

    @Test
    void shouldRejectBlankGroupAvatar() {
        chatRoomService.stubRoom = buildGroupRoom();
        currentUserIsOwner = true;

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatRoomService.updateGroupProfile(90L, null, " ", null, 1L));

        Assertions.assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
    }

    @Test
    void shouldNotFailGroupProfileUpdateWhenSessionPushThrows() {
        chatRoomService.stubRoom = buildGroupRoom();
        currentUserIsOwner = true;
        stubGroupInfo = new ChatGroupInfo();
        stubGroupInfo.setId(32L);
        stubGroupInfo.setRoomId(90L);
        roomMembers.add(buildMember(90L, 1L));
        sessionPushThrows = true;

        Assertions.assertDoesNotThrow(() -> chatRoomService.updateGroupProfile(90L, "new-name", null, null, 1L));

        Assertions.assertEquals("new-name", chatRoomService.stubRoom.getName());
        Assertions.assertEquals("new-name", updatedGroupInfo.getGroupName());
        Assertions.assertTrue(groupInfoValidated);
    }

    @Test
    void shouldRejectMemberRemovalForPrivateRoom() {
        chatRoomService.stubRoom = new ChatRoom();
        chatRoomService.stubRoom.setId(90L);
        chatRoomService.stubRoom.setType(ChatRoomTypeEnum.PRIVATE.getCode());
        currentUserIsOwner = true;

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatRoomService.removeMember(90L, 2L, 1L));

        Assertions.assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        assertNoRemovalSideEffects();
    }

    @Test
    void shouldRejectMemberRemovalForNonOwner() {
        chatRoomService.stubRoom = buildGroupRoom();
        currentUserIsOwner = false;
        targetMember = buildMember(90L, 2L, ChatRoomRoleEnum.MEMBER.getCode());

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatRoomService.removeMember(90L, 2L, 3L));

        Assertions.assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
        assertNoRemovalSideEffects();
    }

    @Test
    void shouldRejectMemberRemovalWhenTargetMissing() {
        chatRoomService.stubRoom = buildGroupRoom();
        currentUserIsOwner = true;
        targetMember = null;

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatRoomService.removeMember(90L, 2L, 1L));

        Assertions.assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), exception.getCode());
        assertNoRemovalSideEffects();
    }

    @Test
    void shouldRejectMemberRemovalForSelf() {
        chatRoomService.stubRoom = buildGroupRoom();
        currentUserIsOwner = true;
        targetMember = buildMember(90L, 1L, ChatRoomRoleEnum.OWNER.getCode());

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatRoomService.removeMember(90L, 1L, 1L));

        Assertions.assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        assertNoRemovalSideEffects();
    }

    @Test
    void shouldRejectMemberRemovalForOwnerAccount() {
        chatRoomService.stubRoom = buildGroupRoom();
        currentUserIsOwner = true;
        targetMember = buildMember(90L, 2L, ChatRoomRoleEnum.OWNER.getCode());

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatRoomService.removeMember(90L, 2L, 1L));

        Assertions.assertEquals(ErrorCode.OPERATION_ERROR.getCode(), exception.getCode());
        assertNoRemovalSideEffects();
    }

    @Test
    void shouldRejectMemberRemovalForAdminRole() {
        chatRoomService.stubRoom = buildGroupRoom();
        currentUserIsOwner = true;
        targetMember = buildMember(90L, 2L, ChatRoomRoleEnum.ADMIN.getCode());

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatRoomService.removeMember(90L, 2L, 1L));

        Assertions.assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
        assertNoRemovalSideEffects();
    }

    @Test
    void shouldRemoveGroupMemberAndDeleteTargetSession() {
        chatRoomService.stubRoom = buildGroupRoom();
        currentUserIsOwner = true;
        targetMember = buildMember(90L, 2L, ChatRoomRoleEnum.MEMBER.getCode());

        chatRoomService.removeMember(90L, 2L, 1L);

        Assertions.assertEquals(90L, leftRoomId);
        Assertions.assertEquals(2L, leftUserId);
        Assertions.assertEquals(90L, removedSessionRoomId);
        Assertions.assertEquals(2L, removedSessionUserId);
        Assertions.assertEquals(List.of(2L), sessionDeleteUsers);
    }

    @Test
    void shouldNotFailMemberRemovalWhenSessionDeletePushThrows() {
        chatRoomService.stubRoom = buildGroupRoom();
        currentUserIsOwner = true;
        targetMember = buildMember(90L, 2L, ChatRoomRoleEnum.MEMBER.getCode());
        sessionDeleteThrows = true;

        Assertions.assertDoesNotThrow(() -> chatRoomService.removeMember(90L, 2L, 1L));

        Assertions.assertEquals(90L, leftRoomId);
        Assertions.assertEquals(2L, leftUserId);
        Assertions.assertEquals(90L, removedSessionRoomId);
        Assertions.assertEquals(2L, removedSessionUserId);
    }

    @Test
    void shouldFailMemberRemovalWhenSessionDeletePersistenceThrows() {
        chatRoomService.stubRoom = buildGroupRoom();
        currentUserIsOwner = true;
        targetMember = buildMember(90L, 2L, ChatRoomRoleEnum.MEMBER.getCode());
        sessionRemoveThrows = true;

        RuntimeException exception = Assertions.assertThrows(RuntimeException.class,
                () -> chatRoomService.removeMember(90L, 2L, 1L));

        Assertions.assertEquals("session remove failed", exception.getMessage());
        Assertions.assertNull(leftRoomId);
        Assertions.assertNull(leftUserId);
        Assertions.assertTrue(sessionDeleteUsers.isEmpty());
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

    private ChatPrivateRoomService createChatPrivateRoomService() {
        return (ChatPrivateRoomService) Proxy.newProxyInstance(
                ChatPrivateRoomService.class.getClassLoader(),
                new Class[]{ChatPrivateRoomService.class},
                (proxy, method, args) -> {
                    if ("getOne".equals(method.getName())) {
                        return existingRoom;
                    }
                    if ("save".equals(method.getName())) {
                        privateRoomMappingSaved = true;
                        return true;
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
                    if ("addMember".equals(method.getName()) && args.length >= 2) {
                        if (addedMemberOne == null) {
                            addedMemberOne = (Long) args[1];
                        } else {
                            addedMemberTwo = (Long) args[1];
                        }
                        return null;
                    }
                    if ("isMember".equals(method.getName())) {
                        return currentUserIsMember;
                    }
                    if ("isOwner".equals(method.getName())) {
                        return currentUserIsOwner;
                    }
                    if ("listByRoomId".equals(method.getName())) {
                        return roomMembers;
                    }
                    if ("getMember".equals(method.getName())) {
                        return targetMember;
                    }
                    if ("leaveRoom".equals(method.getName())) {
                        leftRoomId = (Long) args[0];
                        leftUserId = (Long) args[1];
                        return null;
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
                    if ("getOne".equals(method.getName())) {
                        return stubGroupInfo;
                    }
                    if ("validChatGroupInfo".equals(method.getName())) {
                        groupInfoValidated = true;
                        return null;
                    }
                    if ("save".equals(method.getName())) {
                        savedGroupInfo = (ChatGroupInfo) args[0];
                        return true;
                    }
                    if ("updateById".equals(method.getName())) {
                        updatedGroupInfo = (ChatGroupInfo) args[0];
                        return true;
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
                        ChatSessionVO sessionVO = new ChatSessionVO();
                        sessionVO.setRoomId((Long) args[0]);
                        ChatGroupInfo currentGroupInfo = updatedGroupInfo != null ? updatedGroupInfo : savedGroupInfo;
                        if (currentGroupInfo != null) {
                            sessionVO.setName(currentGroupInfo.getGroupName());
                            sessionVO.setAvatar(currentGroupInfo.getGroupAvatar());
                        }
                        return sessionVO;
                    }
                    if ("remove".equals(method.getName())) {
                        if (sessionRemoveThrows) {
                            throw new RuntimeException("session remove failed");
                        }
                        removedSessionRoomId = targetMember == null ? null : targetMember.getRoomId();
                        removedSessionUserId = targetMember == null ? null : targetMember.getUserId();
                        return true;
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private ChatMqProducer createChatMqProducer() {
        return new ChatMqProducer() {
            @Override
            public void sendSessionUpdate(Long userId, Long roomId, Object data, String bizId) {
                if (sessionPushThrows) {
                    throw new RuntimeException("session push failed");
                }
                sessionUpdateUsers.add(userId);
                sessionUpdatePayloads.add(data);
                sessionUpdateBizIds.add(bizId);
            }

            @Override
            public void sendSessionDelete(Long userId, Long roomId, String bizId) {
                if (sessionDeleteThrows) {
                    throw new RuntimeException("session delete failed");
                }
                sessionDeleteUsers.add(userId);
            }
        };
    }

    private ChatRoom buildGroupRoom() {
        ChatRoom room = new ChatRoom();
        room.setId(90L);
        room.setName("old-name");
        room.setAvatar("old-avatar");
        room.setType(ChatRoomTypeEnum.GROUP.getCode());
        room.setCreateUser(1L);
        return room;
    }

    private ChatRoomMember buildMember(Long roomId, Long userId) {
        return buildMember(roomId, userId, ChatRoomRoleEnum.MEMBER.getCode());
    }

    private ChatRoomMember buildMember(Long roomId, Long userId, Integer role) {
        ChatRoomMember member = new ChatRoomMember();
        member.setRoomId(roomId);
        member.setUserId(userId);
        member.setRole(role);
        return member;
    }

    private void assertNoRemovalSideEffects() {
        Assertions.assertNull(leftRoomId);
        Assertions.assertNull(leftUserId);
        Assertions.assertNull(removedSessionRoomId);
        Assertions.assertNull(removedSessionUserId);
        Assertions.assertTrue(sessionDeleteUsers.isEmpty());
    }

    private static final class TestableChatRoomServiceImpl extends ChatRoomServiceImpl {
        private ChatRoom stubRoom;

        @Override
        public ChatRoom getById(java.io.Serializable id) {
            return stubRoom;
        }

        @Override
        public boolean updateById(ChatRoom entity) {
            this.stubRoom = entity;
            return true;
        }

        @Override
        public boolean save(ChatRoom entity) {
            entity.setId(100L);
            stubRoom = entity;
            return true;
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
