package com.stephen.cloud.chat.support;

import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * IM 运维指标记录器（恢复演练与一致性检查）。
 */
@Component
public class OpsMetricsRecorder {

    private static final String UNKNOWN = "UNKNOWN";

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    public OpsMetricsRecorder() {
    }

    public OpsMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordRecovery(String phase, String result) {
        recordCounter("mallchat.im.recovery.total", "phase", phase, "result", result);
    }

    public void recordConsistency(String domain, String result) {
        recordCounter("mallchat.im.consistency.total", "domain", domain, "result", result);
    }

    private void recordCounter(String metric, String tag1Key, String tag1Value, String tag2Key, String tag2Value) {
        if (meterRegistry == null) {
            return;
        }
        meterRegistry.counter(metric,
                tag1Key, normalize(tag1Value),
                tag2Key, normalize(tag2Value)).increment();
    }

    private String normalize(String value) {
        return StringUtils.isBlank(value) ? UNKNOWN : value;
    }
}
