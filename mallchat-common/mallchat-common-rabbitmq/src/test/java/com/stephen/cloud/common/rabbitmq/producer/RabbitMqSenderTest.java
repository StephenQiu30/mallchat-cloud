package com.stephen.cloud.common.rabbitmq.producer;

import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

class RabbitMqSenderTest {

    private RabbitMqSender sender;
    private FakeRabbitTemplate rabbitTemplate;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        sender = new RabbitMqSender();
        rabbitTemplate = new FakeRabbitTemplate();
        meterRegistry = new SimpleMeterRegistry();
        ReflectionTestUtils.setField(sender, "rabbitTemplateBean", rabbitTemplate);
        ReflectionTestUtils.setField(sender, "publishObservation", new RabbitMqPublishObservation(meterRegistry));
    }

    @Test
    void shouldRecordPublishAcceptedWithBizTypeAndBizId() {
        sender.send(MqBizTypeEnum.WEBSOCKET_PUSH, "session_msg:1", Map.of("roomId", 1L));

        Assertions.assertEquals(MqBizTypeEnum.WEBSOCKET_PUSH.getExchange(), rabbitTemplate.exchange);
        Assertions.assertEquals(MqBizTypeEnum.WEBSOCKET_PUSH.getRoutingKey(), rabbitTemplate.routingKey);
        Assertions.assertEquals("WEBSOCKET_PUSH:session_msg:1", rabbitTemplate.correlationId);
        Assertions.assertEquals("WEBSOCKET_PUSH", rabbitTemplate.headers.get("bizType"));
        Assertions.assertEquals("session_msg:1", rabbitTemplate.headers.get("bizId"));
        Assertions.assertInstanceOf(RabbitMessage.class, rabbitTemplate.payload);
        Assertions.assertEquals(1.0, counterValue("mallchat.rabbitmq.publish.total", "WEBSOCKET_PUSH", "accepted"));
    }

    @Test
    void shouldRecordPublishFailureAndRethrow() {
        rabbitTemplate.throwOnSend = true;

        Assertions.assertThrows(AmqpException.class,
                () -> sender.send(MqBizTypeEnum.CHAT_MESSAGE_PUSH, "chat_group_msg:1", Map.of("messageId", 1L)));

        Assertions.assertEquals(1.0, counterValue("mallchat.rabbitmq.publish.total", "CHAT_MESSAGE_PUSH", "failed"));
    }

    @Test
    void shouldRecordRejectedPublishWhenPayloadIsNull() {
        sender.send(MqBizTypeEnum.NOTIFICATION_SEND, "notice-1", null);

        Assertions.assertNull(rabbitTemplate.payload);
        Assertions.assertEquals(1.0, counterValue("mallchat.rabbitmq.publish.total", "NOTIFICATION_SEND", "rejected"));
    }

    private double counterValue(String name, String bizType, String result) {
        return meterRegistry.get(name)
                .tag("bizType", bizType)
                .tag("result", result)
                .counter()
                .count();
    }

    private static class FakeRabbitTemplate extends RabbitTemplate {
        private String exchange;
        private String routingKey;
        private Object payload;
        private String correlationId;
        private Map<String, Object> headers;
        private boolean throwOnSend;

        @Override
        public void convertAndSend(String exchange, String routingKey, Object object,
                                   MessagePostProcessor messagePostProcessor,
                                   CorrelationData correlationData) throws AmqpException {
            if (throwOnSend) {
                throw new AmqpException("send failed");
            }
            this.exchange = exchange;
            this.routingKey = routingKey;
            this.payload = object;
            this.correlationId = correlationData == null ? null : correlationData.getId();
            Message message = new Message(new byte[0], new MessageProperties());
            Message processed = messagePostProcessor.postProcessMessage(message);
            this.headers = processed.getMessageProperties().getHeaders();
        }
    }
}
