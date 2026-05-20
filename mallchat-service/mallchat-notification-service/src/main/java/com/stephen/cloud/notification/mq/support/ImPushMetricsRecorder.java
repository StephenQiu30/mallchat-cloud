package com.stephen.cloud.notification.mq.support;

import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * IM 实时推送指标记录器。
 */
@Component
public class ImPushMetricsRecorder {

    private static final String UNKNOWN = "UNKNOWN";

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    public ImPushMetricsRecorder() {
    }

    public ImPushMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void record(String bizType, String eventType, String result) {
        record(bizType, eventType, result, 1);
    }

    public void record(String bizType, String eventType, String result, int count) {
        if (meterRegistry == null || count <= 0) {
            return;
        }
        meterRegistry.counter("mallchat.im.push.total",
                "bizType", normalize(bizType),
                "eventType", normalize(eventType),
                "result", normalize(result)).increment(count);
    }

    private String normalize(String value) {
        return StringUtils.isBlank(value) ? UNKNOWN : value;
    }
}
