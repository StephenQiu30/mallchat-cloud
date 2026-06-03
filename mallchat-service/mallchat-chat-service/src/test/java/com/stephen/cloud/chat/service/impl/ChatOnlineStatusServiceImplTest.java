package com.stephen.cloud.chat.service.impl;

import com.stephen.cloud.common.cache.utils.CacheUtils;
import com.stephen.cloud.common.constants.WebSocketConstant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

class ChatOnlineStatusServiceImplTest {

    private final FakeCacheUtils cacheUtils = new FakeCacheUtils();

    private ChatOnlineStatusServiceImpl onlineStatusService;

    @BeforeEach
    void setUp() {
        onlineStatusService = new ChatOnlineStatusServiceImpl();
        ReflectionTestUtils.setField(onlineStatusService, "cacheUtils", cacheUtils);
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
