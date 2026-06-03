package com.stephen.cloud.common.websocket.manager;

import com.stephen.cloud.common.rabbitmq.model.ImWebSocketEvent;
import com.stephen.cloud.common.rabbitmq.model.WebSocketMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 在线状态通知规划器
 * 负责合并通知目标、构建去重 ID 和构建消息体
 *
 * @author StephenQiu30
 */
public class OnlineStatusNotificationPlanner {

    private static final int WS_TYPE_ONLINE_STATUS = 10;

    /**
     * 合并通知目标用户集合（包含当前用户和好友）
     *
     * @param userId    当前用户 ID
     * @param friendIds 好友 ID 集合
     * @return 需要通知的目标用户 ID 集合
     */
    public static Set<Long> mergeNotificationTargets(Long userId, Set<Long> friendIds) {
        Set<Long> targets = new HashSet<>();
        targets.add(userId);
        if (friendIds != null) {
            targets.addAll(friendIds);
        }
        return targets;
    }

    /**
     * 构建去重 ID
     *
     * @param userId 用户 ID
     * @param online 在线状态
     * @return 去重 ID
     */
    public static String buildDedupeId(Long userId, boolean online) {
        return "online_status:" + userId + ":" + (online ? "1" : "0");
    }

    /**
     * 构建在线状态变更消息
     *
     * @param userId       用户 ID
     * @param online       在线状态
     * @param targetUserIds 目标用户 ID 集合
     * @param dedupeId     去重 ID
     * @return WebSocket 消息
     */
    public static WebSocketMessage buildMessage(Long userId, boolean online, Set<Long> targetUserIds, String dedupeId) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("onlineStatus", online ? 1 : 0);

        ImWebSocketEvent event = ImWebSocketEvent.builder()
                .type("online_status")
                .bizId(dedupeId)
                .data(data)
                .build();

        return WebSocketMessage.builder()
                .type(WS_TYPE_ONLINE_STATUS)
                .data(event)
                .userIds(new ArrayList<>(targetUserIds))
                .pushType("multiple")
                .bizType("online_status")
                .bizId(dedupeId)
                .dedupeId(dedupeId)
                .build();
    }
}
