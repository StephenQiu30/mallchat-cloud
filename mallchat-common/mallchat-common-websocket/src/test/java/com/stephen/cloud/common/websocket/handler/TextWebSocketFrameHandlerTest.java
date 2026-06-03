package com.stephen.cloud.common.websocket.handler;

import cn.hutool.json.JSONUtil;
import com.stephen.cloud.common.cache.utils.CacheUtils;
import com.stephen.cloud.common.constants.WebSocketConstant;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.enums.WebSocketMessageTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.WebSocketMessage;
import com.stephen.cloud.common.rabbitmq.producer.RabbitMqSender;
import com.stephen.cloud.common.websocket.manager.ChannelManager;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
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

/**
 * TextWebSocketFrameHandler 单元测试
 * <p>
 * 覆盖场景：
 * - 心跳处理（pong 回复 + Redis 连接刷新）
 * - 未认证连接拒绝业务消息
 * - 握手完成事件注册连接
 * - 连接断开清理
 * - 异常处理
 * - 断线原因分类
 *
 * @author StephenQiu30
 */
class TextWebSocketFrameHandlerTest {

    private static final AttributeKey<String> ATTR_USER_ID = AttributeKey.valueOf("ws_user_id");

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
        channelManager.setServerId("test-server");
        channelManager.setFriendIdsResolver(userId -> Set.of());
    }

    @Test
    void shouldReplyPongWhenHeartbeatReceived() {
        EmbeddedChannel channel = newAuthedChannel("1001");
        WebSocketMessage heartbeat = new WebSocketMessage();
        heartbeat.setType(WebSocketMessageTypeEnum.HEARTBEAT.getCode());
        heartbeat.setData("ping");

        channel.writeInbound(new TextWebSocketFrame(JSONUtil.toJsonStr(heartbeat)));

        TextWebSocketFrame response = channel.readOutbound();
        Assertions.assertNotNull(response);
        WebSocketMessage reply = JSONUtil.parseObj(response.text()).toBean(WebSocketMessage.class);
        Assertions.assertEquals(WebSocketMessageTypeEnum.HEARTBEAT.getCode(), reply.getType());
        Assertions.assertEquals("pong", reply.getData());
        response.release();
        channel.finishAndReleaseAll();
    }

    @Test
    void shouldRefreshRedisConnectionWhenHeartbeatReceived() {
        EmbeddedChannel channel = newAuthedChannel("1001");
        // Fire handshake so channel is registered in ChannelManager
        channel.pipeline().fireUserEventTriggered(
                new WebSocketServerProtocolHandler.HandshakeComplete(
                        channel.id().asLongText(), null, "/websocket"));

        WebSocketMessage heartbeat = new WebSocketMessage();
        heartbeat.setType(WebSocketMessageTypeEnum.HEARTBEAT.getCode());
        heartbeat.setData("ping");

        channel.writeInbound(new TextWebSocketFrame(JSONUtil.toJsonStr(heartbeat)));

        // After heartbeat, the user connections key should be refreshed and non-empty
        Set<String> connections = cacheUtils.sMembers(WebSocketConstant.WS_USER_CONNECTIONS_KEY + "1001");
        Assertions.assertFalse(connections.isEmpty());
        channel.finishAndReleaseAll();
    }

    @Test
    void shouldRejectBusinessMessageAndCloseWhenNotAuthenticated() {
        EmbeddedChannel channel = new EmbeddedChannel(new TextWebSocketFrameHandler(channelManager));
        WebSocketMessage message = new WebSocketMessage();
        message.setType(WebSocketMessageTypeEnum.MESSAGE.getCode());
        message.setData("hello");

        channel.writeInbound(new TextWebSocketFrame(JSONUtil.toJsonStr(message)));

        TextWebSocketFrame response = channel.readOutbound();
        Assertions.assertNotNull(response);
        WebSocketMessage errorReply = JSONUtil.parseObj(response.text()).toBean(WebSocketMessage.class);
        Assertions.assertEquals(WebSocketMessageTypeEnum.ERROR.getCode(), errorReply.getType());
        Assertions.assertTrue(errorReply.getData().toString().contains("未认证"));
        response.release();
        channel.finishAndReleaseAll();
    }

    @Test
    void shouldIgnoreMessageWhenTypeIsNull() {
        EmbeddedChannel channel = newAuthedChannel("1001");
        WebSocketMessage message = new WebSocketMessage();
        message.setType(null);
        message.setData("hello");

        // Should not throw, just ignore
        Assertions.assertDoesNotThrow(() ->
                channel.writeInbound(new TextWebSocketFrame(JSONUtil.toJsonStr(message))));

        TextWebSocketFrame response = channel.readOutbound();
        Assertions.assertNull(response);
        channel.finishAndReleaseAll();
    }

    @Test
    void shouldIgnoreMessageWhenTypeIsUnknown() {
        EmbeddedChannel channel = newAuthedChannel("1001");
        WebSocketMessage message = new WebSocketMessage();
        message.setType(9999);
        message.setData("hello");

        // Should not throw, just ignore
        Assertions.assertDoesNotThrow(() ->
                channel.writeInbound(new TextWebSocketFrame(JSONUtil.toJsonStr(message))));

        TextWebSocketFrame response = channel.readOutbound();
        Assertions.assertNull(response);
        channel.finishAndReleaseAll();
    }

    @Test
    void shouldSendErrorResponseWhenMessageParsingFails() {
        EmbeddedChannel channel = newAuthedChannel("1001");

        channel.writeInbound(new TextWebSocketFrame("not valid json {"));

        TextWebSocketFrame response = channel.readOutbound();
        Assertions.assertNotNull(response);
        WebSocketMessage errorReply = JSONUtil.parseObj(response.text()).toBean(WebSocketMessage.class);
        Assertions.assertEquals(WebSocketMessageTypeEnum.ERROR.getCode(), errorReply.getType());
        response.release();
        channel.finishAndReleaseAll();
    }

    @Test
    void shouldRegisterChannelOnHandshakeComplete() {
        EmbeddedChannel channel = newAuthedChannel("1001");

        // Simulate handshake complete event
        channel.pipeline().fireUserEventTriggered(
                new WebSocketServerProtocolHandler.HandshakeComplete(
                        channel.id().asLongText(), null, "/websocket"));

        // Channel should be registered in ChannelManager
        Assertions.assertTrue(channelManager.isOnline("1001"));
        channel.finishAndReleaseAll();
    }

    @Test
    void shouldCloseChannelWhenHandshakeCompleteButNoUserIdBound() {
        EmbeddedChannel channel = new EmbeddedChannel(new TextWebSocketFrameHandler(channelManager));

        // Simulate handshake complete without userId
        channel.pipeline().fireUserEventTriggered(
                new WebSocketServerProtocolHandler.HandshakeComplete(
                        channel.id().asLongText(), null, "/websocket"));

        // Channel should be closed
        Assertions.assertFalse(channel.isActive());
        channel.finishAndReleaseAll();
    }

    @Test
    void shouldRemoveChannelOnDisconnect() {
        EmbeddedChannel channel = newAuthedChannel("1001");

        // Register channel first
        channel.pipeline().fireUserEventTriggered(
                new WebSocketServerProtocolHandler.HandshakeComplete(
                        channel.id().asLongText(), null, "/websocket"));
        Assertions.assertTrue(channelManager.isOnline("1001"));

        // Close channel to trigger handlerRemoved
        channel.close();

        Assertions.assertFalse(channelManager.isOnline("1001"));
        channel.finishAndReleaseAll();
    }

    @Test
    void shouldSendMessageToAuthenticatedUser() {
        EmbeddedChannel channel = newAuthedChannel("1001");

        // Register channel
        channel.pipeline().fireUserEventTriggered(
                new WebSocketServerProtocolHandler.HandshakeComplete(
                        channel.id().asLongText(), null, "/websocket"));

        // Send message through ChannelManager
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .type(WebSocketMessageTypeEnum.MESSAGE.getCode())
                .data("test-payload")
                .build();
        int count = channelManager.writeToUser("1001", JSONUtil.toJsonStr(wsMessage));

        Assertions.assertEquals(1, count);
        TextWebSocketFrame outbound = channel.readOutbound();
        Assertions.assertNotNull(outbound);
        outbound.release();
        channel.finishAndReleaseAll();
    }

    // ---- disconnect reason tests ----

    @Test
    void shouldMarkTimeoutReasonWhenReaderIdle() {
        SpyChannelManager spy = new SpyChannelManager(cacheUtils, rabbitMqSender);
        EmbeddedChannel channel = newAuthedChannelWithSpy("1001", spy);
        channel.pipeline().fireUserEventTriggered(
                new WebSocketServerProtocolHandler.HandshakeComplete(
                        channel.id().asLongText(), null, "/websocket"));

        // Fire READER_IDLE event → should set TIMEOUT reason and close
        channel.pipeline().fireUserEventTriggered(
                IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT);

        Assertions.assertEquals(DisconnectReason.TIMEOUT, spy.lastDisconnectReason);
        channel.finishAndReleaseAll();
    }

    @Test
    void shouldMarkExceptionReasonWhenExceptionCaught() {
        SpyChannelManager spy = new SpyChannelManager(cacheUtils, rabbitMqSender);
        EmbeddedChannel channel = newAuthedChannelWithSpy("1001", spy);
        channel.pipeline().fireUserEventTriggered(
                new WebSocketServerProtocolHandler.HandshakeComplete(
                        channel.id().asLongText(), null, "/websocket"));

        // Trigger exception → should set EXCEPTION reason and close
        channel.pipeline().fireExceptionCaught(new RuntimeException("test error"));

        Assertions.assertEquals(DisconnectReason.EXCEPTION, spy.lastDisconnectReason);
        channel.finishAndReleaseAll();
    }

    @Test
    void shouldMarkClientCloseReasonWhenChannelClosedByClient() {
        SpyChannelManager spy = new SpyChannelManager(cacheUtils, rabbitMqSender);
        EmbeddedChannel channel = newAuthedChannelWithSpy("1001", spy);
        channel.pipeline().fireUserEventTriggered(
                new WebSocketServerProtocolHandler.HandshakeComplete(
                        channel.id().asLongText(), null, "/websocket"));

        // Close channel directly → should default to CLIENT_CLOSE
        channel.close();

        Assertions.assertEquals(DisconnectReason.CLIENT_CLOSE, spy.lastDisconnectReason);
        channel.finishAndReleaseAll();
    }

    @Test
    void shouldMarkServerCloseReasonWhenAttributeSet() {
        SpyChannelManager spy = new SpyChannelManager(cacheUtils, rabbitMqSender);
        EmbeddedChannel channel = newAuthedChannelWithSpy("1001", spy);
        channel.pipeline().fireUserEventTriggered(
                new WebSocketServerProtocolHandler.HandshakeComplete(
                        channel.id().asLongText(), null, "/websocket"));

        // Simulate server-side close by setting reason attribute before closing
        channel.attr(TextWebSocketFrameHandler.ATTR_DISCONNECT_REASON).set(DisconnectReason.SERVER_CLOSE);
        channel.close();

        Assertions.assertEquals(DisconnectReason.SERVER_CLOSE, spy.lastDisconnectReason);
        channel.finishAndReleaseAll();
    }

    private EmbeddedChannel newAuthedChannel(String userId) {
        EmbeddedChannel channel = new EmbeddedChannel(new TextWebSocketFrameHandler(channelManager));
        channel.attr(ATTR_USER_ID).set(userId);
        return channel;
    }

    private EmbeddedChannel newAuthedChannelWithSpy(String userId, SpyChannelManager spy) {
        EmbeddedChannel channel = new EmbeddedChannel(new TextWebSocketFrameHandler(spy));
        channel.attr(ATTR_USER_ID).set(userId);
        return channel;
    }

    /**
     * Spy that records the disconnect reason passed to removeChannel
     */
    private static class SpyChannelManager extends ChannelManager {
        DisconnectReason lastDisconnectReason;

        SpyChannelManager(FakeCacheUtils cacheUtils, RecordingRabbitMqSender sender) {
            ReflectionTestUtils.setField(this, "cacheUtils", cacheUtils);
            ReflectionTestUtils.setField(this, "rabbitMqSender", sender);
            setServerId("test-server");
            setFriendIdsResolver(userId -> Set.of());
        }

        @Override
        public synchronized void removeChannel(Channel channel, DisconnectReason reason) {
            this.lastDisconnectReason = reason;
            super.removeChannel(channel, reason);
        }
    }

    private static class FakeCacheUtils extends CacheUtils {
        private final Map<String, Set<String>> setMap = new HashMap<>();
        private final Map<String, Map<String, String>> hashMap = new HashMap<>();

        FakeCacheUtils() {
            ReflectionTestUtils.setField(this, "redissonClient", createRedissonClient());
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

    private static class RecordingRabbitMqSender extends RabbitMqSender {
        private MqBizTypeEnum lastBizType;
        private Object lastPayload;

        @Override
        public void send(MqBizTypeEnum bizTypeEnum, String msgId, Object payload) {
            this.lastBizType = bizTypeEnum;
            this.lastPayload = payload;
        }
    }
}
