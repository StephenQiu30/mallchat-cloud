package com.stephen.cloud.common.websocket.manager;

import com.stephen.cloud.common.cache.constants.ChatCacheConstant;
import com.stephen.cloud.common.cache.utils.CacheUtils;
import com.stephen.cloud.common.constants.WebSocketConstant;
import com.stephen.cloud.common.rabbitmq.enums.ImWebSocketEventTypeEnum;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.enums.WebSocketMessageTypeEnum;
import com.stephen.cloud.common.rabbitmq.enums.WebSocketPushTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.ImWebSocketEvent;
import com.stephen.cloud.common.rabbitmq.model.WebSocketMessage;
import com.stephen.cloud.common.rabbitmq.producer.RabbitMqSender;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * WebSocket 连接管理器
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class ChannelManager {

    @Resource
    private CacheUtils cacheUtils;

    @Resource
    private RabbitMqSender rabbitMqSender;

    private Function<Long, Set<Long>> friendIdsResolver = userId -> Collections.emptySet();

    private String serverId;

    /**
     * 本地连接映射：userId -> (channelId -> Channel)
     */
    private final Map<String, Map<String, Channel>> userChannelMap = new ConcurrentHashMap<>();

    /**
     * 本地连接映射：channelId -> userId
     */
    private final Map<String, String> channelUserMap = new ConcurrentHashMap<>();

    /**
     * 本地连接映射：channelId -> distributedConnectionId
     */
    private final Map<String, String> channelConnectionMap = new ConcurrentHashMap<>();

    /**
     * 所有连接的 Channel 组
     */
    private final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    public void addChannel(String userId, Channel channel) {
        if (serverId == null) {
            log.warn("[ChannelManager] serverId 未设置，分布式推送可能受限");
        }

        boolean wasOnline = isUserOnlineDistributed(userId);
        String channelId = channel.id().asLongText();
        String connectionId = buildConnectionId(channel);

        userChannelMap.computeIfAbsent(userId, key -> new ConcurrentHashMap<>()).put(channelId, channel);
        channelUserMap.put(channelId, userId);
        channelConnectionMap.put(channelId, connectionId);
        channels.add(channel);

        persistConnection(userId, connectionId, channelId);
        log.info("[ChannelManager] 用户连接成功, userId: {}, channelId: {}, connectionId: {}",
                userId, channel.id().asShortText(), connectionId);

        if (!wasOnline && isUserOnlineDistributed(userId)) {
            notifyOnlineStatusChanged(userId, true);
        }
    }

    public void removeChannel(Channel channel) {
        String channelId = channel.id().asLongText();
        String userId = channelUserMap.remove(channelId);
        String connectionId = channelConnectionMap.remove(channelId);

        if (userId == null) {
            return;
        }

        Map<String, Channel> channelMap = userChannelMap.get(userId);
        if (channelMap != null) {
            channelMap.remove(channelId);
            if (channelMap.isEmpty()) {
                userChannelMap.remove(userId);
            }
        }
        channels.remove(channel);

        removePersistedConnection(userId, connectionId);
        log.info("[ChannelManager] 用户断开连接, userId: {}, channelId: {}", userId, channel.id().asShortText());

        if (!isUserOnlineDistributed(userId)) {
            notifyOnlineStatusChanged(userId, false);
        }
    }

    public Channel getChannel(String userId) {
        return getChannels(userId).stream().findFirst().orElse(null);
    }

    public List<Channel> getChannels(String userId) {
        Map<String, Channel> channelMap = userChannelMap.get(userId);
        if (channelMap == null || channelMap.isEmpty()) {
            return List.of();
        }
        return channelMap.values().stream()
                .filter(Channel::isActive)
                .toList();
    }

    public boolean isOnline(String userId) {
        return !getChannels(userId).isEmpty();
    }

    public ChannelGroup getAllChannels() {
        return channels;
    }

    public int getOnlineCount() {
        return userChannelMap.values().stream().mapToInt(Map::size).sum();
    }

    public List<String> getOnlineUserIds() {
        return new ArrayList<>(userChannelMap.keySet());
    }

    public void refreshUserConnection(String userId) {
        String userConnectionsKey = WebSocketConstant.WS_USER_CONNECTIONS_KEY + userId;
        Set<String> connectionIds = cacheUtils.sMembers(userConnectionsKey);
        if (connectionIds == null || connectionIds.isEmpty()) {
            return;
        }
        connectionIds.forEach(connectionId -> {
            String metaKey = WebSocketConstant.WS_CONNECTION_META_KEY + connectionId;
            Map<String, String> connectionInfo = cacheUtils.getHash(metaKey);
            if (connectionInfo != null && !connectionInfo.isEmpty()) {
                cacheUtils.setHash(metaKey, connectionInfo, WebSocketConstant.WS_CONNECTION_EXPIRE_SECONDS);
            }
        });
        cacheUtils.expire(userConnectionsKey, WebSocketConstant.WS_CONNECTION_EXPIRE_SECONDS);
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public void setFriendIdsResolver(Function<Long, Set<Long>> friendIdsResolver) {
        this.friendIdsResolver = friendIdsResolver == null ? userId -> Collections.emptySet() : friendIdsResolver;
    }

    public String getUserServerId(String userId) {
        Set<String> connectionIds = cacheUtils.sMembers(WebSocketConstant.WS_USER_CONNECTIONS_KEY + userId);
        if (connectionIds == null || connectionIds.isEmpty()) {
            return null;
        }
        String firstConnectionId = connectionIds.iterator().next();
        return cacheUtils.getHashField(WebSocketConstant.WS_CONNECTION_META_KEY + firstConnectionId, "serverId");
    }

    public int writeToUser(String userId, String messageJson) {
        List<Channel> userChannels = getChannels(userId);
        if (userChannels.isEmpty()) {
            return 0;
        }
        userChannels.forEach(channel -> channel.writeAndFlush(new TextWebSocketFrame(messageJson)));
        return userChannels.size();
    }

    private void persistConnection(String userId, String connectionId, String channelId) {
        String userConnectionsKey = WebSocketConstant.WS_USER_CONNECTIONS_KEY + userId;
        cacheUtils.sAdd(userConnectionsKey, connectionId);
        cacheUtils.expire(userConnectionsKey, WebSocketConstant.WS_CONNECTION_EXPIRE_SECONDS);

        String metaKey = WebSocketConstant.WS_CONNECTION_META_KEY + connectionId;
        Map<String, String> connectionInfo = new HashMap<>();
        connectionInfo.put("channelId", channelId);
        connectionInfo.put("serverId", serverId);
        connectionInfo.put("connectTime", String.valueOf(System.currentTimeMillis()));
        connectionInfo.put("userId", userId);
        cacheUtils.setHash(metaKey, connectionInfo, WebSocketConstant.WS_CONNECTION_EXPIRE_SECONDS);
    }

    private void removePersistedConnection(String userId, String connectionId) {
        if (connectionId == null) {
            return;
        }
        String userConnectionsKey = WebSocketConstant.WS_USER_CONNECTIONS_KEY + userId;
        cacheUtils.sRemove(userConnectionsKey, connectionId);
        cacheUtils.delete(WebSocketConstant.WS_CONNECTION_META_KEY + connectionId);

        Set<String> remains = cacheUtils.sMembers(userConnectionsKey);
        if (remains == null || remains.isEmpty()) {
            cacheUtils.delete(userConnectionsKey);
        }
    }

    private String buildConnectionId(Channel channel) {
        String channelId = channel.id().asLongText();
        return (serverId == null ? "unknown" : serverId) + ":" + channelId;
    }

    private boolean isUserOnlineDistributed(String userId) {
        Set<String> connectionIds = cacheUtils.sMembers(WebSocketConstant.WS_USER_CONNECTIONS_KEY + userId);
        return connectionIds != null && !connectionIds.isEmpty();
    }

    private void notifyOnlineStatusChanged(String userId, boolean online) {
        try {
            Set<Long> targetUserIds = new LinkedHashSet<>();
            Long currentUserId = Long.valueOf(userId);
            targetUserIds.add(currentUserId);
            targetUserIds.addAll(friendIdsResolver.apply(currentUserId));

            ImWebSocketEvent event = ImWebSocketEvent.builder()
                    .type(ImWebSocketEventTypeEnum.ONLINE_STATUS.getCode())
                    .bizId("online_status:" + userId + ":" + (online ? 1 : 0))
                    .data(Map.of("userId", currentUserId, "onlineStatus", online ? 1 : 0))
                    .build();

            WebSocketMessage wsMessage = WebSocketMessage.builder()
                    .userIds(targetUserIds.stream().toList())
                    .pushType(targetUserIds.size() == 1 ? WebSocketPushTypeEnum.SINGLE.getValue() : WebSocketPushTypeEnum.MULTIPLE.getValue())
                    .type(WebSocketMessageTypeEnum.MESSAGE.getCode())
                    .bizId(event.getBizId())
                    .data(event)
                    .build();

            rabbitMqSender.send(MqBizTypeEnum.WEBSOCKET_PUSH, event.getBizId(), wsMessage);
        } catch (Exception e) {
            log.error("[ChannelManager] 广播在线状态变更失败, userId={}, online={}", userId, online, e);
        }
    }
}
