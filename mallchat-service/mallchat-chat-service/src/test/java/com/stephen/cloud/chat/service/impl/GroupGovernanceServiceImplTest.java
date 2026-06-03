package com.stephen.cloud.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.stephen.cloud.api.chat.model.enums.ChatRoomRoleEnum;
import com.stephen.cloud.chat.model.entity.ChatRoom;
import com.stephen.cloud.chat.model.entity.ChatRoomMember;
import com.stephen.cloud.chat.service.ChatRoomMemberService;
import com.stephen.cloud.chat.service.ChatRoomService;
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

/**
 * 群治理服务测试
 * 覆盖：成员变更审计、缓存一致性检查、群规模限制、治理指标
 */
class GroupGovernanceServiceImplTest {

    private GroupGovernanceServiceImpl governanceService;
    private FakeCacheUtils cacheUtils;
    private Long savedRoomId;
    private Long savedUserId;
    private String savedAction;
    private Long savedOperatorId;
    private List<ChatRoomMember> dbMembers;
    private ChatRoom stubRoom;
    private Integer roomMemberCount;

    @BeforeEach
    void setUp() {
        governanceService = new GroupGovernanceServiceImpl();
        cacheUtils = new FakeCacheUtils();
        ReflectionTestUtils.setField(governanceService, "cacheUtils", cacheUtils);
        ReflectionTestUtils.setField(governanceService, "chatRoomService", createChatRoomService());
        ReflectionTestUtils.setField(governanceService, "chatRoomMemberService", createChatRoomMemberService());
        dbMembers = new ArrayList<>();
        roomMemberCount = 0;
    }

    // ==================== 成员变更审计测试 ====================

    @Test
    void shouldRecordAuditWhenMemberJoins() {
        governanceService.recordAudit(10L, 2L, "JOIN", 1L);

        Assertions.assertEquals(10L, savedRoomId);
        Assertions.assertEquals(2L, savedUserId);
        Assertions.assertEquals("JOIN", savedAction);
        Assertions.assertEquals(1L, savedOperatorId);
    }

    @Test
    void shouldRecordAuditWhenMemberLeaves() {
        governanceService.recordAudit(10L, 2L, "LEAVE", 2L);

        Assertions.assertEquals("LEAVE", savedAction);
    }

    @Test
    void shouldRecordAuditWhenMemberKicked() {
        governanceService.recordAudit(10L, 2L, "KICK", 3L);

        Assertions.assertEquals("KICK", savedAction);
        Assertions.assertEquals(3L, savedOperatorId);
    }

    @Test
    void shouldRejectAuditWithNullRoomId() {
        Assertions.assertThrows(BusinessException.class,
                () -> governanceService.recordAudit(null, 2L, "JOIN", 1L));
    }

    @Test
    void shouldRejectAuditWithNullUserId() {
        Assertions.assertThrows(BusinessException.class,
                () -> governanceService.recordAudit(10L, null, "JOIN", 1L));
    }

    @Test
    void shouldRejectAuditWithBlankAction() {
        Assertions.assertThrows(BusinessException.class,
                () -> governanceService.recordAudit(10L, 2L, "", 1L));
    }

    // ==================== 缓存一致性检查测试 ====================

    @Test
    void shouldReturnEmptyDriftWhenCacheAndDbMatch() {
        dbMembers.add(buildMember(10L, 1L));
        dbMembers.add(buildMember(10L, 2L));
        cacheUtils.sAdd(ChatCacheConstant.getRoomMemberKey(10L), "1");
        cacheUtils.sAdd(ChatCacheConstant.getRoomMemberKey(10L), "2");

        GroupGovernanceServiceImpl.CacheConsistencyResult result =
                governanceService.checkCacheConsistency(10L);

        Assertions.assertTrue(result.getMissingInCache().isEmpty());
        Assertions.assertTrue(result.getMissingInDb().isEmpty());
        Assertions.assertFalse(result.isDrifted());
    }

    @Test
    void shouldDetectMembersInDbButNotInCache() {
        dbMembers.add(buildMember(10L, 1L));
        dbMembers.add(buildMember(10L, 2L));
        dbMembers.add(buildMember(10L, 3L));
        // cache only has 1 and 2, missing 3
        cacheUtils.sAdd(ChatCacheConstant.getRoomMemberKey(10L), "1");
        cacheUtils.sAdd(ChatCacheConstant.getRoomMemberKey(10L), "2");

        GroupGovernanceServiceImpl.CacheConsistencyResult result =
                governanceService.checkCacheConsistency(10L);

        Assertions.assertTrue(result.isDrifted());
        Assertions.assertTrue(result.getMissingInCache().contains(3L));
        Assertions.assertTrue(result.getMissingInDb().isEmpty());
    }

    @Test
    void shouldDetectMembersInCacheButNotInDb() {
        dbMembers.add(buildMember(10L, 1L));
        // cache has 1 and 2, but 2 is not in DB
        cacheUtils.sAdd(ChatCacheConstant.getRoomMemberKey(10L), "1");
        cacheUtils.sAdd(ChatCacheConstant.getRoomMemberKey(10L), "2");

        GroupGovernanceServiceImpl.CacheConsistencyResult result =
                governanceService.checkCacheConsistency(10L);

        Assertions.assertTrue(result.isDrifted());
        Assertions.assertTrue(result.getMissingInCache().isEmpty());
        Assertions.assertTrue(result.getMissingInDb().contains(2L));
    }

    @Test
    void shouldDetectBidirectionalDrift() {
        dbMembers.add(buildMember(10L, 1L));
        dbMembers.add(buildMember(10L, 3L));
        // cache has 1 and 2, but DB has 1 and 3
        cacheUtils.sAdd(ChatCacheConstant.getRoomMemberKey(10L), "1");
        cacheUtils.sAdd(ChatCacheConstant.getRoomMemberKey(10L), "2");

        GroupGovernanceServiceImpl.CacheConsistencyResult result =
                governanceService.checkCacheConsistency(10L);

        Assertions.assertTrue(result.isDrifted());
        Assertions.assertTrue(result.getMissingInCache().contains(3L));
        Assertions.assertTrue(result.getMissingInDb().contains(2L));
    }

    @Test
    void shouldReportNoDriftWhenBothEmpty() {
        GroupGovernanceServiceImpl.CacheConsistencyResult result =
                governanceService.checkCacheConsistency(10L);

        Assertions.assertFalse(result.isDrifted());
        Assertions.assertTrue(result.getMissingInCache().isEmpty());
        Assertions.assertTrue(result.getMissingInDb().isEmpty());
    }

    // ==================== 群规模限制测试 ====================

    @Test
    void shouldAllowAddWhenUnderMaxMembers() {
        stubRoom = buildRoom(10L, 100);
        roomMemberCount = 50;

        Assertions.assertDoesNotThrow(() -> governanceService.enforceMaxMembers(10L));
    }

    @Test
    void shouldRejectAddWhenAtMaxMembers() {
        stubRoom = buildRoom(10L, 100);
        roomMemberCount = 100;

        BusinessException ex = Assertions.assertThrows(BusinessException.class,
                () -> governanceService.enforceMaxMembers(10L));
        Assertions.assertEquals(ErrorCode.OPERATION_ERROR.getCode(), ex.getCode());
    }

    @Test
    void shouldRejectAddWhenOverMaxMembers() {
        stubRoom = buildRoom(10L, 50);
        roomMemberCount = 51;

        BusinessException ex = Assertions.assertThrows(BusinessException.class,
                () -> governanceService.enforceMaxMembers(10L));
        Assertions.assertEquals(ErrorCode.OPERATION_ERROR.getCode(), ex.getCode());
    }

    @Test
    void shouldAllowAddWhenNoMaxSet() {
        stubRoom = buildRoom(10L, null);
        roomMemberCount = 9999;

        Assertions.assertDoesNotThrow(() -> governanceService.enforceMaxMembers(10L));
    }

    @Test
    void shouldAllowAddWhenMaxIsZero() {
        stubRoom = buildRoom(10L, 0);
        roomMemberCount = 9999;

        Assertions.assertDoesNotThrow(() -> governanceService.enforceMaxMembers(10L));
    }

    @Test
    void shouldAllowExactBoundaryWhenAtMaxMinusOne() {
        stubRoom = buildRoom(10L, 100);
        roomMemberCount = 99;

        Assertions.assertDoesNotThrow(() -> governanceService.enforceMaxMembers(10L));
    }

    // ==================== 治理指标测试 ====================

    @Test
    void shouldReturnGovernanceMetricsForMemberRoom() {
        stubRoom = buildRoom(10L, 200);
        roomMemberCount = 50;
        dbMembers.add(buildMember(10L, 1L));
        dbMembers.add(buildMember(10L, 2L));
        dbMembers.add(buildMember(10L, 3L));
        // set roles
        dbMembers.get(0).setRole(ChatRoomRoleEnum.OWNER.getCode());
        dbMembers.get(1).setRole(ChatRoomRoleEnum.ADMIN.getCode());
        dbMembers.get(2).setRole(ChatRoomRoleEnum.MEMBER.getCode());

        GroupGovernanceServiceImpl.GovernanceMetrics metrics =
                governanceService.getGovernanceMetrics(10L);

        Assertions.assertEquals(10L, metrics.getRoomId());
        Assertions.assertEquals(3, metrics.getTotalMembers());
        Assertions.assertEquals(1, metrics.getOwnerCount());
        Assertions.assertEquals(1, metrics.getAdminCount());
        Assertions.assertEquals(1, metrics.getMemberCount());
        Assertions.assertEquals(200, metrics.getMaxMembers());
        Assertions.assertEquals(50, metrics.getCurrentMemberCount());
    }

    @Test
    void shouldReturnZeroMetricsForEmptyRoom() {
        stubRoom = buildRoom(10L, 100);
        roomMemberCount = 0;

        GroupGovernanceServiceImpl.GovernanceMetrics metrics =
                governanceService.getGovernanceMetrics(10L);

        Assertions.assertEquals(0, metrics.getTotalMembers());
        Assertions.assertEquals(0, metrics.getOwnerCount());
        Assertions.assertEquals(0, metrics.getAdminCount());
        Assertions.assertEquals(0, metrics.getMemberCount());
    }

    @Test
    void shouldRejectMetricsForNullRoom() {
        stubRoom = null;

        Assertions.assertThrows(BusinessException.class,
                () -> governanceService.getGovernanceMetrics(999L));
    }

    // ==================== 辅助方法 ====================

    private ChatRoomMember buildMember(Long roomId, Long userId) {
        ChatRoomMember member = new ChatRoomMember();
        member.setId(userId);
        member.setRoomId(roomId);
        member.setUserId(userId);
        member.setRole(ChatRoomRoleEnum.MEMBER.getCode());
        return member;
    }

    private ChatRoom buildRoom(Long roomId, Integer maxMembers) {
        ChatRoom room = new ChatRoom();
        room.setId(roomId);
        room.setName("测试群");
        room.setType(1);
        room.setCreateUser(1L);
        room.setMaxMembers(maxMembers);
        return room;
    }

    private ChatRoomService createChatRoomService() {
        return (ChatRoomService) Proxy.newProxyInstance(
                ChatRoomService.class.getClassLoader(),
                new Class[]{ChatRoomService.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getById" -> stubRoom;
                    default -> defaultValue(method.getReturnType());
                });
    }

    private ChatRoomMemberService createChatRoomMemberService() {
        return (ChatRoomMemberService) Proxy.newProxyInstance(
                ChatRoomMemberService.class.getClassLoader(),
                new Class[]{ChatRoomMemberService.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "listByRoomId" -> dbMembers;
                    case "countByRoomId" -> (long) roomMemberCount;
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) return false;
        if (returnType == int.class) return 0;
        if (returnType == long.class) return 0L;
        if (returnType == void.class) return null;
        return null;
    }

    /**
     * 内部可测试子类：重写审计持久化方法以捕获参数
     */
    private class GroupGovernanceServiceImpl extends com.stephen.cloud.chat.service.impl.GroupGovernanceServiceImpl {
        @Override
        protected void saveAuditRecord(Long roomId, Long userId, String action, Long operatorId) {
            savedRoomId = roomId;
            savedUserId = userId;
            savedAction = action;
            savedOperatorId = operatorId;
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
                        default -> defaultValue(setMethod.getReturnType());
                    });
        }

        private static Object defaultValue(Class<?> returnType) {
            if (returnType == boolean.class) return false;
            if (returnType == int.class) return 0;
            if (returnType == long.class) return 0L;
            return null;
        }
    }
}
