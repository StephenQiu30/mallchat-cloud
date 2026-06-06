package com.stephen.cloud.chat.service.impl;

import com.stephen.cloud.api.chat.model.enums.ChatRoomRoleEnum;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.stephen.cloud.chat.model.entity.ChatRoomMember;
import com.stephen.cloud.common.cache.constants.ChatCacheConstant;
import com.stephen.cloud.common.cache.utils.CacheUtils;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.exception.BusinessException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class ChatRoomMemberServiceImplTest {

    private TestableChatRoomMemberServiceImpl chatRoomMemberService;
    private ChatRoomMember existingMember;
    private ChatRoomMember updatedMember;

    @BeforeEach
    void setUp() {
        chatRoomMemberService = new TestableChatRoomMemberServiceImpl();
    }

    @Test
    void shouldKeepExistingAdminRoleWhenDuplicateInviteAddsMemberRole() {
        existingMember = buildMember(ChatRoomRoleEnum.ADMIN.getCode());

        chatRoomMemberService.addMember(10L, 2L, ChatRoomRoleEnum.MEMBER.getCode());

        Assertions.assertEquals(ChatRoomRoleEnum.ADMIN.getCode(), existingMember.getRole());
        Assertions.assertNull(updatedMember);
    }

    @Test
    void shouldKeepExistingOwnerRoleWhenDuplicateInviteAddsMemberRole() {
        existingMember = buildMember(ChatRoomRoleEnum.OWNER.getCode());

        chatRoomMemberService.addMember(10L, 2L, ChatRoomRoleEnum.MEMBER.getCode());

        Assertions.assertEquals(ChatRoomRoleEnum.OWNER.getCode(), existingMember.getRole());
        Assertions.assertNull(updatedMember);
    }

    @Test
    void shouldKeepExistingMemberRoleWhenDuplicateInviteAddsMemberRole() {
        existingMember = buildMember(ChatRoomRoleEnum.MEMBER.getCode());

        chatRoomMemberService.addMember(10L, 2L, ChatRoomRoleEnum.MEMBER.getCode());

        Assertions.assertEquals(ChatRoomRoleEnum.MEMBER.getCode(), existingMember.getRole());
        Assertions.assertNull(updatedMember);
    }

    @Test
    void shouldKeepExistingMemberRoleWhenDuplicateAddAttemptsOwnerRole() {
        existingMember = buildMember(ChatRoomRoleEnum.MEMBER.getCode());

        chatRoomMemberService.addMember(10L, 2L, ChatRoomRoleEnum.OWNER.getCode());

        Assertions.assertEquals(ChatRoomRoleEnum.MEMBER.getCode(), existingMember.getRole());
        Assertions.assertNull(updatedMember);
    }

    @Test
    void shouldRejectInvalidAddMemberParams() {
        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatRoomMemberService.addMember(null, 2L, ChatRoomRoleEnum.MEMBER.getCode()));

        Assertions.assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
    }

    @Test
    void shouldRejectNullUserIdWhenAddingMember() {
        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatRoomMemberService.addMember(10L, null, ChatRoomRoleEnum.MEMBER.getCode()));

        Assertions.assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
    }

    @Test
    void shouldLoadRoomMembersFromDatabaseWhenCacheIsCold() {
        TestableCacheRecoveryChatRoomMemberServiceImpl service = new TestableCacheRecoveryChatRoomMemberServiceImpl();
        FakeCacheUtils cacheUtils = new FakeCacheUtils();
        ReflectionTestUtils.setField(service, "cacheUtils", cacheUtils);
        service.listResult = List.of(buildMember(ChatRoomRoleEnum.MEMBER.getCode()));

        Assertions.assertTrue(service.isMember(10L, 2L));
        Assertions.assertTrue(cacheUtils.exists(ChatCacheConstant.getRoomMemberKey(10L)));
        Assertions.assertTrue(cacheUtils.sIsMember(ChatCacheConstant.getRoomMemberKey(10L), "2"));
    }

    @Test
    void shouldRemoveFromCacheWhenMemberLeavesRoom() {
        TestableCacheRecoveryChatRoomMemberServiceImpl service = new TestableCacheRecoveryChatRoomMemberServiceImpl();
        FakeCacheUtils cacheUtils = new FakeCacheUtils();
        ReflectionTestUtils.setField(service, "cacheUtils", cacheUtils);

        // Pre-populate cache with a member
        String key = ChatCacheConstant.getRoomMemberKey(10L);
        cacheUtils.sAdd(key, "2");
        Assertions.assertTrue(cacheUtils.sIsMember(key, "2"));

        service.leaveRoom(10L, 2L);

        Assertions.assertFalse(cacheUtils.sIsMember(key, "2"));
    }

    @Test
    void shouldReportNonMemberAfterLeave() {
        TestableCacheRecoveryChatRoomMemberServiceImpl service = new TestableCacheRecoveryChatRoomMemberServiceImpl();
        FakeCacheUtils cacheUtils = new FakeCacheUtils();
        ReflectionTestUtils.setField(service, "cacheUtils", cacheUtils);

        // Pre-populate cache with a member
        String key = ChatCacheConstant.getRoomMemberKey(10L);
        cacheUtils.sAdd(key, "2");
        Assertions.assertTrue(service.isMember(10L, 2L));

        service.leaveRoom(10L, 2L);

        Assertions.assertFalse(service.isMember(10L, 2L));
    }

    @Test
    void shouldNotCreateDuplicateMemberOnRepeatedAdd() {
        TestableCacheRecoveryChatRoomMemberServiceImpl service = new TestableCacheRecoveryChatRoomMemberServiceImpl();
        FakeCacheUtils cacheUtils = new FakeCacheUtils();
        ReflectionTestUtils.setField(service, "cacheUtils", cacheUtils);

        // First add: member not in DB
        service.listResult = List.of();
        service.savedMembers.clear();
        service.addMember(10L, 2L, ChatRoomRoleEnum.MEMBER.getCode());

        Assertions.assertEquals(1, service.savedMembers.size());

        // Second add: member now in DB (getMember returns existing)
        ChatRoomMember existing = buildMember(ChatRoomRoleEnum.MEMBER.getCode());
        service.existingMember = existing;
        service.savedMembers.clear();
        service.addMember(10L, 2L, ChatRoomRoleEnum.MEMBER.getCode());

        Assertions.assertTrue(service.savedMembers.isEmpty(), "duplicate add should not save again");
    }

    private ChatRoomMember buildMember(Integer role) {
        ChatRoomMember member = new ChatRoomMember();
        member.setId(1L);
        member.setRoomId(10L);
        member.setUserId(2L);
        member.setRole(role);
        return member;
    }

    private class TestableChatRoomMemberServiceImpl extends ChatRoomMemberServiceImpl {
        @Override
        public ChatRoomMember getMember(Long roomId, Long userId) {
            return existingMember;
        }

        @Override
        public boolean updateById(ChatRoomMember entity) {
            updatedMember = entity;
            return true;
        }
    }

    private static class TestableCacheRecoveryChatRoomMemberServiceImpl extends ChatRoomMemberServiceImpl {
        private List<ChatRoomMember> listResult = List.of();
        private final List<ChatRoomMember> savedMembers = new ArrayList<>();
        private ChatRoomMember existingMember;

        @Override
        public List<ChatRoomMember> list(Wrapper<ChatRoomMember> queryWrapper) {
            return listResult;
        }

        @Override
        public ChatRoomMember getMember(Long roomId, Long userId) {
            return existingMember;
        }

        @Override
        public boolean save(ChatRoomMember entity) {
            savedMembers.add(entity);
            return true;
        }

        @Override
        public boolean remove(Wrapper<ChatRoomMember> queryWrapper) {
            return true;
        }
    }

    private static class FakeCacheUtils extends CacheUtils {
        private final Map<String, Set<String>> setMap = new HashMap<>();

        FakeCacheUtils() {
            ReflectionTestUtils.setField(this, "redissonClient", createRedissonClient());
        }

        private RedissonClient createRedissonClient() {
            return (RedissonClient) Proxy.newProxyInstance(
                    RedissonClient.class.getClassLoader(),
                    new Class[]{RedissonClient.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getBucket" -> createBucketProxy((String) args[0]);
                        case "getSet" -> createSetProxy((String) args[0]);
                        default -> defaultValue(method.getReturnType());
                    });
        }

        private Object createBucketProxy(String key) {
            return Proxy.newProxyInstance(
                    RBucket.class.getClassLoader(),
                    new Class[]{RBucket.class},
                    (bucketProxy, bucketMethod, bucketArgs) -> switch (bucketMethod.getName()) {
                        case "isExists" -> setMap.containsKey(key);
                        case "expire" -> true;
                        default -> defaultValue(bucketMethod.getReturnType());
                    });
        }

        private Object createSetProxy(String key) {
            return Proxy.newProxyInstance(
                    RSet.class.getClassLoader(),
                    new Class[]{RSet.class},
                    (setProxy, setMethod, setArgs) -> switch (setMethod.getName()) {
                        case "addAll" -> {
                            Set<String> members = setMap.computeIfAbsent(key, ignored -> new HashSet<>());
                            members.addAll((Collection<String>) setArgs[0]);
                            yield true;
                        }
                        case "contains" -> setMap.getOrDefault(key, Set.of()).contains(setArgs[0]);
                        case "readAll" -> new HashSet<>(setMap.getOrDefault(key, Set.of()));
                        case "removeAll" -> {
                            Set<String> members = setMap.getOrDefault(key, Set.of());
                            yield members.removeAll((Collection<?>) setArgs[0]);
                        }
                        default -> defaultValue(setMethod.getReturnType());
                    });
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
}
