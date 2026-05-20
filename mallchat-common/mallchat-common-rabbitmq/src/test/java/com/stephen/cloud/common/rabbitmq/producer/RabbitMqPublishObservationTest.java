package com.stephen.cloud.common.rabbitmq.producer;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

class RabbitMqPublishObservationTest {

    private SimpleMeterRegistry meterRegistry;
    private RabbitMqPublishObservation observation;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        observation = new RabbitMqPublishObservation(meterRegistry);
    }

    @Test
    void shouldRecordConfirmAckWithBizTypeAndBizId() {
        observation.recordConfirm("CHAT_MESSAGE_PUSH:chat_group_msg:100", true, null);

        Assertions.assertEquals(1.0, counterValue("mallchat.rabbitmq.confirm.total", "CHAT_MESSAGE_PUSH", "ack"));
    }

    @Test
    void shouldRecordConfirmNackWithBizTypeAndBizId() {
        observation.recordConfirm("WEBSOCKET_PUSH:session_msg:1", false, "exchange unavailable");

        Assertions.assertEquals(1.0, counterValue("mallchat.rabbitmq.confirm.total", "WEBSOCKET_PUSH", "nack"));
    }

    @Test
    void shouldRecordReturnedMessageWithHeaders() {
        observation.recordReturned(Map.of("bizType", "NOTIFICATION_SEND", "bizId", "notice-1"), 312);

        Assertions.assertEquals(1.0, counterValue("mallchat.rabbitmq.return.total", "NOTIFICATION_SEND", "312"));
    }

    private double counterValue(String name, String bizType, String result) {
        return meterRegistry.get(name)
                .tag("bizType", bizType)
                .tag("result", result)
                .counter()
                .count();
    }
}
