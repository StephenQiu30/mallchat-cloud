package com.stephen.cloud.notification.mq.support;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ImPushMetricsRecorderTest {

    @Test
    void shouldRecordDeliveryLatencyAsHistogram() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ImPushMetricsRecorder recorder = new ImPushMetricsRecorder(meterRegistry);

        recorder.recordLatency("chat", "ws_push", 150.0);
        recorder.recordLatency("chat", "ws_push", 200.0);

        Assertions.assertEquals(2, meterRegistry.get("mallchat.im.push.latency")
                .tag("bizType", "chat")
                .tag("eventType", "ws_push")
                .summary()
                .count());
        Assertions.assertEquals(350.0, meterRegistry.get("mallchat.im.push.latency")
                .tag("bizType", "chat")
                .tag("eventType", "ws_push")
                .summary()
                .totalAmount());
    }

    @Test
    void shouldRecordConnectionCountAsGauge() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ImPushMetricsRecorder recorder = new ImPushMetricsRecorder(meterRegistry);

        recorder.recordConnectionCount("ws", 42);

        Assertions.assertEquals(42.0, meterRegistry.get("mallchat.im.push.connections")
                .tag("channelType", "ws")
                .gauge()
                .value());
    }

    @Test
    void shouldUpdateConnectionCountGaugeOnSubsequentCalls() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ImPushMetricsRecorder recorder = new ImPushMetricsRecorder(meterRegistry);

        recorder.recordConnectionCount("ws", 10);
        recorder.recordConnectionCount("ws", 25);

        Assertions.assertEquals(25.0, meterRegistry.get("mallchat.im.push.connections")
                .tag("channelType", "ws")
                .gauge()
                .value());
    }

    @Test
    void shouldSkipLatencyWhenMeterRegistryIsMissing() {
        ImPushMetricsRecorder recorder = new ImPushMetricsRecorder(null);

        Assertions.assertDoesNotThrow(() -> recorder.recordLatency("chat", "ws_push", 150.0));
    }

    @Test
    void shouldSkipConnectionCountWhenMeterRegistryIsMissing() {
        ImPushMetricsRecorder recorder = new ImPushMetricsRecorder(null);

        Assertions.assertDoesNotThrow(() -> recorder.recordConnectionCount("ws", 42));
    }

    @Test
    void shouldNormalizeBlankTagValuesForLatency() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ImPushMetricsRecorder recorder = new ImPushMetricsRecorder(meterRegistry);

        recorder.recordLatency("", null, 100.0);

        Assertions.assertEquals(1.0, meterRegistry.get("mallchat.im.push.latency")
                .tag("bizType", "UNKNOWN")
                .tag("eventType", "UNKNOWN")
                .summary()
                .count());
    }

    @Test
    void shouldNormalizeBlankTagValuesForConnectionCount() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ImPushMetricsRecorder recorder = new ImPushMetricsRecorder(meterRegistry);

        recorder.recordConnectionCount("", 5);

        Assertions.assertEquals(5.0, meterRegistry.get("mallchat.im.push.connections")
                .tag("channelType", "UNKNOWN")
                .gauge()
                .value());
    }
}
