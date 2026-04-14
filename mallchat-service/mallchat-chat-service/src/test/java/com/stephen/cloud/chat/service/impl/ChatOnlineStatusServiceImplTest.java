package com.stephen.cloud.chat.service.impl;

import com.stephen.cloud.common.cache.utils.CacheUtils;
import com.stephen.cloud.common.constants.WebSocketConstant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
