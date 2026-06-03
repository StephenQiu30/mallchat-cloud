package com.stephen.cloud.chat.service.impl;

import com.stephen.cloud.api.chat.model.vo.ImCoreConsistencyCheckVO;
import com.stephen.cloud.chat.ops.ImCoreConsistencyAssertions;
import com.stephen.cloud.chat.ops.ImCoreConsistencyQueryPort;
import com.stephen.cloud.chat.support.OpsMetricsRecorder;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class ImCoreConsistencyCheckServiceImplTest {

    private ImCoreConsistencyCheckServiceImpl consistencyCheckService;
    private FakeConsistencyQueryPort queryPort;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        consistencyCheckService = new ImCoreConsistencyCheckServiceImpl();
        queryPort = new FakeConsistencyQueryPort();
        meterRegistry = new SimpleMeterRegistry();
        ReflectionTestUtils.setField(consistencyCheckService, "consistencyQueryPort", queryPort);
        ReflectionTestUtils.setField(consistencyCheckService, "opsMetricsRecorder", new OpsMetricsRecorder(meterRegistry));
    }

    @Test
    void shouldCoverFriendRoomMessageSessionAndMomentDomains() {
        ImCoreConsistencyCheckVO result = consistencyCheckService.checkAll();

        Set<String> domains = Set.of("friend", "room", "message", "session", "moment");
        Assertions.assertTrue(result.isPassed());
        Assertions.assertEquals(ImCoreConsistencyAssertions.ALL.size(), result.getItems().size());
        Assertions.assertEquals(domains, result.getItems().stream().map(item -> item.getDomain()).distinct()
                .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void shouldFailWhenOrphanRecordsExist() {
        queryPort.counts.put(
                "SELECT COUNT(*) FROM chat_message m LEFT JOIN chat_room r ON r.id = m.room_id WHERE r.id IS NULL",
                2L);

        ImCoreConsistencyCheckVO result = consistencyCheckService.checkAll();

        Assertions.assertFalse(result.isPassed());
        Assertions.assertEquals(1.0, meterRegistry.get("mallchat.im.consistency.total")
                .tag("domain", "message")
                .tag("result", "failure")
                .counter()
                .count());
    }

    private static class FakeConsistencyQueryPort implements ImCoreConsistencyQueryPort {
        private final Map<String, Long> counts = new HashMap<>();

        @Override
        public long countOrphans(String sql) {
            return counts.getOrDefault(sql, 0L);
        }
    }
}
