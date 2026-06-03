package com.stephen.cloud.common.websocket.manager;

import com.stephen.cloud.common.cache.utils.CacheUtils;
import com.stephen.cloud.common.constants.WebSocketConstant;
import com.stephen.cloud.common.websocket.handler.DisconnectReason;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.ImWebSocketEvent;
import com.stephen.cloud.common.rabbitmq.model.WebSocketMessage;
import com.stephen.cloud.common.rabbitmq.producer.RabbitMqSender;
import io.netty.channel.DefaultChannelId;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class ChannelManagerTest {

    private ChannelManager channelManager;
    private FakeCacheUtils cacheUtils;
    private RecordingRabbitMqSender rabbitMqSender;

    @BeforeEach
    void setUp() {
        channelManager = new ChannelManager();
        cacheUtils = new FakeCacheUtils();
        rabbitMqSender = new RecordingRabbitMqSender();
        ReflectionTestUtils.setField(channelManager, "cacheUtils", cacheUtils);
        ReflectionTestUtils.setField(channelManager, "rabbitMqSender", rabbitMqSender);
        channelManager.setServerId("server-a");
        channelManager.setFriendIdsResolver(userId -> Set.of());
    }

    @Test
    void shouldNotifyFriendsWhenFirstConnectionComesOnline() {
        channelManager.setFriendIdsResolver(userId -> Set.of(2L));
        EmbeddedChannel channel = newChannel();

        channelManager.addChannel("1", channel);

        Assertions.assertEquals(MqBizTypeEnum.WEBSOCKET_PUSH, rabbitMqSender.lastBizType);
        Assertions.assertEquals("online_status:1:1", rabbitMqSender.lastMsgId);
        WebSocketMessage message = (WebSocketMessage) rabbitMqSender.lastPayload;
        Assertions.assertTrue(message.getUserIds().containsAll(Set.of(1L, 2L)));
        Assertions.assertEquals("online_status:1:1", message.getDedupeId());
        ImWebSocketEvent event = (ImWebSocketEvent) message.getData();
        Map<?, ?> data = (Map<?, ?>) event.getData();
        Assertions.assertEquals(1L, data.get("userId"));
        Assertions.assertEquals(1, data.get("onlineStatus"));
        channel.close();
    }

    @Test
    void shouldOnlyNotifyOfflineWhenLastConnectionIsRemoved() {
        channelManager.setFriendIdsResolver(userId -> Set.of(2L));
        EmbeddedChannel channel = newChannel();

        channelManager.addChannel("1", channel);
        rabbitMqSender.clear();

        cacheUtils.setMembers(WebSocketConstant.WS_USER_CONNECTIONS_KEY + 1L, Set.of("server-a:embedded", "remote-conn"));
        channelManager.removeChannel(channel);
        Assertions.assertNull(rabbitMqSender.lastPayload);

        cacheUtils.setMembers(WebSocketConstant.WS_USER_CONNECTIONS_KEY + 1L, Set.of());
        ReflectionTestUtils.invokeMethod(channelManager, "notifyOnlineStatusChanged", "1", false);
        WebSocketMessage message = (WebSocketMessage) rabbitMqSender.lastPayload;
        ImWebSocketEvent event = (ImWebSocketEvent) message.getData();
        Map<?, ?> data = (Map<?, ?>) event.getData();
        Assertions.assertEquals(0, data.get("onlineStatus"));
    }

    @Test
    void shouldSkipDuplicateOnlineStatusNotificationWithinDedupWindow() {
        channelManager.setOnlineStatusPublishDedupeTtlMillis(60_000L);

        ReflectionTestUtils.invokeMethod(channelManager, "notifyOnlineStatusChanged", "1", true);
        ReflectionTestUtils.invokeMethod(channelManager, "notifyOnlineStatusChanged", "1", true);

        Assertions.assertEquals(1, rabbitMqSender.sendCount);
    }

    @Test
    void shouldBatchMergeFriendIdsIntoSingleMqMessage() {
        channelManager.setFriendIdsResolver(userId -> Set.of(2L, 3L, 4L));
        EmbeddedChannel channel = newChannel();

        channelManager.addChannel("1", channel);

        WebSocketMessage message = (WebSocketMessage) rabbitMqSender.lastPayload;
        Assertions.assertEquals(4, message.getUserIds().size());
        Assertions.assertEquals(Set.of(1L, 2L, 3L, 4L), new HashSet<>(message.getUserIds()));
        Assertions.assertEquals(1, rabbitMqSender.sendCount);
        channel.close();
    }

    @Test
    void shouldHandleEmptyFriendResolverResult() {
        EmbeddedChannel channel = newChannel();

        channelManager.addChannel("1", channel);

        WebSocketMessage message = (WebSocketMessage) rabbitMqSender.lastPayload;
        Assertions.assertEquals(Set.of(1L), new HashSet<>(message.getUserIds()));
        channel.close();
    }

    @Test
    void shouldRejectNewChannelWhenUserConnectionLimitExceeded() {
        channelManager.setRuntimeGuard(1, 0L);
        EmbeddedChannel first = newChannel();
        EmbeddedChannel second = newChannel();

        Assertions.assertTrue(channelManager.addChannel("1", first));
        Assertions.assertFalse(channelManager.addChannel("1", second));

        Assertions.assertEquals(1, channelManager.getChannels("1").size());
        Assertions.assertEquals(1L, channelManager.getRejectedConnectionCount());
        Assertions.assertFalse(second.isActive());
        first.close();
    }

    @Test
    void shouldKeepConnectionLimitWhenHandshakeCompletesConcurrently() throws InterruptedException {
        channelManager.setRuntimeGuard(1, 0L);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger acceptedCount = new AtomicInteger();
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        for (int i = 0; i < 2; i++) {
            executorService.submit(() -> {
                EmbeddedChannel channel = newChannel();
                try {
                    startLatch.await();
                    if (channelManager.addChannel("1", channel)) {
                        acceptedCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        startLatch.countDown();
        executorService.shutdown();
        Assertions.assertTrue(executorService.awaitTermination(5, TimeUnit.SECONDS));
        Assertions.assertEquals(1, acceptedCount.get());
        Assertions.assertEquals(1, channelManager.getChannels("1").size());
        Assertions.assertEquals(1L, channelManager.getRejectedConnectionCount());
    }

    @Test
    void shouldRejectRepeatedConnectionWithinGuardWindow() {
        channelManager.setRuntimeGuard(5, 60_000L);
        EmbeddedChannel first = newChannel();
        EmbeddedChannel second = newChannel();

        Assertions.assertTrue(channelManager.addChannel("1", first));
        channelManager.removeChannel(first);
        Assertions.assertFalse(channelManager.addChannel("1", second));

        Assertions.assertEquals(1L, channelManager.getRejectedConnectionCount());
        Assertions.assertFalse(second.isActive());
    }

    @Test
    void shouldNotAuditGuardRejectedChannelAsAbnormalDisconnect() {
        channelManager.setRuntimeGuard(1, 0L);
        EmbeddedChannel first = newChannel();
        EmbeddedChannel second = newChannel();

        Assertions.assertTrue(channelManager.addChannel("1", first));
        Assertions.assertFalse(channelManager.addChannel("1", second));
        channelManager.removeChannel(second);

        Assertions.assertEquals(1L, channelManager.getRejectedConnectionCount());
        Assertions.assertEquals(0L, channelManager.getAbnormalDisconnectCount());
        first.close();
    }

    @Test
    void shouldAuditUnknownChannelDisconnect() {
        EmbeddedChannel channel = newChannel();

        channelManager.removeChannel(channel);

        Assertions.assertEquals(1L, channelManager.getAbnormalDisconnectCount());
    }

    @Test
    void shouldRebuildRedisConnectionStateWhenHeartbeatFindsCacheMissing() {
        EmbeddedChannel channel = newChannel();
        channelManager.addChannel("1", channel);
        rabbitMqSender.clear();
        cacheUtils.clear();

        channelManager.refreshUserConnection("1");

        Set<String> connectionIds = cacheUtils.sMembers(WebSocketConstant.WS_USER_CONNECTIONS_KEY + 1L);
        Assertions.assertEquals(1, connectionIds.size());
        String connectionId = connectionIds.iterator().next();
        Assertions.assertEquals("1", cacheUtils.getHashField(WebSocketConstant.WS_CONNECTION_META_KEY + connectionId, "userId"));
        Assertions.assertEquals("server-a", cacheUtils.getHashField(WebSocketConstant.WS_CONNECTION_META_KEY + connectionId, "serverId"));
        channel.close();
    }

    @Test
    void shouldRebuildRedisConnectionMetaWhenHeartbeatFindsMetaMissing() {
        EmbeddedChannel channel = newChannel();
        channelManager.addChannel("1", channel);
        Set<String> connectionIds = cacheUtils.sMembers(WebSocketConstant.WS_USER_CONNECTIONS_KEY + 1L);
        String connectionId = connectionIds.iterator().next();
        cacheUtils.clearHash(WebSocketConstant.WS_CONNECTION_META_KEY + connectionId);

        channelManager.refreshUserConnection("1");

        Assertions.assertEquals("1", cacheUtils.getHashField(WebSocketConstant.WS_CONNECTION_META_KEY + connectionId, "userId"));
        Assertions.assertEquals("server-a", cacheUtils.getHashField(WebSocketConstant.WS_CONNECTION_META_KEY + connectionId, "serverId"));
        channel.close();
    }

    // ---- disconnect reason metrics tests ----

    @Test
    void shouldTrackTimeoutDisconnect() {
        EmbeddedChannel channel = newChannel();
        channelManager.addChannel("1", channel);

        channelManager.removeChannel(channel, DisconnectReason.TIMEOUT);

        Assertions.assertEquals(1L, channelManager.getDisconnectCount(DisconnectReason.TIMEOUT));
        Assertions.assertEquals(0L, channelManager.getDisconnectCount(DisconnectReason.EXCEPTION));
        Assertions.assertEquals(0L, channelManager.getDisconnectCount(DisconnectReason.CLIENT_CLOSE));
        Assertions.assertEquals(0L, channelManager.getDisconnectCount(DisconnectReason.SERVER_CLOSE));
    }

    @Test
    void shouldTrackExceptionDisconnect() {
        EmbeddedChannel channel = newChannel();
        channelManager.addChannel("1", channel);

        channelManager.removeChannel(channel, DisconnectReason.EXCEPTION);

        Assertions.assertEquals(1L, channelManager.getDisconnectCount(DisconnectReason.EXCEPTION));
        Assertions.assertEquals(0L, channelManager.getDisconnectCount(DisconnectReason.TIMEOUT));
    }

    @Test
    void shouldTrackClientCloseDisconnect() {
        EmbeddedChannel channel = newChannel();
        channelManager.addChannel("1", channel);

        channelManager.removeChannel(channel, DisconnectReason.CLIENT_CLOSE);

        Assertions.assertEquals(1L, channelManager.getDisconnectCount(DisconnectReason.CLIENT_CLOSE));
    }

    @Test
    void shouldTrackServerCloseDisconnect() {
        EmbeddedChannel channel = newChannel();
        channelManager.addChannel("1", channel);

        channelManager.removeChannel(channel, DisconnectReason.SERVER_CLOSE);

        Assertions.assertEquals(1L, channelManager.getDisconnectCount(DisconnectReason.SERVER_CLOSE));
    }

    @Test
    void shouldReturnZeroForAllReasonsWhenNoDisconnects() {
        Assertions.assertEquals(0L, channelManager.getDisconnectCount(DisconnectReason.TIMEOUT));
        Assertions.assertEquals(0L, channelManager.getDisconnectCount(DisconnectReason.EXCEPTION));
        Assertions.assertEquals(0L, channelManager.getDisconnectCount(DisconnectReason.CLIENT_CLOSE));
        Assertions.assertEquals(0L, channelManager.getDisconnectCount(DisconnectReason.SERVER_CLOSE));
    }

    @Test
    void shouldReturnDisconnectReasonMetricsSnapshot() {
        EmbeddedChannel ch1 = newChannel();
        EmbeddedChannel ch2 = newChannel();
        channelManager.addChannel("1", ch1);
        channelManager.addChannel("2", ch2);

        channelManager.removeChannel(ch1, DisconnectReason.TIMEOUT);
        channelManager.removeChannel(ch2, DisconnectReason.EXCEPTION);

        Map<DisconnectReason, Long> metrics = channelManager.getDisconnectReasonMetrics();
        Assertions.assertEquals(1L, metrics.get(DisconnectReason.TIMEOUT));
        Assertions.assertEquals(1L, metrics.get(DisconnectReason.EXCEPTION));
        Assertions.assertEquals(0L, metrics.get(DisconnectReason.CLIENT_CLOSE));
        Assertions.assertEquals(0L, metrics.get(DisconnectReason.SERVER_CLOSE));
    }

    @Test
    void shouldCountMultipleDisconnectsForSameReason() {
        EmbeddedChannel ch1 = newChannel();
        EmbeddedChannel ch2 = newChannel();
        channelManager.addChannel("1", ch1);
        channelManager.addChannel("2", ch2);

        channelManager.removeChannel(ch1, DisconnectReason.TIMEOUT);
        channelManager.removeChannel(ch2, DisconnectReason.TIMEOUT);

        Assertions.assertEquals(2L, channelManager.getDisconnectCount(DisconnectReason.TIMEOUT));
    }

    private static class RecordingRabbitMqSender extends RabbitMqSender {
        private MqBizTypeEnum lastBizType;
        private String lastMsgId;
        private Object lastPayload;
        private int sendCount;

        @Override
        public void send(MqBizTypeEnum bizTypeEnum, String msgId, Object payload) {
            this.lastBizType = bizTypeEnum;
            this.lastMsgId = msgId;
            this.lastPayload = payload;
            this.sendCount++;
        }

        void clear() {
            this.lastBizType = null;
            this.lastMsgId = null;
            this.lastPayload = null;
            this.sendCount = 0;
        }
    }

    private EmbeddedChannel newChannel() {
        return new EmbeddedChannel(DefaultChannelId.newInstance());
    }

    private static class FakeCacheUtils extends CacheUtils {
        private final Map<String, Set<String>> setMap = new HashMap<>();
        private final Map<String, Map<String, String>> hashMap = new HashMap<>();

        FakeCacheUtils() {
            ReflectionTestUtils.setField(this, "redissonClient", createRedissonClient());
        }

        void setMembers(String key, Set<String> values) {
            setMap.put(key, new HashSet<>(values));
        }

        void clear() {
            setMap.clear();
            hashMap.clear();
        }

        void clearHash(String key) {
            hashMap.remove(key);
        }

        @Override
        public <T> Set<T> sMembers(String key) {
            return (Set<T>) setMap.getOrDefault(key, Set.of());
        }

        private RedissonClient createRedissonClient() {
            return (RedissonClient) Proxy.newProxyInstance(
                    RedissonClient.class.getClassLoader(),
                    new Class[]{RedissonClient.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getSet" -> Proxy.newProxyInstance(
                                method.getReturnType().getClassLoader(),
                                new Class[]{method.getReturnType()},
                                (setProxy, setMethod, setArgs) -> switch (setMethod.getName()) {
                                    case "addAll" -> {
                                        Set<String> members = setMap.computeIfAbsent((String) args[0], ignored -> new HashSet<>());
                                        members.addAll(((java.util.Collection<String>) setArgs[0]));
                                        yield true;
                                    }
                                    case "removeAll" -> {
                                        Set<String> members = setMap.computeIfAbsent((String) args[0], ignored -> new HashSet<>());
                                        members.removeAll(((java.util.Collection<String>) setArgs[0]));
                                        yield true;
                                    }
                                    case "readAll" -> new HashSet<>(setMap.getOrDefault((String) args[0], Set.of()));
                                    default -> defaultValue(setMethod.getReturnType());
                                }
                        );
                        case "getBucket" -> Proxy.newProxyInstance(
                                method.getReturnType().getClassLoader(),
                                new Class[]{method.getReturnType()},
                                (bucketProxy, bucketMethod, bucketArgs) -> switch (bucketMethod.getName()) {
                                    case "delete" -> {
                                        setMap.remove((String) args[0]);
                                        hashMap.remove((String) args[0]);
                                        yield true;
                                    }
                                    case "expire" -> true;
                                    default -> defaultValue(bucketMethod.getReturnType());
                                }
                        );
                        case "getMap" -> Proxy.newProxyInstance(
                                method.getReturnType().getClassLoader(),
                                new Class[]{method.getReturnType()},
                                (mapProxy, mapMethod, mapArgs) -> switch (mapMethod.getName()) {
                                    case "putAll" -> {
                                        hashMap.put((String) args[0], new HashMap<>((Map<String, String>) mapArgs[0]));
                                        yield null;
                                    }
                                    case "readAllMap" -> new HashMap<>(hashMap.getOrDefault((String) args[0], Map.of()));
                                    case "get" -> hashMap.getOrDefault((String) args[0], Map.of()).get(mapArgs[0]);
                                    default -> defaultValue(mapMethod.getReturnType());
                                }
                        );
                        default -> defaultValue(method.getReturnType());
                    }
            );
        }

        private Object defaultValue(Class<?> returnType) {
            if (returnType == boolean.class) {
                return false;
            }
            if (returnType == int.class) {
                return 0;
            }
            if (returnType == long.class) {
                return 0L;
            }
            return null;
        }
    }
}
