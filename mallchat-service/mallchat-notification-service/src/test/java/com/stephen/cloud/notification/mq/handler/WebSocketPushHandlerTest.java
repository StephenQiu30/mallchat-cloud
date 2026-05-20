package com.stephen.cloud.notification.mq.handler;

import com.stephen.cloud.common.rabbitmq.enums.WebSocketMessageTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.ImWebSocketEvent;
import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import com.stephen.cloud.common.rabbitmq.model.WebSocketMessage;
import com.stephen.cloud.common.websocket.manager.ChannelManager;
import com.stephen.cloud.notification.mq.support.ImPushMetricsRecorder;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class WebSocketPushHandlerTest {

    private WebSocketPushHandler handler;
    private FakeChannelManager channelManager;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        handler = new WebSocketPushHandler();
        channelManager = new FakeChannelManager();
        meterRegistry = new SimpleMeterRegistry();
        ReflectionTestUtils.setField(handler, "channelManager", channelManager);
        ReflectionTestUtils.setField(handler, "metricsRecorder", new ImPushMetricsRecorder(meterRegistry));
    }

    @Test
    void shouldRecordOfflineWhenSingleUserHasNoLocalConnection() throws Exception {
        channelManager.writeResult = 0;
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .userId(1L)
                .type(WebSocketMessageTypeEnum.MESSAGE.getCode())
                .data(ImWebSocketEvent.builder()
                        .type("SESSION_UPDATE")
                        .bizId("session_msg:1")
                        .data(Map.of("roomId", 1L))
                        .build())
                .build();

        handler.onMessage(wsMessage, RabbitMessage.builder().msgId("msg-1").build());

        Assertions.assertEquals(1.0, pushCounter("WEBSOCKET_PUSH", "SESSION_UPDATE", "offline"));
    }

    @Test
    void shouldRecordFailureAndRethrowWhenSingleUserWriteThrows() {
        channelManager.throwOnWrite = true;
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .userId(1L)
                .type(WebSocketMessageTypeEnum.MESSAGE.getCode())
                .data(ImWebSocketEvent.builder()
                        .type("SESSION_UPDATE")
                        .bizId("session_msg:1")
                        .data(Map.of("roomId", 1L))
                        .build())
                .build();

        Assertions.assertThrows(RuntimeException.class,
                () -> handler.onMessage(wsMessage, RabbitMessage.builder().msgId("msg-1").build()));

        Assertions.assertEquals(1.0, pushCounter("WEBSOCKET_PUSH", "SESSION_UPDATE", "failure"));
    }

    @Test
    void shouldRecordOfflineForEachTargetWhenMultipleUsersHaveNoLocalConnection() throws Exception {
        channelManager.writeResult = 0;
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .userIds(List.of(1L, 2L))
                .type(WebSocketMessageTypeEnum.ONLINE_STATUS.getCode())
                .data(Map.of("type", "ONLINE_STATUS"))
                .build();

        handler.onMessage(wsMessage, RabbitMessage.builder().msgId("msg-2").build());

        Assertions.assertEquals(2.0, pushCounter("WEBSOCKET_PUSH", "ONLINE_STATUS", "offline"));
    }

    @Test
    void shouldRecordMixedSuccessAndOfflineWhenOnlyPartOfMultipleTargetsAreLocalOnline() throws Exception {
        channelManager.writeResultByUser.put("1", 1);
        channelManager.writeResultByUser.put("2", 0);
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .userIds(List.of(1L, 2L))
                .type(WebSocketMessageTypeEnum.ONLINE_STATUS.getCode())
                .data(Map.of("type", "ONLINE_STATUS"))
                .build();

        handler.onMessage(wsMessage, RabbitMessage.builder().msgId("msg-2").build());

        Assertions.assertEquals(1.0, pushCounter("WEBSOCKET_PUSH", "ONLINE_STATUS", "success"));
        Assertions.assertEquals(1.0, pushCounter("WEBSOCKET_PUSH", "ONLINE_STATUS", "offline"));
    }

    private double pushCounter(String bizType, String eventType, String result) {
        return meterRegistry.get("mallchat.im.push.total")
                .tag("bizType", bizType)
                .tag("eventType", eventType)
                .tag("result", result)
                .counter()
                .count();
    }

    private static class FakeChannelManager extends ChannelManager {
        private final Map<String, Integer> writeResultByUser = new HashMap<>();
        private int writeResult = 1;
        private boolean throwOnWrite;

        @Override
        public int writeToUser(String userId, String messageJson) {
            if (throwOnWrite) {
                throw new RuntimeException("push failed");
            }
            return writeResultByUser.getOrDefault(userId, writeResult);
        }
    }
}
