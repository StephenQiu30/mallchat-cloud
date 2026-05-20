package com.stephen.cloud.chat.support;

import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * IM 关键业务指标记录器。
 */
@Component
public class ChatBusinessMetricsRecorder {

    private static final String UNKNOWN = "UNKNOWN";

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    public ChatBusinessMetricsRecorder() {
    }

    public ChatBusinessMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void record(String action, String result) {
        if (meterRegistry == null) {
            return;
        }
        meterRegistry.counter("mallchat.im.business.total",
                "action", normalize(action),
                "result", normalize(result)).increment();
    }

    private String normalize(String value) {
        return StringUtils.isBlank(value) ? UNKNOWN : value;
    }
}
