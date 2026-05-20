package com.stephen.cloud.notification.mq.handler;

import com.stephen.cloud.common.rabbitmq.model.NotificationMessage;
import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import com.stephen.cloud.common.websocket.manager.ChannelManager;
import com.stephen.cloud.notification.mq.support.ImPushMetricsRecorder;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class NotificationPushHandlerTest {

    private NotificationPushHandler handler;
    private FakeChannelManager channelManager;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        handler = new NotificationPushHandler();
        channelManager = new FakeChannelManager();
        meterRegistry = new SimpleMeterRegistry();
        ReflectionTestUtils.setField(handler, "channelManager", channelManager);
        ReflectionTestUtils.setField(handler, "metricsRecorder", new ImPushMetricsRecorder(meterRegistry));
    }

    @Test
    void shouldRecordOfflineNotificationPushWhenUserHasNoLocalConnection() throws Exception {
        channelManager.writeResult = 0;
        NotificationMessage message = NotificationMessage.builder()
                .userId(1L)
                .title("通知")
                .content("hello")
                .build();

        handler.onMessage(message, RabbitMessage.builder().msgId("notice-1").build());

        Assertions.assertEquals(1.0, pushCounter("NOTIFICATION_SEND", "SYSTEM_NOTICE", "offline"));
    }

    @Test
    void shouldRecordNotificationPushFailureWhenChannelWriteThrows() {
        channelManager.throwOnWrite = true;
        NotificationMessage message = NotificationMessage.builder()
                .userId(1L)
                .title("通知")
                .content("hello")
                .build();

        Assertions.assertThrows(RuntimeException.class,
                () -> handler.onMessage(message, RabbitMessage.builder().msgId("notice-2").build()));

        Assertions.assertEquals(1.0, pushCounter("NOTIFICATION_SEND", "SYSTEM_NOTICE", "failure"));
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
        private int writeResult = 1;
        private boolean throwOnWrite;

        @Override
        public int writeToUser(String userId, String messageJson) {
            if (throwOnWrite) {
                throw new RuntimeException("push failed");
            }
            return writeResult;
        }
    }
}
