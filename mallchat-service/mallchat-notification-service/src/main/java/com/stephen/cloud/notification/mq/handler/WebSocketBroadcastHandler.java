package com.stephen.cloud.notification.mq.handler;

import cn.hutool.json.JSONUtil;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqHandler;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.enums.WebSocketMessageTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.ImWebSocketEvent;
import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import com.stephen.cloud.common.rabbitmq.model.WebSocketMessage;
import com.stephen.cloud.common.websocket.manager.ChannelManager;
import com.stephen.cloud.notification.mq.support.ImPushMetricsRecorder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

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

    @Resource
    private ImPushMetricsRecorder metricsRecorder;

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
            pushToSingleUser(wsMessage, messageJson, msgId);
            return;
        }

        // 2. 如果是特定多用户推送
        if (wsMessage.getUserIds() != null && !wsMessage.getUserIds().isEmpty()) {
            pushToMultipleUsers(wsMessage, messageJson, msgId);
            return;
        }

        // 3. 全服广播
        channelManager.getAllChannels().writeAndFlush(new TextWebSocketFrame(messageJson));
        int onlineCount = channelManager.getOnlineCount();
        metricsRecorder.record(getBizType(), resolveEventType(wsMessage), "success", onlineCount);
        log.info("[WebSocketBroadcastHandler] 全服广播成功, 在线人数: {}, msgId: {}", onlineCount, msgId);
    }

    private void pushToSingleUser(WebSocketMessage wsMessage, String messageJson, String msgId) {
        String userIdStr = String.valueOf(wsMessage.getUserId());
        int successCount;
        try {
            successCount = channelManager.writeToUser(userIdStr, messageJson);
        } catch (Exception e) {
            metricsRecorder.record(getBizType(), resolveEventType(wsMessage), "failure");
            throw e;
        }
        if (successCount > 0) {
            metricsRecorder.record(getBizType(), resolveEventType(wsMessage), "success", successCount);
            log.info("[WebSocketBroadcastHandler] 中转推送成功, userId: {}, connections: {}, msgId: {}",
                    userIdStr, successCount, msgId);
        } else {
            metricsRecorder.record(getBizType(), resolveEventType(wsMessage), "offline");
        }
    }

    private void pushToMultipleUsers(WebSocketMessage wsMessage, String messageJson, String msgId) {
        List<Long> userIds = wsMessage.getUserIds();
        int successCount = 0;
        int offlineCount = 0;

        for (Long uid : userIds) {
            try {
                int writeCount = channelManager.writeToUser(String.valueOf(uid), messageJson);
                if (writeCount > 0) {
                    successCount += writeCount;
                } else {
                    offlineCount++;
                }
            } catch (Exception e) {
                metricsRecorder.record(getBizType(), resolveEventType(wsMessage), "failure");
                throw e;
            }
        }

        if (successCount > 0) {
            metricsRecorder.record(getBizType(), resolveEventType(wsMessage), "success", successCount);
        }
        if (offlineCount > 0) {
            metricsRecorder.record(getBizType(), resolveEventType(wsMessage), "offline", offlineCount);
        }
        log.info("[WebSocketBroadcastHandler] 多用户推送完成, success: {}, offline: {}, msgId: {}",
                successCount, offlineCount, msgId);
    }

    private String resolveEventType(WebSocketMessage wsMessage) {
        Object data = wsMessage.getData();
        if (data instanceof ImWebSocketEvent event) {
            return event.getType();
        }
        WebSocketMessageTypeEnum typeEnum = WebSocketMessageTypeEnum.getEnumByCode(wsMessage.getType());
        return typeEnum == null ? String.valueOf(wsMessage.getType()) : typeEnum.name();
    }

    @Override
    public Class<WebSocketMessage> getDataType() {
        return WebSocketMessage.class;
    }
}
