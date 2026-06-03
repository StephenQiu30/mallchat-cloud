package com.stephen.cloud.notification.mq.support;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ImPushMetricsRecorderTest {

    @Test
    void shouldRecordPushMetricWithThreeTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ImPushMetricsRecorder recorder = new ImPushMetricsRecorder(registry);

        recorder.record("CHAT_MESSAGE_PUSH", "CHAT_MESSAGE", "success");

        Assertions.assertEquals(1.0, registry.get("mallchat.im.push.total")
                .tag("bizType", "CHAT_MESSAGE_PUSH")
                .tag("eventType", "CHAT_MESSAGE")
                .tag("result", "success")
                .counter()
                .count());
    }

    @Test
    void shouldSkipWhenMeterRegistryIsNull() {
        ImPushMetricsRecorder recorder = new ImPushMetricsRecorder(null);

        Assertions.assertDoesNotThrow(() -> recorder.record("CHAT_MESSAGE_PUSH", "CHAT_MESSAGE", "success"));
    }

    @Test
    void shouldIncrementByBatchCount() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ImPushMetricsRecorder recorder = new ImPushMetricsRecorder(registry);

        recorder.record("CHAT_MESSAGE_PUSH", "CHAT_MESSAGE", "offline", 5);

        Assertions.assertEquals(5.0, registry.get("mallchat.im.push.total")
                .tag("bizType", "CHAT_MESSAGE_PUSH")
                .tag("eventType", "CHAT_MESSAGE")
                .tag("result", "offline")
                .counter()
                .count());
    }

    @Test
    void shouldSkipWhenCountIsZeroOrNegative() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ImPushMetricsRecorder recorder = new ImPushMetricsRecorder(registry);

        recorder.record("CHAT_MESSAGE_PUSH", "CHAT_MESSAGE", "success", 0);
        recorder.record("CHAT_MESSAGE_PUSH", "CHAT_MESSAGE", "success", -1);

        Assertions.assertNull(registry.find("mallchat.im.push.total")
                .tag("bizType", "CHAT_MESSAGE_PUSH")
                .tag("eventType", "CHAT_MESSAGE")
                .tag("result", "success")
                .counter());
    }

    @Test
    void shouldNormalizeBlankTagsToUnknown() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ImPushMetricsRecorder recorder = new ImPushMetricsRecorder(registry);

        recorder.record("", null, "  ");

        Assertions.assertEquals(1.0, registry.get("mallchat.im.push.total")
                .tag("bizType", "UNKNOWN")
                .tag("eventType", "UNKNOWN")
                .tag("result", "UNKNOWN")
                .counter()
                .count());
    }

    @Test
    void shouldRecordMultipleResultTypesIndependently() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ImPushMetricsRecorder recorder = new ImPushMetricsRecorder(registry);

        recorder.record("CHAT_MESSAGE_PUSH", "CHAT_MESSAGE", "success");
        recorder.record("CHAT_MESSAGE_PUSH", "CHAT_MESSAGE", "success");
        recorder.record("CHAT_MESSAGE_PUSH", "CHAT_MESSAGE", "offline");
        recorder.record("CHAT_MESSAGE_PUSH", "CHAT_MESSAGE", "failure");

        Assertions.assertEquals(2.0, registry.get("mallchat.im.push.total")
                .tag("result", "success").counter().count());
        Assertions.assertEquals(1.0, registry.get("mallchat.im.push.total")
                .tag("result", "offline").counter().count());
        Assertions.assertEquals(1.0, registry.get("mallchat.im.push.total")
                .tag("result", "failure").counter().count());
    }
}
