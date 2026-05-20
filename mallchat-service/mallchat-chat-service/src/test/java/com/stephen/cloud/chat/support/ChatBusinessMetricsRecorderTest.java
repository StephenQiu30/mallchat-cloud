package com.stephen.cloud.chat.support;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ChatBusinessMetricsRecorderTest {

    @Test
    void shouldRecordBusinessActionWithLowCardinalityTags() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ChatBusinessMetricsRecorder recorder = new ChatBusinessMetricsRecorder(meterRegistry);

        recorder.record("message_send", "success");

        Assertions.assertEquals(1.0, meterRegistry.get("mallchat.im.business.total")
                .tag("action", "message_send")
                .tag("result", "success")
                .counter()
                .count());
    }

    @Test
    void shouldSkipWhenMeterRegistryIsMissing() {
        ChatBusinessMetricsRecorder recorder = new ChatBusinessMetricsRecorder(null);

        Assertions.assertDoesNotThrow(() -> recorder.record("message_send", "success"));
    }
}
