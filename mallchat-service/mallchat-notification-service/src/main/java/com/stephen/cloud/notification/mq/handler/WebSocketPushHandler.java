package com.stephen.cloud.notification.mq.handler;

import cn.hutool.json.JSONUtil;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqHandler;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.enums.WebSocketMessageTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import com.stephen.cloud.common.rabbitmq.model.WebSocketMessage;
import com.stephen.cloud.common.websocket.manager.ChannelManager;
import com.stephen.cloud.common.rabbitmq.model.ImWebSocketEvent;
import com.stephen.cloud.notification.mq.support.ImPushMetricsRecorder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * WebSocket 通用推送处理器
 * <p>
 * 负责消费 WEBSOCKET_PUSH 类型的消息。
 * 支持多种推送模式：
 * 1. <b>单用户模式</b>：指定单个 userId 时的点对点推送。
 * 2. <b>多用户模式</b>：指定 userIds 集合时的本地实例在线用户组播。
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class WebSocketPushHandler implements RabbitMqHandler<WebSocketMessage> {

    @Resource
    private ChannelManager channelManager;

    @Resource
    private ImPushMetricsRecorder metricsRecorder;

    @Override
    public String getBizType() {
        return MqBizTypeEnum.WEBSOCKET_PUSH.getValue();
    }

    @Override
    public void onMessage(WebSocketMessage wsMessage, RabbitMessage rabbitMessage) throws Exception {
        String msgId = rabbitMessage.getMsgId();

        log.info("[WebSocketPushHandler] 收到 WebSocket 推送消息, userId: {}, msgId: {}", wsMessage.getUserId(), msgId);

        if (wsMessage.getUserId() != null) {
            pushToSingleUser(wsMessage);
        } else if (wsMessage.getUserIds() != null && !wsMessage.getUserIds().isEmpty()) {
            pushToMultipleUsers(wsMessage);
        } else {
            log.warn("[WebSocketPushHandler] 消息中没有指定用户ID，忽略推送, msgId: {}", msgId);
            metricsRecorder.record(getBizType(), resolveEventType(wsMessage), "skipped");
        }
    }

    @Override
    public Class<WebSocketMessage> getDataType() {
        return WebSocketMessage.class;
    }

    private void pushToSingleUser(WebSocketMessage wsMessage) {
        Long userId = wsMessage.getUserId();
        String userIdStr = String.valueOf(userId);
        String messageJson = JSONUtil.toJsonStr(wsMessage.getData() != null ? wsMessage.getData() : wsMessage);
        int successCount;
        try {
            successCount = channelManager.writeToUser(userIdStr, messageJson);
        } catch (Exception e) {
            metricsRecorder.record(getBizType(), resolveEventType(wsMessage), "failure");
            throw e;
        }
        if (successCount > 0) {
            metricsRecorder.record(getBizType(), resolveEventType(wsMessage), "success", successCount);
            log.info("[WebSocketPushHandler] 成功向本地用户 {} 的 {} 个连接推送 WebSocket 消息", userId, successCount);
        } else {
            metricsRecorder.record(getBizType(), resolveEventType(wsMessage), "offline");
        }
    }

    private void pushToMultipleUsers(WebSocketMessage wsMessage) {
        List<Long> userIds = wsMessage.getUserIds();
        String messageJson = JSONUtil.toJsonStr(wsMessage.getData() != null ? wsMessage.getData() : wsMessage);
        int successCount = 0;
        int offlineCount = 0;

        for (Long userId : userIds) {
            try {
                int writeCount = channelManager.writeToUser(String.valueOf(userId), messageJson);
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
        log.info("[WebSocketPushHandler] 成功向 {} 个本地在线用户推送消息 (目标用户总数: {})",
                successCount, userIds.size());
    }

    private String resolveEventType(WebSocketMessage wsMessage) {
        Object data = wsMessage.getData();
        if (data instanceof ImWebSocketEvent event) {
            return event.getType();
        }
        WebSocketMessageTypeEnum typeEnum = WebSocketMessageTypeEnum.getEnumByCode(wsMessage.getType());
        return typeEnum == null ? String.valueOf(wsMessage.getType()) : typeEnum.name();
    }
}
