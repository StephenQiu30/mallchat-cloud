package com.stephen.cloud.common.websocket.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class WebSocketPropertiesTest {

    @Test
    void shouldDisableWebSocketServerByDefault() {
        WebSocketProperties properties = new WebSocketProperties();

        Assertions.assertFalse(properties.getEnabled());
    }

    @Test
    void shouldNotRestrictOriginsByDefault() {
        WebSocketProperties properties = new WebSocketProperties();

        Assertions.assertTrue(properties.getAllowedOrigins().isEmpty());
    }

    @Test
    void shouldUseConservativeRuntimeGuardDefaults() {
        WebSocketProperties properties = new WebSocketProperties();

        Assertions.assertEquals(5, properties.getMaxConnectionsPerUser());
        Assertions.assertEquals(0L, properties.getMinConnectIntervalMillis());
    }
}
