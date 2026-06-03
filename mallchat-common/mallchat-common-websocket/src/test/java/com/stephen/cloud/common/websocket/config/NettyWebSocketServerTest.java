package com.stephen.cloud.common.websocket.config;

import com.stephen.cloud.common.websocket.manager.ChannelManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class NettyWebSocketServerTest {

    private final ChannelManager channelManager = Mockito.mock(ChannelManager.class);
    private final NettyWebSocketServer server = new NettyWebSocketServer(channelManager);

    @Test
    void shouldRejectNullReaderIdle() {
        WebSocketProperties properties = new WebSocketProperties();
        properties.setHeartbeatReaderIdle(null);
        properties.setHeartbeatWriterIdle(30L);

        Assertions.assertThrows(IllegalArgumentException.class, () -> server.startServer(properties));
    }

    @Test
    void shouldRejectNullWriterIdle() {
        WebSocketProperties properties = new WebSocketProperties();
        properties.setHeartbeatReaderIdle(60L);
        properties.setHeartbeatWriterIdle(null);

        Assertions.assertThrows(IllegalArgumentException.class, () -> server.startServer(properties));
    }

    @Test
    void shouldRejectNegativeReaderIdle() {
        WebSocketProperties properties = new WebSocketProperties();
        properties.setHeartbeatReaderIdle(-1L);
        properties.setHeartbeatWriterIdle(30L);

        Assertions.assertThrows(IllegalArgumentException.class, () -> server.startServer(properties));
    }

    @Test
    void shouldRejectZeroReaderIdle() {
        WebSocketProperties properties = new WebSocketProperties();
        properties.setHeartbeatReaderIdle(0L);
        properties.setHeartbeatWriterIdle(30L);

        Assertions.assertThrows(IllegalArgumentException.class, () -> server.startServer(properties));
    }

    @Test
    void shouldRejectNegativeWriterIdle() {
        WebSocketProperties properties = new WebSocketProperties();
        properties.setHeartbeatReaderIdle(60L);
        properties.setHeartbeatWriterIdle(-1L);

        Assertions.assertThrows(IllegalArgumentException.class, () -> server.startServer(properties));
    }

    @Test
    void shouldRejectZeroWriterIdle() {
        WebSocketProperties properties = new WebSocketProperties();
        properties.setHeartbeatReaderIdle(60L);
        properties.setHeartbeatWriterIdle(0L);

        Assertions.assertThrows(IllegalArgumentException.class, () -> server.startServer(properties));
    }

    @Test
    void shouldRejectWriterIdleGreaterThanOrEqualReaderIdle() {
        WebSocketProperties properties = new WebSocketProperties();
        properties.setHeartbeatReaderIdle(30L);
        properties.setHeartbeatWriterIdle(60L);

        Assertions.assertThrows(IllegalArgumentException.class, () -> server.startServer(properties));
    }

    @Test
    void shouldRejectWriterIdleEqualToReaderIdle() {
        WebSocketProperties properties = new WebSocketProperties();
        properties.setHeartbeatReaderIdle(30L);
        properties.setHeartbeatWriterIdle(30L);

        Assertions.assertThrows(IllegalArgumentException.class, () -> server.startServer(properties));
    }
}
