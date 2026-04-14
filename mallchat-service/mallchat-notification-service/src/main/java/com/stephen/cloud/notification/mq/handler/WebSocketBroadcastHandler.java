package com.stephen.cloud.notification.mq.handler;

import cn.hutool.json.JSONUtil;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqHandler;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import com.stephen.cloud.common.rabbitmq.model.WebSocketMessage;
import com.stephen.cloud.common.websocket.manager.ChannelManager;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * WebSocket 广播处理器 (全服广播或跨实例分发)
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class WebSocketBroadcastHandler implements RabbitMqHandler<WebSocketMessage> {

    @Resource
    private ChannelManager channelManager;

    @Override
    public String getBizType() {
        return MqBizTypeEnum.WEBSOCKET_BROADCAST.getValue();
    }

    @Override
    public void onMessage(WebSocketMessage wsMessage, RabbitMessage rabbitMessage) throws Exception {
        String msgId = rabbitMessage.getMsgId();
        log.info("[WebSocketBroadcastHandler] 收到 WebSocket 广播/中转消息, msgId: {}", msgId);

        String messageJson = JSONUtil.toJsonStr(wsMessage.getData() != null ? wsMessage.getData() : wsMessage);

        // 1. 如果是特定单用户推送（中转过来的）
        if (wsMessage.getUserId() != null) {
            String userIdStr = String.valueOf(wsMessage.getUserId());
            int successCount = channelManager.writeToUser(userIdStr, messageJson);
            if (successCount > 0) {
                log.info("[WebSocketBroadcastHandler] 中转推送成功, userId: {}, connections: {}, msgId: {}",
                        userIdStr, successCount, msgId);
            }
            return;
        }

        // 2. 如果是特定多用户推送
        if (wsMessage.getUserIds() != null && !wsMessage.getUserIds().isEmpty()) {
            wsMessage.getUserIds().forEach(uid ->
                    channelManager.writeToUser(String.valueOf(uid), messageJson));
            return;
        }

        // 3. 全服广播
        channelManager.getAllChannels().writeAndFlush(new TextWebSocketFrame(messageJson));
        log.info("[WebSocketBroadcastHandler] 全服广播成功, 在线人数: {}, msgId: {}",
                channelManager.getOnlineCount(), msgId);
    }

    @Override
    public Class<WebSocketMessage> getDataType() {
        return WebSocketMessage.class;
    }
}
