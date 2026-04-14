package com.stephen.cloud.notification.mq.handler;

import com.stephen.cloud.common.cache.constants.ChatCacheConstant;
import com.stephen.cloud.common.cache.utils.CacheUtils;
import com.stephen.cloud.common.rabbitmq.enums.WebSocketPushTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.ImWebSocketEvent;
import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import com.stephen.cloud.common.rabbitmq.model.WebSocketMessage;
import com.stephen.cloud.common.websocket.manager.ChannelManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class ChatMessagePushHandlerTest {

    private ChatMessagePushHandler handler;

    private FakeChannelManager channelManager;

    private FakeCacheUtils cacheUtils;

    @BeforeEach
    void setUp() {
        handler = new ChatMessagePushHandler();
        channelManager = new FakeChannelManager();
        cacheUtils = new FakeCacheUtils();
        ReflectionTestUtils.setField(handler, "channelManager", channelManager);
        ReflectionTestUtils.setField(handler, "cacheUtils", cacheUtils);
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

    private static class FakeChannelManager extends ChannelManager {
        private final Map<String, Integer> writeCountByUser = new HashMap<>();

        @Override
        public int writeToUser(String userId, String messageJson) {
            writeCountByUser.merge(userId, 1, Integer::sum);
            return 1;
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
