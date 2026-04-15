package com.stephen.cloud.chat.service.impl;

import com.stephen.cloud.chat.model.entity.ChatPrivateRoom;
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

    private ChatRoomServiceImpl chatRoomService;
    private boolean mutualFriend;
    private ChatPrivateRoom existingRoom;

    @BeforeEach
    void setUp() {
        chatRoomService = new ChatRoomServiceImpl();
        ReflectionTestUtils.setField(chatRoomService, "userFriendService", createUserFriendService());
        ReflectionTestUtils.setField(chatRoomService, "chatPrivateRoomService", createChatPrivateRoomService());
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
                    return defaultValue(method.getReturnType());
                }
        );
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
