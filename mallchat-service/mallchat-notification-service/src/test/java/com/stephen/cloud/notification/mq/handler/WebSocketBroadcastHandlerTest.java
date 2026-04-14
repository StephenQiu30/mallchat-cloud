package com.stephen.cloud.notification.mq.handler;

import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import com.stephen.cloud.common.rabbitmq.model.WebSocketMessage;
import com.stephen.cloud.common.websocket.manager.ChannelManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class WebSocketBroadcastHandlerTest {

    private WebSocketBroadcastHandler handler;

    private FakeChannelManager channelManager;

    @BeforeEach
    void setUp() {
        handler = new WebSocketBroadcastHandler();
        channelManager = new FakeChannelManager();
        ReflectionTestUtils.setField(handler, "channelManager", channelManager);
    }

    @Test
    void shouldForwardSingleUserMessageToAllLocalConnections() throws Exception {
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .userId(1L)
                .data(Map.of("type", "ONLINE_STATUS"))
                .build();

        handler.onMessage(wsMessage, RabbitMessage.builder().msgId("broadcast-single").build());

        Assertions.assertEquals(1, channelManager.writeCountByUser.get("1"));
    }

    @Test
    void shouldForwardMultipleUserMessageByWriteToUser() throws Exception {
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .userIds(List.of(1L, 2L))
                .data(Map.of("type", "SESSION_UPDATE"))
                .build();

        handler.onMessage(wsMessage, RabbitMessage.builder().msgId("broadcast-multi").build());

        Assertions.assertEquals(1, channelManager.writeCountByUser.get("1"));
        Assertions.assertEquals(1, channelManager.writeCountByUser.get("2"));
        Assertions.assertNull(channelManager.getChannel("1"));
    }

    private static class FakeChannelManager extends ChannelManager {
        private final Map<String, Integer> writeCountByUser = new HashMap<>();

        @Override
        public int writeToUser(String userId, String messageJson) {
            writeCountByUser.merge(userId, 1, Integer::sum);
            return 1;
        }
    }
}
