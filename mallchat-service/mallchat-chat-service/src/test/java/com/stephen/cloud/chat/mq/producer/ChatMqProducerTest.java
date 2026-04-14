package com.stephen.cloud.chat.mq.producer;

import com.stephen.cloud.api.chat.model.vo.ChatMessageVO;
import com.stephen.cloud.common.rabbitmq.enums.ImWebSocketEventTypeEnum;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.enums.WebSocketPushTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.ImWebSocketEvent;
import com.stephen.cloud.common.rabbitmq.model.WebSocketMessage;
import com.stephen.cloud.common.rabbitmq.producer.RabbitMqSender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

class ChatMqProducerTest {

    private final FakeRabbitMqSender rabbitMqSender = new FakeRabbitMqSender();

    private ChatMqProducer chatMqProducer;

    @BeforeEach
    void setUp() {
        chatMqProducer = new ChatMqProducer();
        ReflectionTestUtils.setField(chatMqProducer, "mqSender", rabbitMqSender);
    }

    @Test
    void shouldWrapChatMessageAsUnifiedRoomEvent() {
        ChatMessageVO chatMessageVO = ChatMessageVO.builder()
                .id(100L)
                .roomId(200L)
                .content("hello")
                .build();

        chatMqProducer.sendChatMessageGroupPush(200L, chatMessageVO);

        Assertions.assertEquals(MqBizTypeEnum.CHAT_MESSAGE_PUSH, rabbitMqSender.bizTypeEnum);
        Assertions.assertEquals("chat_group_msg:100", rabbitMqSender.msgId);
        WebSocketMessage wsMessage = rabbitMqSender.webSocketMessage;
        Assertions.assertEquals(WebSocketPushTypeEnum.BROADCAST.getValue(), wsMessage.getPushType());
        Assertions.assertEquals(200L, wsMessage.getRoomId());
        Assertions.assertInstanceOf(ImWebSocketEvent.class, wsMessage.getData());

        ImWebSocketEvent event = (ImWebSocketEvent) wsMessage.getData();
        Assertions.assertEquals(ImWebSocketEventTypeEnum.CHAT_MESSAGE.getCode(), event.getType());
        Assertions.assertEquals("chat_group_msg:100", event.getBizId());
        Assertions.assertEquals(chatMessageVO, event.getData());
    }

    @Test
    void shouldSendSessionDeleteAsSessionUpdateEvent() {
        chatMqProducer.sendSessionDelete(1L, 10L, "session_delete:10:1");

        Assertions.assertEquals(MqBizTypeEnum.WEBSOCKET_PUSH, rabbitMqSender.bizTypeEnum);
        Assertions.assertEquals("session_delete:10:1", rabbitMqSender.msgId);
        WebSocketMessage wsMessage = rabbitMqSender.webSocketMessage;
        Assertions.assertEquals(WebSocketPushTypeEnum.SINGLE.getValue(), wsMessage.getPushType());
        Assertions.assertEquals(1L, wsMessage.getUserId());

        ImWebSocketEvent event = (ImWebSocketEvent) wsMessage.getData();
        Assertions.assertEquals(ImWebSocketEventTypeEnum.SESSION_UPDATE.getCode(), event.getType());
        Assertions.assertEquals(10L, event.getRoomId());
        Assertions.assertEquals(Map.of("roomId", 10L, "deleted", true), event.getData());
    }

    private static class FakeRabbitMqSender extends RabbitMqSender {
        private MqBizTypeEnum bizTypeEnum;
        private String msgId;
        private WebSocketMessage webSocketMessage;

        @Override
        public void send(MqBizTypeEnum bizTypeEnum, String msgId, Object payload) {
            this.bizTypeEnum = bizTypeEnum;
            this.msgId = msgId;
            this.webSocketMessage = (WebSocketMessage) payload;
        }
    }
}
