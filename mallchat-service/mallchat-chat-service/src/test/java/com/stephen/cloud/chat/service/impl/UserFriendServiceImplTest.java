package com.stephen.cloud.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.stephen.cloud.chat.model.entity.UserFriend;
import com.stephen.cloud.common.cache.constants.ChatCacheConstant;
import com.stephen.cloud.common.cache.utils.CacheUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class UserFriendServiceImplTest {

    private TestableUserFriendServiceImpl userFriendService;
    private FakeCacheUtils cacheUtils;

    @BeforeEach
    void setUp() {
        userFriendService = new TestableUserFriendServiceImpl();
        cacheUtils = new FakeCacheUtils();
        ReflectionTestUtils.setField(userFriendService, "cacheUtils", cacheUtils);
    }

    @Test
    void shouldLoadFriendIdsFromDatabaseWhenCacheIsCold() {
        userFriendService.listResult = List.of(createFriend(1L, 2L), createFriend(1L, 3L));

        Set<Long> friendIds = userFriendService.listFriendIdsForNotification(1L);

        Assertions.assertEquals(new LinkedHashSet<>(Set.of(2L, 3L)), friendIds);
        Assertions.assertTrue(cacheUtils.exists(ChatCacheConstant.getUserFriendKey(1L)));
    }

    @Test
    void shouldReturnEmptySetForUserWithoutFriends() {
        userFriendService.listResult = List.of();

        Set<Long> friendIds = userFriendService.listFriendIdsForNotification(1L);

        Assertions.assertTrue(friendIds.isEmpty());
        Assertions.assertTrue(cacheUtils.exists(ChatCacheConstant.getUserFriendKey(1L)));
    }

    private UserFriend createFriend(Long userId, Long friendUserId) {
        UserFriend friend = new UserFriend();
        friend.setUserId(userId);
        friend.setFriendUserId(friendUserId);
        return friend;
    }

    private static class TestableUserFriendServiceImpl extends UserFriendServiceImpl {
        private List<UserFriend> listResult = new ArrayList<>();

        @Override
        public List<UserFriend> list(Wrapper<UserFriend> queryWrapper) {
            return new ArrayList<>(listResult);
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
                        case "getBucket" -> Proxy.newProxyInstance(
                                method.getReturnType().getClassLoader(),
                                new Class[]{method.getReturnType()},
                                (bucketProxy, bucketMethod, bucketArgs) -> switch (bucketMethod.getName()) {
                                    case "isExists" -> setMap.containsKey((String) args[0]);
                                    case "expire" -> true;
                                    case "delete" -> {
                                        setMap.remove((String) args[0]);
                                        yield true;
                                    }
                                    default -> defaultValue(bucketMethod.getReturnType());
                                }
                        );
                        case "getSet" -> Proxy.newProxyInstance(
                                method.getReturnType().getClassLoader(),
                                new Class[]{method.getReturnType()},
                                (setProxy, setMethod, setArgs) -> switch (setMethod.getName()) {
                                    case "add" -> {
                                        Set<String> members = setMap.computeIfAbsent((String) args[0], ignored -> new HashSet<>());
                                        members.add((String) setArgs[0]);
                                        yield true;
                                    }
                                    case "addAll" -> {
                                        Set<String> members = setMap.computeIfAbsent((String) args[0], ignored -> new HashSet<>());
                                        members.addAll((java.util.Collection<String>) setArgs[0]);
                                        yield true;
                                    }
                                    case "contains" -> setMap.getOrDefault((String) args[0], Set.of()).contains(setArgs[0]);
                                    case "readAll" -> new HashSet<>(setMap.getOrDefault((String) args[0], Set.of()));
                                    default -> defaultValue(setMethod.getReturnType());
                                }
                        );
                        default -> defaultValue(method.getReturnType());
                    }
            );
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
}
