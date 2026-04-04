package com.stephen.cloud.chat.mq.producer;

import com.stephen.cloud.api.chat.model.vo.ChatMessageVO;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.enums.WebSocketMessageTypeEnum;
import com.stephen.cloud.common.rabbitmq.enums.WebSocketPushTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.WebSocketMessage;
import com.stephen.cloud.common.rabbitmq.producer.RabbitMqSender;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 聊天系统消息队列生产者
 * <p>
 * 负责发送聊天消息推送、WebSocket 实时推送等 MQ 消息。
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class ChatMqProducer {

    @Resource
    private RabbitMqSender mqSender;

    /**
     * 发送聊天内容推送
     *
     * @param userIds       接收用户 ID 列表
     * @param chatMessageVO 聊天消息视图对象
     */
    public void sendChatMessagePush(List<Long> userIds, ChatMessageVO chatMessageVO) {
        if (chatMessageVO == null || chatMessageVO.getId() == null) {
            log.warn("[ChatMqProducer] 聊天消息对象或ID为空，跳过发送");
            return;
        }

        try {
            WebSocketMessage wsMessage = WebSocketMessage.builder()
                    .userIds(userIds)
                    .data(chatMessageVO)
                    .build();

            String bizId = "chat_msg:" + chatMessageVO.getId();
            mqSender.send(MqBizTypeEnum.CHAT_MESSAGE_PUSH, bizId, wsMessage);
            log.info("[ChatMqProducer] 发送聊天推送消息成功, msgId: {}, members: {}", chatMessageVO.getId(), userIds.size());
        } catch (Exception e) {
            log.error("[ChatMqProducer] 发送聊天推送消息失败, msgId: {}", chatMessageVO.getId(), e);
        }
    }

    /**
     * 发送通用 WebSocket 推送
     *
     * @param userId userId
     * @param type   消息类型
     * @param data   数据内容
     * @param bizId  业务 ID (用于幂等)
     */
    public void sendWebSocketPush(Long userId, WebSocketMessageTypeEnum type, Object data, String bizId) {
        if (userId == null) {
            log.warn("[ChatMqProducer] 用户ID为空，跳过发送 WebSocket 推送, bizId: {}", bizId);
            return;
        }

        try {
            WebSocketMessage wsMessage = WebSocketMessage.builder()
                    .userId(userId)
                    .type(type.getCode())
                    .pushType(WebSocketPushTypeEnum.SINGLE.getValue())
                    .data(data)
                    .build();

            mqSender.send(MqBizTypeEnum.WEBSOCKET_PUSH, bizId, wsMessage);
            log.info("[ChatMqProducer] 发送 WebSocket 推送消息成功, bizId: {}, userId: {}, type: {}",
                    bizId, userId, type.getDesc());
        } catch (Exception e) {
            log.error("[ChatMqProducer] 发送 WebSocket 推送消息失败, bizId: {}, userId: {}", bizId, userId, e);
        }
    }
}
