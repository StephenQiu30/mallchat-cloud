package com.stephen.cloud.chat.service.impl;

import com.stephen.cloud.common.cache.utils.CacheUtils;
import com.stephen.cloud.common.cache.utils.LocalCacheUtils;
import com.stephen.cloud.common.constants.WebSocketConstant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

class ChatOnlineStatusServiceImplTest {

    private final FakeCacheUtils cacheUtils = new FakeCacheUtils();
    private final FakeLocalCacheUtils localCacheUtils = new FakeLocalCacheUtils();

    private ChatOnlineStatusServiceImpl onlineStatusService;

    @BeforeEach
    void setUp() {
        onlineStatusService = new ChatOnlineStatusServiceImpl();
        ReflectionTestUtils.setField(onlineStatusService, "cacheUtils", cacheUtils);
        ReflectionTestUtils.setField(onlineStatusService, "localCacheUtils", localCacheUtils);
    }

    @Test
    void shouldReturnOnlineWhenUserHasConnections() {
        cacheUtils.setMembers(WebSocketConstant.WS_USER_CONNECTIONS_KEY + 1L, Set.of("conn-1"));

        Assertions.assertEquals(1, onlineStatusService.getOnlineStatus(1L));
    }

    @Test
    void shouldReturnOfflineWhenUserHasNoConnections() {
        cacheUtils.setMembers(WebSocketConstant.WS_USER_CONNECTIONS_KEY + 2L, Set.of());

        Assertions.assertEquals(0, onlineStatusService.getOnlineStatus(2L));
    }

    @Test
    void getOnlineStatusShouldCompleteWithin5ms() {
        // given: 预置在线用户数据
        cacheUtils.setMembers(WebSocketConstant.WS_USER_CONNECTIONS_KEY + 1L, Set.of("conn-1"));

        // when & then: 单次查询应在 5ms 内完成
        long startNanos = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            onlineStatusService.getOnlineStatus(1L);
        }
        long elapsedNanos = System.nanoTime() - startNanos;
        double avgMs = elapsedNanos / 1_000_000.0 / 1000;

        Assertions.assertTrue(avgMs < 5.0,
                "getOnlineStatus 平均耗时应 < 5ms，实际: " + String.format("%.3f", avgMs) + "ms");
    }

    // --- 边界守卫测试 ---

    @Test
    void getOnlineStatusShouldReturnOfflineForNullUserId() {
        Assertions.assertEquals(0, onlineStatusService.getOnlineStatus(null));
    }

    @Test
    void getOnlineStatusShouldReturnOfflineForZeroUserId() {
        Assertions.assertEquals(0, onlineStatusService.getOnlineStatus(0L));
    }

    @Test
    void getOnlineStatusShouldReturnOfflineForNegativeUserId() {
        Assertions.assertEquals(0, onlineStatusService.getOnlineStatus(-1L));
    }

    @Test
    void getOnlineStatusMapShouldReturnEmptyMapForNullCollection() {
        Map<Long, Integer> result = onlineStatusService.getOnlineStatusMap(null);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void getOnlineStatusMapShouldReturnEmptyMapForEmptyCollection() {
        Map<Long, Integer> result = onlineStatusService.getOnlineStatusMap(Collections.emptyList());
        Assertions.assertTrue(result.isEmpty());
    }

    // --- 批量正确性测试 ---

    @Test
    void getOnlineStatusMapShouldReturnCorrectStatusForEachUser() {
        // given: user1 在线, user2 离线, user3 在线
        cacheUtils.setMembers(WebSocketConstant.WS_USER_CONNECTIONS_KEY + 1L, Set.of("conn-1"));
        cacheUtils.setMembers(WebSocketConstant.WS_USER_CONNECTIONS_KEY + 2L, Set.of());
        cacheUtils.setMembers(WebSocketConstant.WS_USER_CONNECTIONS_KEY + 3L, Set.of("conn-3a", "conn-3b"));

        // when
        Map<Long, Integer> result = onlineStatusService.getOnlineStatusMap(Arrays.asList(1L, 2L, 3L));

        // then
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals(1, result.get(1L));
        Assertions.assertEquals(0, result.get(2L));
        Assertions.assertEquals(1, result.get(3L));
    }

    @Test
    void getOnlineStatusMapShouldHandleUnknownUsersAsOffline() {
        // given: 只预置 user1, 查询 user1 和 user999（user999 无数据）
        cacheUtils.setMembers(WebSocketConstant.WS_USER_CONNECTIONS_KEY + 1L, Set.of("conn-1"));

        // when
        Map<Long, Integer> result = onlineStatusService.getOnlineStatusMap(Arrays.asList(1L, 999L));

        // then
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(1, result.get(1L));
        Assertions.assertEquals(0, result.get(999L));
    }

    // --- 性能基准测试 ---

    @Test
    void getOnlineStatusMapShouldCompleteWithinReasonableTime() {
        // given: 预置 100 个用户数据
        List<Long> userIds = LongStream.rangeClosed(1, 100).boxed().collect(Collectors.toList());
        for (Long userId : userIds) {
            cacheUtils.setMembers(WebSocketConstant.WS_USER_CONNECTIONS_KEY + userId, Set.of("conn-" + userId));
        }

        // when & then: 批量查询 100 个用户应在合理时间内完成
        long startNanos = System.nanoTime();
        Map<Long, Integer> result = onlineStatusService.getOnlineStatusMap(userIds);
        long elapsedNanos = System.nanoTime() - startNanos;
        double elapsedMs = elapsedNanos / 1_000_000.0;

        Assertions.assertEquals(100, result.size());
        Assertions.assertTrue(elapsedMs < 50.0,
                "getOnlineStatusMap(100) 应在 50ms 内完成，实际: " + String.format("%.3f", elapsedMs) + "ms");
    }

    private static class FakeLocalCacheUtils extends LocalCacheUtils {
        private final Map<String, Object> store = new HashMap<>();

        @Override
        public void put(String key, Object value) {
            store.put(key, value);
        }

        @Override
        public Object get(String key) {
            return store.get(key);
        }

        @Override
        public <T> T get(String key, Class<T> clazz) {
            Object value = store.get(key);
            if (value == null) {
                return null;
            }
            return clazz.cast(value);
        }

        @Override
        public boolean exists(String key) {
            return store.containsKey(key);
        }

        @Override
        public void delete(String key) {
            store.remove(key);
        }

        @Override
        public void clear() {
            store.clear();
        }
    }

    private static class FakeCacheUtils extends CacheUtils {
        private final Map<String, Set<String>> setMap = new HashMap<>();

        void setMembers(String key, Set<String> values) {
            setMap.put(key, values);
        }

        @Override
        public <T> Set<T> sMembers(String key) {
            return (Set<T>) setMap.getOrDefault(key, Set.of());
        }
    }
}
