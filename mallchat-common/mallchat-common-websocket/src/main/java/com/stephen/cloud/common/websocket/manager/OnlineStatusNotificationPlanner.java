package com.stephen.cloud.common.websocket.manager;

import com.stephen.cloud.common.rabbitmq.enums.ImWebSocketEventTypeEnum;
import com.stephen.cloud.common.rabbitmq.enums.WebSocketMessageTypeEnum;
import com.stephen.cloud.common.rabbitmq.enums.WebSocketPushTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.ImWebSocketEvent;
import com.stephen.cloud.common.rabbitmq.model.WebSocketMessage;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 在线状态通知规划器：批量合并推送目标并生成稳定去重 ID。
 */
final class OnlineStatusNotificationPlanner {

    static final String DEDUPE_ID_PREFIX = "online_status:";

    private OnlineStatusNotificationPlanner() {
    }

    static String buildDedupeId(long userId, boolean online) {
        return DEDUPE_ID_PREFIX + userId + ":" + (online ? 1 : 0);
    }

    static Set<Long> mergeNotificationTargets(long userId, Set<Long> friendIds) {
        Set<Long> targets = new LinkedHashSet<>();
        targets.add(userId);
        if (friendIds != null && !friendIds.isEmpty()) {
            targets.addAll(friendIds);
        }
        return targets;
    }

    static WebSocketMessage buildMessage(long userId, boolean online, Set<Long> targetUserIds, String dedupeId) {
        ImWebSocketEvent event = ImWebSocketEvent.builder()
                .type(ImWebSocketEventTypeEnum.ONLINE_STATUS.getCode())
                .bizId(dedupeId)
                .data(Map.of("userId", userId, "onlineStatus", online ? 1 : 0))
                .build();

        return WebSocketMessage.builder()
                .userIds(targetUserIds.stream().toList())
                .pushType(targetUserIds.size() == 1
                        ? WebSocketPushTypeEnum.SINGLE.getValue()
                        : WebSocketPushTypeEnum.MULTIPLE.getValue())
                .type(WebSocketMessageTypeEnum.MESSAGE.getCode())
                .bizId(dedupeId)
                .dedupeId(dedupeId)
                .data(event)
                .build();
    }
}
