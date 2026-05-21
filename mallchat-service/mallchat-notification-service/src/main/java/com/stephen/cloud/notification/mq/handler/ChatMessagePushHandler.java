package com.stephen.cloud.notification.mq.handler;

import cn.hutool.json.JSONUtil;
import com.stephen.cloud.common.cache.constants.ChatCacheConstant;
import com.stephen.cloud.common.cache.utils.CacheUtils;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqHandler;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.enums.WebSocketMessageTypeEnum;
import com.stephen.cloud.common.rabbitmq.enums.WebSocketPushTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import com.stephen.cloud.common.rabbitmq.model.WebSocketMessage;
import com.stephen.cloud.common.websocket.manager.ChannelManager;
import com.stephen.cloud.common.rabbitmq.model.ImWebSocketEvent;
import com.stephen.cloud.notification.mq.support.ImPushMetricsRecorder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Resource
    private CacheUtils cacheUtils;

    @Resource
    private ImPushMetricsRecorder metricsRecorder;

    @Override
    public String getBizType() {
        return MqBizTypeEnum.CHAT_MESSAGE_PUSH.getValue();
    }

    /**
     * 处理 MQ 消息回调
     *
     * @param wsMessage     WebSocket 包装消息
     * @param rabbitMessage MQ 原始消息 (含元数据)
     * @throws Exception 处理异常
     */
    @Override
    public void onMessage(WebSocketMessage wsMessage, RabbitMessage rabbitMessage) throws Exception {
        String msgId = rabbitMessage.getMsgId();
        String pushType = wsMessage.getPushType();

        log.info("[ChatMessagePushHandler] 收到推送请求, type: {}, roomId: {}, msgId: {}",
                pushType, wsMessage.getRoomId(), msgId);

        if (WebSocketPushTypeEnum.BROADCAST.getValue().equalsIgnoreCase(pushType)) {
            // 如果指定了房间 ID，进行房间级别的准广播 (只推送到房间成员)
            if (wsMessage.getRoomId() != null) {
                pushToRoomMembers(wsMessage);
            } else {
                // 如果没有房间 ID，进行全量广播 (系统级通知)
                broadcast(wsMessage);
            }
            return;
        }

        // 定点单发或多发模式 (MULTIPLE / SINGLE)
        List<Long> userIds = wsMessage.getUserIds();
        if (userIds == null || userIds.isEmpty()) {
            log.warn("[ChatMessagePushHandler] 消息中没有指定用户ID且非广播，忽略推送, msgId: {}", msgId);
            metricsRecorder.record(getBizType(), resolveEventType(wsMessage), "skipped");
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
            log.debug("[ChatMessagePushHandler] 向 {} 个本地用户推送成功", successCount);
        }
        if (offlineCount > 0) {
            metricsRecorder.record(getBizType(), resolveEventType(wsMessage), "offline", offlineCount);
        }
    }

    /**
     * 定向推送给房间内的本地在线成员
     * 逻辑：从 Redis 获取房间成员，然后在本地 ChannelManager 中寻找匹配的活跃连接进行发送
     *
     * @param wsMessage WebSocket 包装消息
     */
    private void pushToRoomMembers(WebSocketMessage wsMessage) {
        Long roomId = wsMessage.getRoomId();
        String messageJson = JSONUtil.toJsonStr(wsMessage.getData() != null ? wsMessage.getData() : wsMessage);

        String key = ChatCacheConstant.getRoomMemberKey(roomId);
        Set<String> memberIds = cacheUtils.sMembers(key);
        Set<String> snapshotMemberIds = resolveSnapshotMemberIds(wsMessage);

        if (memberIds == null || memberIds.isEmpty()) {
            memberIds = snapshotMemberIds;
            if (memberIds.isEmpty()) {
                log.warn("[ChatMessagePushHandler] 房间 {} 缓存和消息成员快照均为空，跳过推送", roomId);
                metricsRecorder.record(getBizType(), resolveEventType(wsMessage), "skipped");
                return;
            }
            log.warn("[ChatMessagePushHandler] 房间 {} 缓存中没有成员，使用消息成员快照兜底推送", roomId);
        } else if (!snapshotMemberIds.isEmpty()) {
            memberIds = memberIds.stream()
                    .filter(snapshotMemberIds::contains)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (memberIds.isEmpty()) {
                log.warn("[ChatMessagePushHandler] 房间 {} 成员缓存与消息成员快照无交集，跳过推送", roomId);
                metricsRecorder.record(getBizType(), resolveEventType(wsMessage), "skipped");
                return;
            }
        }

        int successCount = 0;
        int offlineCount = 0;
        for (String userIdStr : memberIds) {
            try {
                int writeCount = channelManager.writeToUser(userIdStr, messageJson);
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
            log.info("[ChatMessagePushHandler] 房间 {} 推送成功, 本地在线接收者: {}/{}", roomId, successCount, memberIds.size());
        }
        if (offlineCount > 0) {
            metricsRecorder.record(getBizType(), resolveEventType(wsMessage), "offline", offlineCount);
        }
    }

    private Set<String> resolveSnapshotMemberIds(WebSocketMessage wsMessage) {
        List<Long> userIds = wsMessage.getUserIds();
        if (userIds == null || userIds.isEmpty()) {
            return Set.of();
        }
        return userIds.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String resolveEventType(WebSocketMessage wsMessage) {
        Object data = wsMessage.getData();
        if (data instanceof ImWebSocketEvent event) {
            return event.getType();
        }
        WebSocketMessageTypeEnum typeEnum = WebSocketMessageTypeEnum.getEnumByCode(wsMessage.getType());
        return typeEnum == null ? String.valueOf(wsMessage.getType()) : typeEnum.name();
    }

    /**
     * 广播消息给本地服务器上的所有在线用户 (全量广播)
     *
     * @param wsMessage WebSocket 包装消息
     */
    private void broadcast(WebSocketMessage wsMessage) {
        String messageJson = JSONUtil.toJsonStr(wsMessage.getData() != null ? wsMessage.getData() : wsMessage);
        channelManager.getAllChannels().writeAndFlush(new TextWebSocketFrame(messageJson));
        log.info("[ChatMessagePushHandler] 已完成本地全量广播推送");
    }
}
