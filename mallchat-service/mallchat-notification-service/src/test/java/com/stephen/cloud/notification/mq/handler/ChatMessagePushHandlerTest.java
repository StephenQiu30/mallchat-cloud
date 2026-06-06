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
    void shouldRespectMessageUserIdsAsAllowlistWhenRoomMemberCacheAlsoExists() throws Exception {
        Long roomId = 100L;
        cacheUtils.roomMembers.put(ChatCacheConstant.getRoomMemberKey(roomId), Set.of("1", "2", "3"));

        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .roomId(roomId)
                .userIds(List.of(1L, 2L))
                .pushType(WebSocketPushTypeEnum.BROADCAST.getValue())
                .data(Map.of("roomId", roomId))
                .build();

        handler.onMessage(wsMessage, RabbitMessage.builder().msgId("msg-1").build());

        Assertions.assertEquals(1, channelManager.writeCountByUser.get("1"));
        Assertions.assertEquals(1, channelManager.writeCountByUser.get("2"));
        Assertions.assertNull(channelManager.writeCountByUser.get("3"));
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

    @Test
    void shouldDeduplicateOfflineUserPushByBizId() throws Exception {
        // 模拟重复投递：同一个 bizId 的消息不应该重复推送给离线用户
        Long roomId = 100L;
        String bizId = "chat_group_msg:1";
        channelManager.writeResult = 0; // 模拟用户离线

        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .roomId(roomId)
                .userIds(List.of(1L, 2L))
                .pushType(WebSocketPushTypeEnum.BROADCAST.getValue())
                .bizId(bizId)
                .data(ImWebSocketEvent.builder()
                        .type("CHAT_MESSAGE")
                        .bizId(bizId)
                        .roomId(roomId)
                        .data(Map.of("roomId", roomId))
                        .build())
                .build();

        // 第一次投递
        handler.onMessage(wsMessage, RabbitMessage.builder().msgId("msg-1").build());
        Assertions.assertEquals(1, channelManager.writeCountByUser.get("1"));
        Assertions.assertEquals(1, channelManager.writeCountByUser.get("2"));
        Assertions.assertEquals(2.0, pushCounter("CHAT_MESSAGE_PUSH", "CHAT_MESSAGE", "offline"));

        // 重置计数
        channelManager.writeCountByUser.clear();

        // 模拟重复投递（同一 bizId）
        handler.onMessage(wsMessage, RabbitMessage.builder().msgId("msg-2").build());

        // 验证不应该重复推送
        Assertions.assertNull(channelManager.writeCountByUser.get("1"));
        Assertions.assertNull(channelManager.writeCountByUser.get("2"));
    }

    @Test
    void shouldAllowDifferentBizIdPushEvenIfUserOffline() throws Exception {
        // 不同 bizId 的消息应该正常推送
        Long roomId = 100L;
        channelManager.writeResult = 0;

        WebSocketMessage wsMessage1 = WebSocketMessage.builder()
                .roomId(roomId)
                .userIds(List.of(1L))
                .pushType(WebSocketPushTypeEnum.MULTIPLE.getValue())
                .bizId("chat_group_msg:1")
                .data(ImWebSocketEvent.builder()
                        .type("CHAT_MESSAGE")
                        .bizId("chat_group_msg:1")
                        .roomId(roomId)
                        .build())
                .build();

        WebSocketMessage wsMessage2 = WebSocketMessage.builder()
                .roomId(roomId)
                .userIds(List.of(1L))
                .pushType(WebSocketPushTypeEnum.MULTIPLE.getValue())
                .bizId("chat_group_msg:2")
                .data(ImWebSocketEvent.builder()
                        .type("CHAT_MESSAGE")
                        .bizId("chat_group_msg:2")
                        .roomId(roomId)
                        .build())
                .build();

        handler.onMessage(wsMessage1, RabbitMessage.builder().msgId("msg-1").build());
        handler.onMessage(wsMessage2, RabbitMessage.builder().msgId("msg-2").build());

        // 不同 bizId 应该都推送成功
        Assertions.assertEquals(2, channelManager.writeCountByUser.get("1"));
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
        private final Map<String, Boolean> dedupKeys = new HashMap<>();
        private String lastRequestedKey;

        @Override
        public Set<String> sMembers(String key) {
            lastRequestedKey = key;
            return roomMembers.get(key);
        }

        @Override
        public boolean trySetDedupKey(String bizId, String userId) {
            String dedupKey = "dedup:" + bizId + ":" + userId;
            if (dedupKeys.containsKey(dedupKey)) {
                return false; // already processed
            }
            dedupKeys.put(dedupKey, true);
            return true; // new key, can process
        }
    }
}
