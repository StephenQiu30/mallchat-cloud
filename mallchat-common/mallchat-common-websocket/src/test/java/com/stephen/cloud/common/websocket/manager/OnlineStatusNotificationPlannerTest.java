package com.stephen.cloud.common.websocket.manager;

import com.stephen.cloud.common.rabbitmq.model.ImWebSocketEvent;
import com.stephen.cloud.common.rabbitmq.model.WebSocketMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

class OnlineStatusNotificationPlannerTest {

    @Test
    void shouldBatchMergeSelfAndFriendsWithoutDuplicates() {
        Set<Long> friendIds = new LinkedHashSet<>();
        friendIds.add(2L);
        friendIds.add(3L);
        friendIds.add(2L);

        Set<Long> targets = OnlineStatusNotificationPlanner.mergeNotificationTargets(1L, friendIds);

        Assertions.assertEquals(new LinkedHashSet<>(Set.of(1L, 2L, 3L)), targets);
    }

    @Test
    void shouldBuildStableDedupeIdForOnlineAndOffline() {
        Assertions.assertEquals("online_status:42:1", OnlineStatusNotificationPlanner.buildDedupeId(42L, true));
        Assertions.assertEquals("online_status:42:0", OnlineStatusNotificationPlanner.buildDedupeId(42L, false));
    }

    @Test
    void shouldAttachDedupeIdToWebSocketMessageAndEvent() {
        WebSocketMessage message = OnlineStatusNotificationPlanner.buildMessage(
                7L, true, Set.of(7L, 8L), "online_status:7:1");

        Assertions.assertEquals("online_status:7:1", message.getDedupeId());
        Assertions.assertEquals("online_status:7:1", message.getBizId());
        ImWebSocketEvent event = (ImWebSocketEvent) message.getData();
        Assertions.assertEquals("online_status:7:1", event.getBizId());
    }
}
