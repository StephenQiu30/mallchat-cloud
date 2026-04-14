package com.stephen.cloud.chat.mq.producer;

import com.stephen.cloud.api.chat.model.vo.ChatMessageVO;
import com.stephen.cloud.common.rabbitmq.enums.ImWebSocketEventTypeEnum;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.enums.WebSocketMessageTypeEnum;
import com.stephen.cloud.common.rabbitmq.enums.WebSocketPushTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.ImWebSocketEvent;
import com.stephen.cloud.common.rabbitmq.model.WebSocketMessage;
import com.stephen.cloud.common.rabbitmq.producer.RabbitMqSender;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 聊天系统消息队列生产者
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class ChatMqProducer {

    @Resource
    private RabbitMqSender mqSender;

    public void sendChatMessagePush(List<Long> userIds, ChatMessageVO chatMessageVO) {
        if (chatMessageVO == null || chatMessageVO.getId() == null) {
            return;
        }
        sendUsersEvent(userIds, ImWebSocketEventTypeEnum.CHAT_MESSAGE, chatMessageVO.getRoomId(), chatMessageVO,
                "chat_msg:" + chatMessageVO.getId());
    }

    public void sendChatMessageGroupPush(Long roomId, ChatMessageVO chatMessageVO) {
        if (chatMessageVO == null || chatMessageVO.getId() == null || roomId == null) {
            log.warn("[ChatMqProducer] 房间ID或消息对象为空，跳过广播发送");
            return;
        }
        sendRoomEvent(roomId, ImWebSocketEventTypeEnum.CHAT_MESSAGE, chatMessageVO, "chat_group_msg:" + chatMessageVO.getId());
    }

    public void sendMessageRecall(Long roomId, ChatMessageVO chatMessageVO) {
        if (chatMessageVO == null || chatMessageVO.getId() == null || roomId == null) {
            return;
        }
        sendRoomEvent(roomId, ImWebSocketEventTypeEnum.MESSAGE_RECALL, chatMessageVO, "chat_recall:" + chatMessageVO.getId());
    }

    public void sendMessageRead(Long roomId, Object data, String bizId) {
        sendRoomEvent(roomId, ImWebSocketEventTypeEnum.MESSAGE_READ, data, bizId);
    }

    public void sendSessionUpdate(Long userId, Long roomId, Object data, String bizId) {
        sendUserEvent(userId, ImWebSocketEventTypeEnum.SESSION_UPDATE, roomId, data, bizId);
    }

    public void sendSessionDelete(Long userId, Long roomId, String bizId) {
        sendUserEvent(userId, ImWebSocketEventTypeEnum.SESSION_UPDATE, roomId,
                Map.of("roomId", roomId, "deleted", true), bizId);
    }

    public void sendFriendApply(Long userId, Object data, String bizId) {
        sendUserEvent(userId, ImWebSocketEventTypeEnum.FRIEND_APPLY, null, data, bizId);
    }

    public void sendFriendApprove(Long userId, Object data, String bizId) {
        sendUserEvent(userId, ImWebSocketEventTypeEnum.FRIEND_APPROVE, null, data, bizId);
    }

    public void sendOnlineStatus(List<Long> userIds, Object data, String bizId) {
        sendUsersEvent(userIds, ImWebSocketEventTypeEnum.ONLINE_STATUS, null, data, bizId);
    }

    public void sendWebSocketPush(Long userId, ImWebSocketEventTypeEnum eventType, Long roomId, Object data, String bizId) {
        sendUserEvent(userId, eventType, roomId, data, bizId);
    }

    private void sendUserEvent(Long userId, ImWebSocketEventTypeEnum eventType, Long roomId, Object data, String bizId) {
        if (userId == null) {
            log.warn("[ChatMqProducer] 用户ID为空，跳过发送事件 {}", eventType.getCode());
            return;
        }
        sendUsersEvent(List.of(userId), eventType, roomId, data, bizId);
    }

    private void sendUsersEvent(List<Long> userIds, ImWebSocketEventTypeEnum eventType, Long roomId, Object data, String bizId) {
        if (userIds == null || userIds.isEmpty() || eventType == null) {
            return;
        }
        try {
            ImWebSocketEvent event = buildEvent(eventType, roomId, data, bizId);
            WebSocketMessage wsMessage;
            if (userIds.size() == 1) {
                wsMessage = WebSocketMessage.builder()
                        .userId(userIds.get(0))
                        .pushType(WebSocketPushTypeEnum.SINGLE.getValue())
                        .type(WebSocketMessageTypeEnum.MESSAGE.getCode())
                        .bizId(bizId)
                        .roomId(roomId)
                        .data(event)
                        .build();
            } else {
                wsMessage = WebSocketMessage.builder()
                        .userIds(userIds)
                        .pushType(WebSocketPushTypeEnum.MULTIPLE.getValue())
                        .type(WebSocketMessageTypeEnum.MESSAGE.getCode())
                        .bizId(bizId)
                        .roomId(roomId)
                        .data(event)
                        .build();
            }
            mqSender.send(MqBizTypeEnum.WEBSOCKET_PUSH, bizId, wsMessage);
        } catch (Exception e) {
            log.error("[ChatMqProducer] 发送 WebSocket 事件失败, type={}, bizId={}", eventType.getCode(), bizId, e);
        }
    }

    private void sendRoomEvent(Long roomId, ImWebSocketEventTypeEnum eventType, Object data, String bizId) {
        if (roomId == null || eventType == null) {
            return;
        }
        try {
            ImWebSocketEvent event = buildEvent(eventType, roomId, data, bizId);
            WebSocketMessage wsMessage = WebSocketMessage.builder()
                    .roomId(roomId)
                    .pushType(WebSocketPushTypeEnum.BROADCAST.getValue())
                    .type(WebSocketMessageTypeEnum.MESSAGE.getCode())
                    .bizId(bizId)
                    .data(event)
                    .build();
            mqSender.send(MqBizTypeEnum.CHAT_MESSAGE_PUSH, bizId, wsMessage);
        } catch (Exception e) {
            log.error("[ChatMqProducer] 发送房间 WebSocket 事件失败, type={}, roomId={}, bizId={}",
                    eventType.getCode(), roomId, bizId, e);
        }
    }

    private ImWebSocketEvent buildEvent(ImWebSocketEventTypeEnum eventType, Long roomId, Object data, String bizId) {
        return ImWebSocketEvent.builder()
                .type(eventType.getCode())
                .bizId(bizId)
                .roomId(roomId)
                .data(data)
                .build();
    }
}
