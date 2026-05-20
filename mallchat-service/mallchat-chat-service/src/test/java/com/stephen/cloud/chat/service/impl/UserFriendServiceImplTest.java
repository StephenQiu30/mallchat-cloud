package com.stephen.cloud.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.chat.model.vo.ChatFriendUserVO;
import com.stephen.cloud.api.user.client.UserFeignClient;
import com.stephen.cloud.api.user.model.dto.UserQueryRequest;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.chat.service.ChatOnlineStatusService;
import com.stephen.cloud.chat.model.entity.UserFriend;
import com.stephen.cloud.common.cache.constants.ChatCacheConstant;
import com.stephen.cloud.common.cache.utils.CacheUtils;
import com.stephen.cloud.common.common.ErrorCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RSet;
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
    private UserVO friend;
    private UserVO stranger;

    @BeforeEach
    void setUp() {
        userFriendService = new TestableUserFriendServiceImpl();
        cacheUtils = new FakeCacheUtils();
        ReflectionTestUtils.setField(userFriendService, "cacheUtils", cacheUtils);

        friend = new UserVO();
        friend.setId(2L);
        friend.setUserName("friend");
        friend.setUserAvatar("friend-avatar");

        stranger = new UserVO();
        stranger.setId(3L);
        stranger.setUserName("stranger");
        stranger.setUserAvatar("stranger-avatar");

        ReflectionTestUtils.setField(userFriendService, "userFeignClient", createUserFeignClient());
        ReflectionTestUtils.setField(userFriendService, "chatOnlineStatusService", createChatOnlineStatusService());
    }

    @Test
    void shouldLoadFriendIdsFromDatabaseWhenCacheIsCold() {
        userFriendService.listResult = List.of(createFriend(1L, 2L), createFriend(1L, 3L));

        Set<Long> friendIds = userFriendService.listFriendIdsForNotification(1L);

        Assertions.assertEquals(new LinkedHashSet<>(Set.of(2L, 3L)), friendIds);
        Assertions.assertTrue(cacheUtils.exists(ChatCacheConstant.getUserFriendKey(1L)));
    }

    @Test
    void shouldCheckMutualFriendFromDatabaseWhenCacheIsCold() {
        ActualCacheRecoveryUserFriendServiceImpl service = new ActualCacheRecoveryUserFriendServiceImpl();
        FakeCacheUtils localCacheUtils = new FakeCacheUtils();
        ReflectionTestUtils.setField(service, "cacheUtils", localCacheUtils);
        service.listResult = List.of(createFriend(1L, 2L));

        Assertions.assertTrue(service.isMutualFriend(1L, 2L));
        Assertions.assertTrue(localCacheUtils.exists(ChatCacheConstant.getUserFriendKey(1L)));
        Assertions.assertTrue(localCacheUtils.sIsMember(ChatCacheConstant.getUserFriendKey(1L), "2"));
    }

    @Test
    void shouldReturnEmptySetForUserWithoutFriends() {
        userFriendService.listResult = List.of();

        Set<Long> friendIds = userFriendService.listFriendIdsForNotification(1L);

        Assertions.assertTrue(friendIds.isEmpty());
        Assertions.assertTrue(cacheUtils.exists(ChatCacheConstant.getUserFriendKey(1L)));
    }

    @Test
    void shouldReturnSelfFriendshipStatus() {
        Integer status = userFriendService.getFriendshipStatus(1L, 1L);

        Assertions.assertEquals(1, status);
    }

    @Test
    void shouldRejectFriendshipStatusWhenUserMissing() {
        Assertions.assertThrows(RuntimeException.class, () -> userFriendService.getFriendshipStatus(null, 2L));
        Assertions.assertThrows(RuntimeException.class, () -> userFriendService.getFriendshipStatus(1L, null));
    }

    @Test
    void shouldReturnFriendshipStatusByMutualFriendRelation() {
        userFriendService.setMutualFriend(1L, 2L, true);

        Integer status = userFriendService.getFriendshipStatus(1L, 2L);

        Assertions.assertEquals(2, status);
    }

    @Test
    void shouldReturnPendingFriendshipStatusByDirection() {
        userFriendService.setPendingFriendApply(1L, 2L, true);

        Integer statusFromMe = userFriendService.getFriendshipStatus(1L, 2L);
        Assertions.assertEquals(3, statusFromMe);

        userFriendService.setPendingFriendApply(1L, 2L, false);
        userFriendService.setPendingFriendApply(2L, 1L, true);
        Integer statusToMe = userFriendService.getFriendshipStatus(1L, 2L);
        Assertions.assertEquals(4, statusToMe);
    }

    @Test
    void shouldReturnStrangerFriendshipStatusWhenNoRelation() {
        Integer status = userFriendService.getFriendshipStatus(1L, 2L);

        Assertions.assertEquals(0, status);
    }

    @Test
    void shouldSearchFriendsWithStatusAndExcludeSelfInRequest() {
        userFriendService.setSearchUsers(buildSearchUsersPage(12L, List.of(friend, stranger)));
        userFriendService.setPendingFriendApply(1L, 2L, true);
        userFriendService.setOnlineStatusMap(Map.of(2L, 1, 3L, 0));

        Page<ChatFriendUserVO> result = userFriendService.searchFriends(1L, "abc", 1, 10);

        UserQueryRequest request = userFriendService.getCapturedSearchRequest();
        Assertions.assertEquals(1, request.getCurrent());
        Assertions.assertEquals(10, request.getPageSize());
        Assertions.assertEquals("abc", request.getSearchText());
        Assertions.assertEquals(1L, request.getNotId());

        Assertions.assertEquals(2, result.getRecords().size());
        Assertions.assertEquals(12L, result.getTotal());
        Assertions.assertEquals(2L, result.getRecords().get(0).getId());
        Assertions.assertEquals(3L, result.getRecords().get(1).getId());
        Assertions.assertEquals(3, result.getRecords().get(0).getFriendStatus());
        Assertions.assertEquals(0, result.getRecords().get(1).getFriendStatus());
        Assertions.assertEquals(1, result.getRecords().get(0).getOnlineStatus());
        Assertions.assertEquals(0, result.getRecords().get(1).getOnlineStatus());
    }

    @Test
    void shouldRejectSearchFriendsWhenCurrentUserMissing() {
        Assertions.assertThrows(RuntimeException.class,
                () -> userFriendService.searchFriends(null, "abc", 1, 10));
    }

    @Test
    void shouldKeepRemoveFriendshipIdempotentWhenRelationshipMissing() {
        userFriendService.setRemoveResult(false);

        userFriendService.removeFriend(1L, 2L);
        userFriendService.removeFriend(1L, 2L);

        Assertions.assertEquals(2, userFriendService.removeInvocationCount);
    }

    @Test
    void shouldRejectRemoveSelfFriendship() {
        Assertions.assertThrows(RuntimeException.class, () -> userFriendService.removeFriend(1L, 1L));
        Assertions.assertEquals(0, userFriendService.removeInvocationCount);
    }

    @Test
    void shouldClearFriendshipCacheForBothUsersWhenRemoveSucceeds() {
        userFriendService.setRemoveResult(true);
        cacheUtils.sAdd(ChatCacheConstant.getUserFriendKey(1L), "2");
        cacheUtils.sAdd(ChatCacheConstant.getUserFriendKey(2L), "1");

        userFriendService.removeFriend(1L, 2L);

        Assertions.assertEquals(1, cacheUtils.removeCallCount.getOrDefault(ChatCacheConstant.getUserFriendKey(1L) + ":2", 0));
        Assertions.assertEquals(1, cacheUtils.removeCallCount.getOrDefault(ChatCacheConstant.getUserFriendKey(2L) + ":1", 0));
    }

    @Test
    void shouldRejectSearchFriendsWithInvalidPageSize() {
        Assertions.assertThrows(RuntimeException.class,
                () -> userFriendService.searchFriends(1L, "abc", 1, 0));
        Assertions.assertThrows(RuntimeException.class,
                () -> userFriendService.searchFriends(1L, "abc", 1, 21));
    }

    @Test
    void shouldRejectDeleteFriendWhenRequiredParametersMissing() {
        Assertions.assertThrows(RuntimeException.class, () -> userFriendService.removeFriend(null, 2L));
        Assertions.assertThrows(RuntimeException.class, () -> userFriendService.removeFriend(1L, null));
    }

    private Page<UserVO> buildSearchUsersPage(Long total, List<UserVO> records) {
        Page<UserVO> page = new Page<>();
        page.setCurrent(1L);
        page.setSize(10L);
        page.setTotal(total);
        page.setRecords(records);
        return page;
    }

    private UserFeignClient createUserFeignClient() {
        return (UserFeignClient) Proxy.newProxyInstance(
                UserFeignClient.class.getClassLoader(),
                new Class[]{UserFeignClient.class},
                (proxy, method, args) -> {
                    if ("listUserByPage".equals(method.getName())) {
                        userFriendService.setCapturedSearchRequest((UserQueryRequest) args[0]);
                        Page<UserVO> searchUsersPage = userFriendService.getSearchUsersPage();
                        return new BaseResponse<>(ErrorCode.SUCCESS.getCode(), searchUsersPage, "ok");
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private ChatOnlineStatusService createChatOnlineStatusService() {
        return (ChatOnlineStatusService) Proxy.newProxyInstance(
                com.stephen.cloud.chat.service.ChatOnlineStatusService.class.getClassLoader(),
                new Class[]{com.stephen.cloud.chat.service.ChatOnlineStatusService.class},
                (proxy, method, args) -> {
                    if ("getOnlineStatusMap".equals(method.getName())) {
                        return userFriendService.getOnlineStatusMapForSearch();
                    }
                    if ("getOnlineStatus".equals(method.getName())) {
                        return userFriendService.getOnlineStatusForSingle((Long) args[0]);
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private UserFriend createFriend(Long userId, Long friendUserId) {
        UserFriend friend = new UserFriend();
        friend.setUserId(userId);
        friend.setFriendUserId(friendUserId);
        return friend;
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

    private static class TestableUserFriendServiceImpl extends UserFriendServiceImpl {
        private List<UserFriend> listResult = new ArrayList<>();
        private int removeInvocationCount;
        private boolean removeResult;
        private final Map<String, Boolean> pendingFriendApply = new HashMap<>();
        private final Map<String, Boolean> mutualFriend = new HashMap<>();
        private Page<UserVO> searchUsersPage = new Page<>();
        private UserQueryRequest capturedSearchRequest;
        private Map<Long, Integer> onlineStatusMap = new HashMap<>();

        @Override
        public List<UserFriend> list(Wrapper<UserFriend> queryWrapper) {
            return new ArrayList<>(listResult);
        }

        @Override
        public boolean remove(Wrapper<UserFriend> queryWrapper) {
            removeInvocationCount++;
            return removeResult;
        }

        @Override
        public boolean isMutualFriend(Long userId, Long friendUserId) {
            return mutualFriend.getOrDefault(key(userId, friendUserId), false);
        }

        @Override
        protected boolean hasPendingFriendApply(Long userId, Long targetUserId) {
            return pendingFriendApply.getOrDefault(key(userId, targetUserId), false);
        }

        void setPendingFriendApply(Long userId, Long targetUserId, boolean exists) {
            String k = key(userId, targetUserId);
            if (exists) {
                pendingFriendApply.put(k, true);
                return;
            }
            pendingFriendApply.remove(k);
        }

        void setMutualFriend(Long userId, Long friendUserId, boolean exists) {
            String k = key(userId, friendUserId);
            if (exists) {
                mutualFriend.put(k, true);
                return;
            }
            mutualFriend.remove(k);
        }

        void setSearchUsers(Page<UserVO> searchUsersPage) {
            this.searchUsersPage = searchUsersPage;
        }

        Page<UserVO> getSearchUsersPage() {
            return searchUsersPage;
        }

        void setCapturedSearchRequest(UserQueryRequest capturedSearchRequest) {
            this.capturedSearchRequest = capturedSearchRequest;
        }

        UserQueryRequest getCapturedSearchRequest() {
            return capturedSearchRequest;
        }

        Map<Long, Integer> getOnlineStatusMapForSearch() {
            return new HashMap<>(onlineStatusMap);
        }

        Integer getOnlineStatusForSingle(Long userId) {
            return onlineStatusMap.getOrDefault(userId, 0);
        }

        void setOnlineStatusMap(Map<Long, Integer> onlineStatusMap) {
            this.onlineStatusMap = new HashMap<>(onlineStatusMap);
        }

        void setRemoveResult(boolean result) {
            this.removeResult = result;
        }

        private static String key(Long userId, Long targetId) {
            return userId + ":" + targetId;
        }
    }

    private static class ActualCacheRecoveryUserFriendServiceImpl extends UserFriendServiceImpl {
        private List<UserFriend> listResult = new ArrayList<>();

        @Override
        public List<UserFriend> list(Wrapper<UserFriend> queryWrapper) {
            return new ArrayList<>(listResult);
        }
    }

    private static class FakeCacheUtils extends CacheUtils {
        private final Map<String, Set<String>> setMap = new HashMap<>();
        private final Map<String, Integer> removeCallCount = new HashMap<>();

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
                            members.addAll((java.util.Collection<String>) setArgs[0]);
                            yield true;
                        }
                        case "contains" -> setMap.getOrDefault(key, Set.of()).contains(setArgs[0]);
                        case "readAll" -> new HashSet<>(setMap.getOrDefault(key, Set.of()));
                        case "removeAll" -> {
                            Set<String> members = setMap.computeIfAbsent(key, ignored -> new HashSet<>());
                            for (Object value : (java.util.Collection<?>) setArgs[0]) {
                                String valueStr = (String) value;
                                members.remove(valueStr);
                                String counterKey = key + ":" + valueStr;
                                removeCallCount.put(counterKey, removeCallCount.getOrDefault(counterKey, 0) + 1);
                            }
                            yield true;
                        }
                        case "remove" -> {
                            Set<String> members = setMap.computeIfAbsent(key, ignored -> new HashSet<>());
                            String value = (String) setArgs[0];
                            members.remove(value);
                            String counterKey = key + ":" + value;
                            removeCallCount.put(counterKey, removeCallCount.getOrDefault(counterKey, 0) + 1);
                            yield true;
                        }
                        default -> defaultValue(setMethod.getReturnType());
                    });
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
