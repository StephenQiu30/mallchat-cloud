package com.stephen.cloud.chat.service.impl;

import com.stephen.cloud.api.chat.model.dto.ChatFriendApproveRequest;
import com.stephen.cloud.api.chat.model.vo.ChatFriendApplyVO;
import com.stephen.cloud.api.user.client.UserFeignClient;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.chat.model.entity.UserFriendApply;
import com.stephen.cloud.chat.mq.producer.ChatMqProducer;
import com.stephen.cloud.chat.service.ChatRoomService;
import com.stephen.cloud.chat.service.UserFriendService;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.exception.BusinessException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

class UserFriendApplyServiceImplTest {

    private TestableUserFriendApplyServiceImpl userFriendApplyService;
    private FakeUserFriendService userFriendService;
    private FakeChatRoomService chatRoomService;
    private FakeChatMqProducer chatMqProducer;
    private Map<Long, UserVO> users;

    @BeforeEach
    void setUp() {
        userFriendApplyService = new TestableUserFriendApplyServiceImpl();
        userFriendService = new FakeUserFriendService();
        chatRoomService = new FakeChatRoomService();
        chatMqProducer = new FakeChatMqProducer();
        users = new HashMap<>();

        ReflectionTestUtils.setField(userFriendApplyService, "userFriendService", userFriendService.createProxy());
        ReflectionTestUtils.setField(userFriendApplyService, "chatRoomService", chatRoomService.createProxy());
        ReflectionTestUtils.setField(userFriendApplyService, "userFeignClient", createUserFeignClient());
        ReflectionTestUtils.setField(userFriendApplyService, "chatMqProducer", chatMqProducer);
    }

    @Test
    void shouldRejectApplyWhenTargetUserDoesNotExist() {
        UserFriendApply apply = new UserFriendApply();
        apply.setTargetId(2L);
        apply.setMsg("hello");

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> userFriendApplyService.applyFriend(apply, 1L));

        Assertions.assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), exception.getCode());
        Assertions.assertNull(chatMqProducer.lastApplyUserId);
    }

    @Test
    void shouldRejectReverseDirectionPendingApplication() {
        UserVO targetUser = new UserVO();
        targetUser.setId(2L);
        users.put(2L, targetUser);
        userFriendService.mutualFriend = false;

        UserFriendApply apply = new UserFriendApply();
        apply.setTargetId(2L);
        apply.setMsg("hello");

        UserFriendApply existing = new UserFriendApply();
        existing.setId(99L);
        existing.setUserId(2L);
        existing.setTargetId(1L);
        existing.setStatus(1);
        userFriendApplyService.existingPending = existing;

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> userFriendApplyService.applyFriend(apply, 1L));

        Assertions.assertEquals(ErrorCode.OPERATION_ERROR.getCode(), exception.getCode());
        Assertions.assertNull(chatMqProducer.lastApplyUserId);
    }

    @Test
    void shouldReturnExistingIdForSameDirectionPendingApplication() {
        UserVO targetUser = new UserVO();
        targetUser.setId(2L);
        users.put(2L, targetUser);
        userFriendService.mutualFriend = false;

        UserFriendApply apply = new UserFriendApply();
        apply.setTargetId(2L);
        apply.setMsg("hello");

        UserFriendApply existing = new UserFriendApply();
        existing.setId(99L);
        existing.setUserId(1L);
        existing.setTargetId(2L);
        existing.setStatus(1);
        userFriendApplyService.existingPending = existing;

        Long result = userFriendApplyService.applyFriend(apply, 1L);

        Assertions.assertEquals(99L, result);
        Assertions.assertNull(chatMqProducer.lastApplyUserId);
    }

    @Test
    void shouldApproveFriendByCreatingFriendshipAndPrivateRoom() {
        UserVO applyUser = new UserVO();
        applyUser.setId(1L);
        applyUser.setUserName("u1");
        users.put(1L, applyUser);

        ChatFriendApproveRequest request = new ChatFriendApproveRequest();
        request.setApplyId(10L);
        request.setStatus(2);

        UserFriendApply apply = new UserFriendApply();
        apply.setId(10L);
        apply.setUserId(1L);
        apply.setTargetId(2L);
        apply.setStatus(1);
        userFriendApplyService.applyById = apply;
        userFriendApplyService.updateResult = true;

        boolean result = userFriendApplyService.approveFriend(request, 2L);

        Assertions.assertTrue(result);
        Assertions.assertEquals(1L, userFriendService.lastAddUserId);
        Assertions.assertEquals(2L, userFriendService.lastAddFriendUserId);
        Assertions.assertEquals(1L, chatRoomService.lastPeerUserId);
        Assertions.assertEquals(2L, chatRoomService.lastUserId);
        Assertions.assertEquals(1L, chatMqProducer.lastApproveUserId);
        Assertions.assertInstanceOf(ChatFriendApplyVO.class, chatMqProducer.lastApprovePayload);
    }

    private UserFeignClient createUserFeignClient() {
        return (UserFeignClient) Proxy.newProxyInstance(
                UserFeignClient.class.getClassLoader(),
                new Class[]{UserFeignClient.class},
                (proxy, method, args) -> {
                    if ("getUserVOById".equals(method.getName())) {
                        Long userId = (Long) args[0];
                        return new BaseResponse<>(0, users.get(userId), "ok");
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private static class TestableUserFriendApplyServiceImpl extends UserFriendApplyServiceImpl {
        private UserFriendApply existingPending;
        private UserFriendApply applyById;
        private boolean updateResult;

        @Override
        public UserFriendApply getOne(com.baomidou.mybatisplus.core.conditions.Wrapper<UserFriendApply> queryWrapper) {
            return existingPending;
        }

        @Override
        public UserFriendApply getById(java.io.Serializable id) {
            return applyById;
        }

        @Override
        public boolean updateById(UserFriendApply entity) {
            this.applyById = entity;
            return updateResult;
        }
    }

    private static class FakeUserFriendService {
        private boolean mutualFriend;
        private Long lastAddUserId;
        private Long lastAddFriendUserId;

        private UserFriendService createProxy() {
            return (UserFriendService) Proxy.newProxyInstance(
                    UserFriendService.class.getClassLoader(),
                    new Class[]{UserFriendService.class},
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "isMutualFriend":
                                return mutualFriend;
                            case "addFriend":
                                lastAddUserId = (Long) args[0];
                                lastAddFriendUserId = (Long) args[1];
                                return null;
                            default:
                                return defaultValue(method.getReturnType());
                        }
                    }
            );
        }
    }

    private static class FakeChatRoomService {
        private Long lastPeerUserId;
        private Long lastUserId;

        private ChatRoomService createProxy() {
            return (ChatRoomService) Proxy.newProxyInstance(
                    ChatRoomService.class.getClassLoader(),
                    new Class[]{ChatRoomService.class},
                    (proxy, method, args) -> {
                        if ("getOrCreatePrivateRoom".equals(method.getName())) {
                            lastPeerUserId = (Long) args[0];
                            lastUserId = (Long) args[1];
                            return 100L;
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
        }
    }

    private static class FakeChatMqProducer extends ChatMqProducer {
        private Long lastApplyUserId;
        private Object lastApplyPayload;
        private Long lastApproveUserId;
        private Object lastApprovePayload;

        @Override
        public void sendFriendApply(Long userId, Object data, String bizId) {
            this.lastApplyUserId = userId;
            this.lastApplyPayload = data;
        }

        @Override
        public void sendFriendApprove(Long userId, Object data, String bizId) {
            this.lastApproveUserId = userId;
            this.lastApprovePayload = data;
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
