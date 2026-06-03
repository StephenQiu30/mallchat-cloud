package com.stephen.cloud.chat.support;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class OpsMetricsRecorderTest {

    @Test
    void shouldRecordRecoveryMetric() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OpsMetricsRecorder recorder = new OpsMetricsRecorder(registry);

        recorder.recordRecovery("plan", "success");

        Assertions.assertEquals(1.0, registry.get("mallchat.im.recovery.total")
                .tag("phase", "plan")
                .tag("result", "success")
                .counter()
                .count());
    }

    @Test
    void shouldRecordConsistencyMetric() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OpsMetricsRecorder recorder = new OpsMetricsRecorder(registry);

        recorder.recordConsistency("friend", "success");

        Assertions.assertEquals(1.0, registry.get("mallchat.im.consistency.total")
                .tag("domain", "friend")
                .tag("result", "success")
                .counter()
                .count());
    }

    @Test
    void shouldIgnoreWhenMeterRegistryMissing() {
        OpsMetricsRecorder recorder = new OpsMetricsRecorder(null);
        Assertions.assertDoesNotThrow(() -> {
            recorder.recordRecovery("execute", "failure");
            recorder.recordConsistency("room", "failure");
        });
    }
}
