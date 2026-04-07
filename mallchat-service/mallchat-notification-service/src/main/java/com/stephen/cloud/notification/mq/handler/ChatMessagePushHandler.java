package com.stephen.cloud.notification.mq.handler;

import cn.hutool.json.JSONUtil;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import com.stephen.cloud.common.rabbitmq.model.WebSocketMessage;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqHandler;
import com.stephen.cloud.common.websocket.manager.ChannelManager;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 聊天消息推送处理器
 * <p>
 * 负责消费 CHAT_MESSAGE_PUSH 类型的消息。
 * 将聊天消息推送到对应的房间成员。
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class ChatMessagePushHandler implements RabbitMqHandler<WebSocketMessage> {

    @Resource
    private ChannelManager channelManager;

    @Override
    public String getBizType() {
        return MqBizTypeEnum.CHAT_MESSAGE_PUSH.getValue();
    }

    /**
     * 处理 MQ 消息回调
     *
     * @param wsMessage      WebSocket 包装消息
     * @param rabbitMessage  MQ 原始消息 (含元数据)
     * @throws Exception     处理异常
     */
    @Override
    public void onMessage(WebSocketMessage wsMessage, RabbitMessage rabbitMessage) throws Exception {
        String msgId = rabbitMessage.getMsgId();
        List<Long> userIds = wsMessage.getUserIds();

        log.info("[ChatMessagePushHandler] 收到聊天消息推送, 目标用户数: {}, msgId: {}", 
                userIds != null ? userIds.size() : 0, msgId);

        if ("broadcast".equalsIgnoreCase(wsMessage.getPushType())) {
            log.info("[ChatMessagePushHandler] 收到广播消息推送, msgId: {}", msgId);
            broadcast(wsMessage);
            return;
        }

        if (userIds == null || userIds.isEmpty()) {
            log.warn("[ChatMessagePushHandler] 消息中没有指定用户ID且非广播，忽略推送, msgId: {}", msgId);
            return;
        }

        pushToMultipleUsers(wsMessage);
    }

    @Override
    public Class<WebSocketMessage> getDataType() {
        return WebSocketMessage.class;
    }

    /**
     * 将消息推送到本地服务器实例中在线的所有目标成员
     *
     * @param wsMessage WebSocket 包装消息
     */
    private void pushToMultipleUsers(WebSocketMessage wsMessage) {
        List<Long> userIds = wsMessage.getUserIds();
        String messageJson = JSONUtil.toJsonStr(wsMessage);
        int successCount = 0;

        for (Long userId : userIds) {
            String userIdStr = String.valueOf(userId);
            // 只推送到本地在线的用户
            io.netty.channel.Channel channel = channelManager.getChannel(userIdStr);
            if (channel != null && channel.isActive()) {
                channel.writeAndFlush(new TextWebSocketFrame(messageJson));
                successCount++;
            }
        }

        if (successCount > 0) {
            log.info("[ChatMessagePushHandler] 成功向 {} 个本地在线用户推送聊天消息", successCount);
        }
    }

    /**
     * 广播消息给本地服务器上的所有在线用户
     *
     * @param wsMessage WebSocket 包装消息
     */
    private void broadcast(WebSocketMessage wsMessage) {
        String messageJson = JSONUtil.toJsonStr(wsMessage);
        channelManager.getAllChannels().writeAndFlush(new TextWebSocketFrame(messageJson));
        log.info("[ChatMessagePushHandler] 已完成本地全量广播推送");
    }
}
