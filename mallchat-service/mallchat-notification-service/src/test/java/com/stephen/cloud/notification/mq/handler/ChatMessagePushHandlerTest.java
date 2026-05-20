package com.stephen.cloud.notification.mq.handler;

import com.stephen.cloud.common.cache.constants.ChatCacheConstant;
import com.stephen.cloud.common.cache.utils.CacheUtils;
import com.stephen.cloud.common.rabbitmq.enums.WebSocketPushTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.ImWebSocketEvent;
import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import com.stephen.cloud.common.rabbitmq.model.WebSocketMessage;
import com.stephen.cloud.common.websocket.manager.ChannelManager;
import com.stephen.cloud.notification.mq.support.ImPushMetricsRecorder;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class ChatMessagePushHandlerTest {

    private ChatMessagePushHandler handler;

    private FakeChannelManager channelManager;

    private FakeCacheUtils cacheUtils;

    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        handler = new ChatMessagePushHandler();
        channelManager = new FakeChannelManager();
        cacheUtils = new FakeCacheUtils();
        meterRegistry = new SimpleMeterRegistry();
        ReflectionTestUtils.setField(handler, "channelManager", channelManager);
        ReflectionTestUtils.setField(handler, "cacheUtils", cacheUtils);
        ReflectionTestUtils.setField(handler, "metricsRecorder", new ImPushMetricsRecorder(meterRegistry));
    }

    @Test
    void shouldPushRoomBroadcastUsingSharedRoomMemberCacheKey() throws Exception {
        Long roomId = 100L;
        cacheUtils.roomMembers.put(ChatCacheConstant.getRoomMemberKey(roomId), Set.of("1", "2"));

        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .roomId(roomId)
                .pushType(WebSocketPushTypeEnum.BROADCAST.getValue())
                .data(ImWebSocketEvent.builder()
                        .type("CHAT_MESSAGE")
                        .bizId("chat_group_msg:1")
                        .roomId(roomId)
                        .data(Map.of("roomId", roomId))
                        .build())
                .build();

        handler.onMessage(wsMessage, RabbitMessage.builder().msgId("msg-1").build());

        Assertions.assertEquals(ChatCacheConstant.getRoomMemberKey(roomId), cacheUtils.lastRequestedKey);
        Assertions.assertEquals(1, channelManager.writeCountByUser.get("1"));
        Assertions.assertEquals(1, channelManager.writeCountByUser.get("2"));
    }

    @Test
    void shouldFallbackToMessageUserIdsWhenRoomMemberCacheIsMissing() throws Exception {
        Long roomId = 100L;

        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .roomId(roomId)
                .userIds(List.of(1L, 2L))
                .pushType(WebSocketPushTypeEnum.BROADCAST.getValue())
                .data(ImWebSocketEvent.builder()
                        .type("CHAT_MESSAGE")
                        .bizId("chat_group_msg:1")
                        .roomId(roomId)
                        .data(Map.of("roomId", roomId))
                        .build())
                .build();

        handler.onMessage(wsMessage, RabbitMessage.builder().msgId("msg-1").build());

        Assertions.assertEquals(ChatCacheConstant.getRoomMemberKey(roomId), cacheUtils.lastRequestedKey);
        Assertions.assertEquals(1, channelManager.writeCountByUser.get("1"));
        Assertions.assertEquals(1, channelManager.writeCountByUser.get("2"));
    }

    @Test
    void shouldFallbackToMessageUserIdsWhenRoomMemberCacheIsEmpty() throws Exception {
        Long roomId = 100L;
        cacheUtils.roomMembers.put(ChatCacheConstant.getRoomMemberKey(roomId), Set.of());

        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .roomId(roomId)
                .userIds(List.of(1L, 2L))
                .pushType(WebSocketPushTypeEnum.BROADCAST.getValue())
                .data(Map.of("roomId", roomId))
                .build();

        handler.onMessage(wsMessage, RabbitMessage.builder().msgId("msg-1").build());

        Assertions.assertEquals(1, channelManager.writeCountByUser.get("1"));
        Assertions.assertEquals(1, channelManager.writeCountByUser.get("2"));
    }

    @Test
    void shouldPreferRoomMemberCacheWhenSnapshotAlsoExists() throws Exception {
        Long roomId = 100L;
        cacheUtils.roomMembers.put(ChatCacheConstant.getRoomMemberKey(roomId), Set.of("3"));

        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .roomId(roomId)
                .userIds(List.of(1L, 2L))
                .pushType(WebSocketPushTypeEnum.BROADCAST.getValue())
                .data(Map.of("roomId", roomId))
                .build();

        handler.onMessage(wsMessage, RabbitMessage.builder().msgId("msg-1").build());

        Assertions.assertNull(channelManager.writeCountByUser.get("1"));
        Assertions.assertNull(channelManager.writeCountByUser.get("2"));
        Assertions.assertEquals(1, channelManager.writeCountByUser.get("3"));
    }

    @Test
    void shouldSkipRoomBroadcastWhenCacheAndSnapshotAreBothEmpty() throws Exception {
        Long roomId = 100L;

        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .roomId(roomId)
                .pushType(WebSocketPushTypeEnum.BROADCAST.getValue())
                .data(Map.of("roomId", roomId))
                .build();

        handler.onMessage(wsMessage, RabbitMessage.builder().msgId("msg-1").build());

        Assertions.assertTrue(channelManager.writeCountByUser.isEmpty());
    }

    @Test
    void shouldRecordOfflinePushByMessageTypeWhenNoLocalConnectionExists() throws Exception {
        channelManager.writeResult = 0;
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .userIds(List.of(1L, 2L))
                .pushType(WebSocketPushTypeEnum.MULTIPLE.getValue())
                .data(ImWebSocketEvent.builder()
                        .type("CHAT_MESSAGE")
                        .bizId("chat_group_msg:1")
                        .data(Map.of("roomId", 100L))
                        .build())
                .build();

        handler.onMessage(wsMessage, RabbitMessage.builder().msgId("msg-1").build());

        Assertions.assertEquals(2.0, pushCounter("CHAT_MESSAGE_PUSH", "CHAT_MESSAGE", "offline"));
    }

    @Test
    void shouldRecordMixedSuccessAndOfflineWhenOnlyPartOfTargetsAreLocalOnline() throws Exception {
        channelManager.writeResultByUser.put("1", 1);
        channelManager.writeResultByUser.put("2", 0);
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .userIds(List.of(1L, 2L))
                .pushType(WebSocketPushTypeEnum.MULTIPLE.getValue())
                .data(ImWebSocketEvent.builder()
                        .type("CHAT_MESSAGE")
                        .bizId("chat_group_msg:1")
                        .data(Map.of("roomId", 100L))
                        .build())
                .build();

        handler.onMessage(wsMessage, RabbitMessage.builder().msgId("msg-1").build());

        Assertions.assertEquals(1.0, pushCounter("CHAT_MESSAGE_PUSH", "CHAT_MESSAGE", "success"));
        Assertions.assertEquals(1.0, pushCounter("CHAT_MESSAGE_PUSH", "CHAT_MESSAGE", "offline"));
    }

    @Test
    void shouldRecordPushFailureByMessageTypeWhenChannelWriteThrows() {
        channelManager.throwOnWrite = true;
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .userIds(List.of(1L))
                .pushType(WebSocketPushTypeEnum.MULTIPLE.getValue())
                .data(ImWebSocketEvent.builder()
                        .type("MESSAGE_READ")
                        .bizId("chat_read:100:1:10")
                        .data(Map.of("roomId", 100L))
                        .build())
                .build();

        Assertions.assertThrows(RuntimeException.class,
                () -> handler.onMessage(wsMessage, RabbitMessage.builder().msgId("msg-1").build()));

        Assertions.assertEquals(1.0, pushCounter("CHAT_MESSAGE_PUSH", "MESSAGE_READ", "failure"));
    }

    private double pushCounter(String bizType, String eventType, String result) {
        return meterRegistry.get("mallchat.im.push.total")
                .tag("bizType", bizType)
                .tag("eventType", eventType)
                .tag("result", result)
                .counter()
                .count();
    }

    private static class FakeChannelManager extends ChannelManager {
        private final Map<String, Integer> writeCountByUser = new HashMap<>();
        private final Map<String, Integer> writeResultByUser = new HashMap<>();
        private int writeResult = 1;
        private boolean throwOnWrite;

        @Override
        public int writeToUser(String userId, String messageJson) {
            if (throwOnWrite) {
                throw new RuntimeException("push failed");
            }
            writeCountByUser.merge(userId, 1, Integer::sum);
            return writeResultByUser.getOrDefault(userId, writeResult);
        }
    }

    private static class FakeCacheUtils extends CacheUtils {
        private final Map<String, Set<String>> roomMembers = new HashMap<>();
        private String lastRequestedKey;

        @Override
        public Set<String> sMembers(String key) {
            lastRequestedKey = key;
            return roomMembers.get(key);
        }
    }
}
