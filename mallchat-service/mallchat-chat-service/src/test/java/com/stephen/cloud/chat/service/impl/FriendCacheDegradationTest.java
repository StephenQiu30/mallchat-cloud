package com.stephen.cloud.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.stephen.cloud.chat.mapper.UserFriendBlockMapper;
import com.stephen.cloud.chat.model.entity.UserFriend;
import com.stephen.cloud.chat.model.entity.UserFriendBlock;
import com.stephen.cloud.common.cache.constants.ChatCacheConstant;
import com.stephen.cloud.common.cache.utils.CacheUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RBucket;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.*;

/**
 * 好友关系缓存退化回源测试
 *
 * 验证 Redis 缓存缺失时，Service 层能够正确回源数据库，
 * 保证好友关系判断在冷缓存场景下仍然正确。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FriendCacheDegradationTest {

    private ActualCacheRecoveryUserFriendServiceImpl userFriendService;
    private FakeCacheUtils cacheUtils;

    @Mock
    private UserFriendBlockMapper userFriendBlockMapper;

    @BeforeEach
    void setUp() {
        userFriendService = new ActualCacheRecoveryUserFriendServiceImpl();
        cacheUtils = new FakeCacheUtils();
        ReflectionTestUtils.setField(userFriendService, "cacheUtils", cacheUtils);
        ReflectionTestUtils.setField(userFriendService, "userFriendBlockMapper", userFriendBlockMapper);
    }

    @Test
    @DisplayName("RED: 缓存 miss 时 isMutualFriend 应回源数据库并返回正确结果")
    void shouldReturnCorrectResultWhenCacheMiss() {
        // Given: 数据库中有好友关系，但缓存为空
        userFriendService.setListResult(List.of(createFriend(1L, 2L)));

        // When: 调用 isMutualFriend
        boolean result = userFriendService.isMutualFriend(1L, 2L);

        // Then: 应返回 true（正确识别好友关系）
        Assertions.assertTrue(result, "缓存 miss 时应回源数据库，正确返回好友关系");
    }

    @Test
    @DisplayName("RED: 缓存 miss 时 listMutualFriendIds 应回源数据库并返回正确列表")
    void shouldReturnCorrectFriendIdsWhenCacheMiss() {
        // Given: 数据库中有多个好友，但缓存为空
        userFriendService.setListResult(List.of(
                createFriend(1L, 2L),
                createFriend(1L, 3L),
                createFriend(1L, 4L)
        ));

        // When: 调用 listMutualFriendIds
        Set<Long> friendIds = userFriendService.listMutualFriendIds(1L);

        // Then: 应返回所有好友 ID
        Assertions.assertEquals(3, friendIds.size(), "缓存 miss 时应回源数据库返回正确的好友数量");
        Assertions.assertTrue(friendIds.contains(2L), "应包含好友 2");
        Assertions.assertTrue(friendIds.contains(3L), "应包含好友 3");
        Assertions.assertTrue(friendIds.contains(4L), "应包含好友 4");
    }

    @Test
    @DisplayName("RED: 缓存 miss 回源后应回填缓存")
    void shouldPopulateCacheAfterDatabaseFallback() {
        // Given: 数据库中有好友关系，但缓存为空
        userFriendService.setListResult(List.of(createFriend(1L, 2L)));

        // When: 调用 isMutualFriend 触发回源
        userFriendService.isMutualFriend(1L, 2L);

        // Then: 缓存应被回填
        String cacheKey = ChatCacheConstant.getUserFriendKey(1L);
        Assertions.assertTrue(cacheUtils.exists(cacheKey), "回源后缓存应被回填");
        Assertions.assertTrue(cacheUtils.sIsMember(cacheKey, "2"), "缓存应包含好友 ID");
    }

    @Test
    @DisplayName("GREEN: 缓存 hit 时应直接使用缓存，不查询数据库")
    void shouldUseCacheWhenAvailable() {
        // Given: 缓存中存在好友关系
        String cacheKey = ChatCacheConstant.getUserFriendKey(1L);
        cacheUtils.sAdd(cacheKey, "2");

        // When: 调用 isMutualFriend
        boolean result = userFriendService.isMutualFriend(1L, 2L);

        // Then: 应返回 true，且不触发数据库查询
        Assertions.assertTrue(result, "缓存 hit 时应正确返回好友关系");
    }

    @Test
    @DisplayName("GREEN: 缓存 miss 时非好友应返回 false")
    void shouldReturnFalseForNonFriendWhenCacheMiss() {
        // Given: 数据库中无好友关系，缓存为空
        userFriendService.setListResult(List.of());

        // When: 调用 isMutualFriend
        boolean result = userFriendService.isMutualFriend(1L, 999L);

        // Then: 应返回 false
        Assertions.assertFalse(result, "非好友应返回 false");
    }

    @Test
    @DisplayName("GREEN: 缓存 miss 时应排除拉黑用户")
    void shouldExcludeBlockedUsersWhenCacheMiss() {
        // Given: 用户 1 有好友 2 和 3，但拉黑了 2
        userFriendService.setListResult(List.of(
                createFriend(1L, 2L),
                createFriend(1L, 3L)
        ));
        org.mockito.Mockito.when(userFriendBlockMapper.selectOne(
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(new UserFriendBlock())  // 1 -> 2 被拉黑
                .thenReturn(null);                   // 2 -> 1 未拉黑

        // When: 调用 listMutualFriendIds
        Set<Long> friendIds = userFriendService.listMutualFriendIds(1L);

        // Then: 应排除拉黑用户 2
        Assertions.assertEquals(1, friendIds.size(), "拉黑用户应被排除");
        Assertions.assertTrue(friendIds.contains(3L), "应包含未被拉黑的好友 3");
        Assertions.assertFalse(friendIds.contains(2L), "不应包含被拉黑的好友 2");
    }

    @Test
    @DisplayName("GREEN: 空好友列表应设置空占位符防止缓存穿透")
    void shouldSetEmptyPlaceholderWhenNoFriends() {
        // Given: 用户无好友，缓存为空
        userFriendService.setListResult(List.of());

        // When: 调用 listMutualFriendIds
        Set<Long> friendIds = userFriendService.listMutualFriendIds(1L);

        // Then: 应返回空集合，且缓存应存在（带空占位符）
        Assertions.assertTrue(friendIds.isEmpty(), "无好友时应返回空集合");
        String cacheKey = ChatCacheConstant.getUserFriendKey(1L);
        Assertions.assertTrue(cacheUtils.exists(cacheKey), "空好友列表也应设置缓存防止穿透");
    }

    private UserFriend createFriend(Long userId, Long friendUserId) {
        UserFriend friend = new UserFriend();
        friend.setUserId(userId);
        friend.setFriendUserId(friendUserId);
        return friend;
    }

    /**
     * 可测试的 UserFriendServiceImpl 子类，仅覆盖 list() 方法模拟 DB 查询
     */
    private static class ActualCacheRecoveryUserFriendServiceImpl extends UserFriendServiceImpl {
        private List<UserFriend> listResult = new ArrayList<>();

        void setListResult(List<UserFriend> result) {
            this.listResult = new ArrayList<>(result);
        }

        @Override
        public List<UserFriend> list(Wrapper<UserFriend> queryWrapper) {
            return new ArrayList<>(listResult);
        }
    }

    /**
     * 模拟 CacheUtils：通过注入假 RedissonClient 代理来拦截 Redis 操作
     * 复用 UserFriendServiceImplTest 中的 FakeCacheUtils 模式
     */
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
                        case "delete" -> {
                            setMap.remove(key);
                            yield true;
                        }
                        default -> defaultValue(bucketMethod.getReturnType());
                    });
        }

        @SuppressWarnings("unchecked")
        private Object createSetProxy(String key) {
            return Proxy.newProxyInstance(
                    RSet.class.getClassLoader(),
                    new Class[]{RSet.class},
                    (setProxy, setMethod, setArgs) -> switch (setMethod.getName()) {
                        case "add" -> {
                            Set<String> members = setMap.computeIfAbsent(key, ignored -> new HashSet<>());
                            members.add((String) setArgs[0]);
                            yield true;
                        }
                        case "addAll" -> {
                            Set<String> members = setMap.computeIfAbsent(key, ignored -> new HashSet<>());
                            members.addAll((Collection<String>) setArgs[0]);
                            yield true;
                        }
                        case "contains" -> setMap.getOrDefault(key, Set.of()).contains(setArgs[0]);
                        case "readAll" -> new HashSet<>(setMap.getOrDefault(key, Set.of()));
                        case "removeAll" -> {
                            Set<String> members = setMap.computeIfAbsent(key, ignored -> new HashSet<>());
                            for (Object value : (Collection<?>) setArgs[0]) {
                                members.remove((String) value);
                            }
                            yield true;
                        }
                        case "remove" -> {
                            Set<String> members = setMap.computeIfAbsent(key, ignored -> new HashSet<>());
                            members.remove((String) setArgs[0]);
                            yield true;
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
