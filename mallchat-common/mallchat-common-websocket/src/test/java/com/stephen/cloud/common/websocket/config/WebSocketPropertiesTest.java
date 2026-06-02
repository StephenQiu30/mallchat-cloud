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

    @Test
    void shouldUseDefaultHeartbeatParameters() {
        WebSocketProperties properties = new WebSocketProperties();

        Assertions.assertEquals(60L, properties.getHeartbeatReaderIdle());
        Assertions.assertEquals(30L, properties.getHeartbeatWriterIdle());
    }

    @Test
    void shouldAcceptCustomHeartbeatParameters() {
        WebSocketProperties properties = new WebSocketProperties();
        properties.setHeartbeatReaderIdle(120L);
        properties.setHeartbeatWriterIdle(60L);

        Assertions.assertEquals(120L, properties.getHeartbeatReaderIdle());
        Assertions.assertEquals(60L, properties.getHeartbeatWriterIdle());
    }
}
