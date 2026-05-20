package com.stephen.cloud.common.rabbitmq.config;

import com.stephen.cloud.common.rabbitmq.producer.RabbitMqPublishObservation;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

class RabbitMqConfigurationTest {

    private SimpleMeterRegistry meterRegistry;
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        RabbitMqConfiguration configuration = new RabbitMqConfiguration();
        rabbitTemplate = configuration.rabbitTemplate(Mockito.mock(ConnectionFactory.class),
                configuration.messageConverter(configuration.jacksonMessageConverter()),
                new RabbitMqPublishObservation(meterRegistry));
    }

    @Test
    void shouldRecordConfirmCallbackOutcome() {
        RabbitTemplate.ConfirmCallback confirmCallback =
                (RabbitTemplate.ConfirmCallback) ReflectionTestUtils.getField(rabbitTemplate, "confirmCallback");

        confirmCallback.confirm(new CorrelationData("CHAT_MESSAGE_PUSH:chat_group_msg:1"), true, null);
        confirmCallback.confirm(new CorrelationData("CHAT_MESSAGE_PUSH:chat_group_msg:2"), false, "nack");

        Assertions.assertEquals(1.0, counterValue("mallchat.rabbitmq.confirm.total", "CHAT_MESSAGE_PUSH", "ack"));
        Assertions.assertEquals(1.0, counterValue("mallchat.rabbitmq.confirm.total", "CHAT_MESSAGE_PUSH", "nack"));
    }

    @Test
    void shouldRecordReturnCallbackOutcome() {
        RabbitTemplate.ReturnsCallback returnsCallback =
                (RabbitTemplate.ReturnsCallback) ReflectionTestUtils.getField(rabbitTemplate, "returnsCallback");
        MessageProperties properties = new MessageProperties();
        properties.setHeader("bizType", "WEBSOCKET_PUSH");
        properties.setHeader("bizId", "session_msg:1");

        returnsCallback.returnedMessage(new ReturnedMessage(new Message(new byte[0], properties),
                312, "NO_ROUTE", "exchange", "routing"));

        Assertions.assertEquals(1.0, counterValue("mallchat.rabbitmq.return.total", "WEBSOCKET_PUSH", "312"));
    }

    private double counterValue(String name, String bizType, String result) {
        return meterRegistry.get(name)
                .tag("bizType", bizType)
                .tag("result", result)
                .counter()
                .count();
    }
}
