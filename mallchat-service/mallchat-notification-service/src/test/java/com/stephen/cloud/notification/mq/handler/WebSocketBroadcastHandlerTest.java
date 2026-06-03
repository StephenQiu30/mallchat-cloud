package com.stephen.cloud.notification.mq.handler;

import com.stephen.cloud.common.rabbitmq.enums.WebSocketMessageTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.ImWebSocketEvent;
import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import com.stephen.cloud.common.rabbitmq.model.WebSocketMessage;
import com.stephen.cloud.common.websocket.manager.ChannelManager;
import com.stephen.cloud.notification.mq.support.ImPushMetricsRecorder;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
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
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        handler = new WebSocketBroadcastHandler();
        channelManager = new FakeChannelManager();
        meterRegistry = new SimpleMeterRegistry();
        ReflectionTestUtils.setField(handler, "channelManager", channelManager);
        ReflectionTestUtils.setField(handler, "metricsRecorder", new ImPushMetricsRecorder(meterRegistry));
    }

    // --- 单用户中转 ---

    @Test
    void shouldForwardSingleUserMessageToAllLocalConnections() throws Exception {
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .userId(1L)
                .type(WebSocketMessageTypeEnum.MESSAGE.getCode())
                .data(ImWebSocketEvent.builder()
                        .type("ONLINE_STATUS")
                        .data(Map.of("userId", 1L))
                        .build())
                .build();

        handler.onMessage(wsMessage, RabbitMessage.builder().msgId("broadcast-single").build());

        Assertions.assertEquals(1, channelManager.writeCountByUser.get("1"));
    }

    @Test
    void shouldRecordSuccessWhenSingleUserRelayReachesLocalConnection() throws Exception {
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .userId(1L)
                .type(WebSocketMessageTypeEnum.MESSAGE.getCode())
                .data(ImWebSocketEvent.builder()
                        .type("ONLINE_STATUS")
                        .data(Map.of("userId", 1L))
                        .build())
                .build();

        handler.onMessage(wsMessage, RabbitMessage.builder().msgId("msg-1").build());

        Assertions.assertEquals(1.0, pushCounter("WEBSOCKET_BROADCAST", "ONLINE_STATUS", "success"));
    }

    @Test
    void shouldRecordOfflineWhenSingleUserRelayHasNoLocalConnection() throws Exception {
        channelManager.writeResult = 0;
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .userId(1L)
                .type(WebSocketMessageTypeEnum.MESSAGE.getCode())
                .data(ImWebSocketEvent.builder()
                        .type("ONLINE_STATUS")
                        .data(Map.of("userId", 1L))
                        .build())
                .build();

        handler.onMessage(wsMessage, RabbitMessage.builder().msgId("msg-1").build());

        Assertions.assertEquals(1.0, pushCounter("WEBSOCKET_BROADCAST", "ONLINE_STATUS", "offline"));
    }

    @Test
    void shouldRecordFailureAndRethrowWhenSingleUserRelayWriteThrows() {
        channelManager.throwOnWrite = true;
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .userId(1L)
                .type(WebSocketMessageTypeEnum.MESSAGE.getCode())
                .data(ImWebSocketEvent.builder()
                        .type("ONLINE_STATUS")
                        .data(Map.of("userId", 1L))
                        .build())
                .build();

        Assertions.assertThrows(RuntimeException.class,
                () -> handler.onMessage(wsMessage, RabbitMessage.builder().msgId("msg-1").build()));

        Assertions.assertEquals(1.0, pushCounter("WEBSOCKET_BROADCAST", "ONLINE_STATUS", "failure"));
    }

    // --- 多用户推送 ---

    @Test
    void shouldForwardMultipleUserMessageByWriteToUser() throws Exception {
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .userIds(List.of(1L, 2L))
                .type(WebSocketMessageTypeEnum.MESSAGE.getCode())
                .data(ImWebSocketEvent.builder()
                        .type("SESSION_UPDATE")
                        .data(Map.of("roomId", 1L))
                        .build())
                .build();

        handler.onMessage(wsMessage, RabbitMessage.builder().msgId("broadcast-multi").build());

        Assertions.assertEquals(1, channelManager.writeCountByUser.get("1"));
        Assertions.assertEquals(1, channelManager.writeCountByUser.get("2"));
    }

    @Test
    void shouldRecordAggregatedOfflineWhenMultipleUsersHaveNoLocalConnection() throws Exception {
        channelManager.writeResult = 0;
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .userIds(List.of(1L, 2L))
                .type(WebSocketMessageTypeEnum.MESSAGE.getCode())
                .data(ImWebSocketEvent.builder()
                        .type("SESSION_UPDATE")
                        .data(Map.of("roomId", 1L))
                        .build())
                .build();

        handler.onMessage(wsMessage, RabbitMessage.builder().msgId("msg-2").build());

        Assertions.assertEquals(2.0, pushCounter("WEBSOCKET_BROADCAST", "SESSION_UPDATE", "offline"));
    }

    @Test
    void shouldRecordMixedSuccessAndOfflineWhenOnlyPartOfMultipleTargetsAreOnline() throws Exception {
        channelManager.writeResultByUser.put("1", 1);
        channelManager.writeResultByUser.put("2", 0);
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .userIds(List.of(1L, 2L))
                .type(WebSocketMessageTypeEnum.MESSAGE.getCode())
                .data(ImWebSocketEvent.builder()
                        .type("SESSION_UPDATE")
                        .data(Map.of("roomId", 1L))
                        .build())
                .build();

        handler.onMessage(wsMessage, RabbitMessage.builder().msgId("msg-2").build());

        Assertions.assertEquals(1.0, pushCounter("WEBSOCKET_BROADCAST", "SESSION_UPDATE", "success"));
        Assertions.assertEquals(1.0, pushCounter("WEBSOCKET_BROADCAST", "SESSION_UPDATE", "offline"));
    }

    @Test
    void shouldRecordFailureAndRethrowWhenMultipleUserWriteThrows() {
        channelManager.throwOnWrite = true;
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .userIds(List.of(1L, 2L))
                .type(WebSocketMessageTypeEnum.MESSAGE.getCode())
                .data(ImWebSocketEvent.builder()
                        .type("SESSION_UPDATE")
                        .data(Map.of("roomId", 1L))
                        .build())
                .build();

        Assertions.assertThrows(RuntimeException.class,
                () -> handler.onMessage(wsMessage, RabbitMessage.builder().msgId("msg-2").build()));

        Assertions.assertEquals(1.0, pushCounter("WEBSOCKET_BROADCAST", "SESSION_UPDATE", "failure"));
    }

    // --- 全服广播 ---

    @Test
    void shouldRecordSuccessWithOnlineCountWhenFullBroadcast() throws Exception {
        channelManager.onlineCount = 5;
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .type(WebSocketMessageTypeEnum.MESSAGE.getCode())
                .data(ImWebSocketEvent.builder()
                        .type("SYSTEM_BROADCAST")
                        .data(Map.of("content", "hello"))
                        .build())
                .build();

        handler.onMessage(wsMessage, RabbitMessage.builder().msgId("broadcast-all").build());

        Assertions.assertEquals(5.0, pushCounter("WEBSOCKET_BROADCAST", "SYSTEM_BROADCAST", "success"));
    }

    // --- helpers ---

    private double pushCounter(String bizType, String eventType, String result) {
        return meterRegistry.get("mallchat.im.push.total")
                .tag("bizType", bizType)
                .tag("eventType", eventType)
                .tag("result", result)
                .counter()
                .count();
    }

    private static class FakeChannelManager extends ChannelManager {
        private final Map<String, Integer> writeCountByUser = new HashMap<>();
        private final Map<String, Integer> writeResultByUser = new HashMap<>();
        private int writeResult = 1;
        private boolean throwOnWrite;
        private int onlineCount = 0;

        @Override
        public int writeToUser(String userId, String messageJson) {
            if (throwOnWrite) {
                throw new RuntimeException("push failed");
            }
            writeCountByUser.merge(userId, 1, Integer::sum);
            return writeResultByUser.getOrDefault(userId, writeResult);
        }

        @Override
        public ChannelGroup getAllChannels() {
            return new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        }

        @Override
        public int getOnlineCount() {
            return onlineCount;
        }
    }
}
