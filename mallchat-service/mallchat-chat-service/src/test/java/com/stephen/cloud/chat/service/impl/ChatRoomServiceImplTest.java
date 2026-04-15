package com.stephen.cloud.chat.service.impl;

import com.stephen.cloud.chat.model.entity.ChatRoom;
import com.stephen.cloud.chat.model.entity.ChatPrivateRoom;
import com.stephen.cloud.chat.service.ChatRoomMemberService;
import com.stephen.cloud.chat.service.ChatSessionService;
import com.stephen.cloud.chat.service.ChatPrivateRoomService;
import com.stephen.cloud.chat.service.UserFriendService;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.exception.BusinessException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;

class ChatRoomServiceImplTest {

    private TestableChatRoomServiceImpl chatRoomService;
    private boolean mutualFriend;
    private ChatPrivateRoom existingRoom;
    private boolean privateRoomMappingSaved;
    private Long addedMemberOne;
    private Long addedMemberTwo;
    private boolean currentUserIsMember;

    @BeforeEach
    void setUp() {
        chatRoomService = new TestableChatRoomServiceImpl();
        ReflectionTestUtils.setField(chatRoomService, "userFriendService", createUserFriendService());
        ReflectionTestUtils.setField(chatRoomService, "chatPrivateRoomService", createChatPrivateRoomService());
        ReflectionTestUtils.setField(chatRoomService, "chatRoomMemberService", createChatRoomMemberService());
        ReflectionTestUtils.setField(chatRoomService, "chatSessionService", createChatSessionService());
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
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private ChatSessionService createChatSessionService() {
        return (ChatSessionService) Proxy.newProxyInstance(
                ChatSessionService.class.getClassLoader(),
                new Class[]{ChatSessionService.class},
                (proxy, method, args) -> defaultValue(method.getReturnType())
        );
    }

    private static final class TestableChatRoomServiceImpl extends ChatRoomServiceImpl {
        private ChatRoom stubRoom;

        @Override
        public ChatRoom getById(java.io.Serializable id) {
            return stubRoom;
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
