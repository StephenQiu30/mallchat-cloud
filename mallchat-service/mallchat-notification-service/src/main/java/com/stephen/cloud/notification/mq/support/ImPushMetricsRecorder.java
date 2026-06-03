package com.stephen.cloud.notification.mq.support;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * IM 实时推送指标记录器。
 */
@Component
public class ImPushMetricsRecorder {

    private static final String UNKNOWN = "UNKNOWN";

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    private final Map<String, AtomicInteger> connectionCounts = new ConcurrentHashMap<>();

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

    /**
     * 记录投递延迟（histogram/summary）。
     */
    public void recordLatency(String bizType, String eventType, double latencyMs) {
        if (meterRegistry == null) {
            return;
        }
        meterRegistry.summary("mallchat.im.push.latency",
                "bizType", normalize(bizType),
                "eventType", normalize(eventType)).record(latencyMs);
    }

    /**
     * 记录连接数（gauge）。
     */
    public void recordConnectionCount(String channelType, int count) {
        if (meterRegistry == null) {
            return;
        }
        String normalizedChannelType = normalize(channelType);
        String key = "mallchat.im.push.connections:" + normalizedChannelType;

        connectionCounts.computeIfAbsent(key, k -> {
            AtomicInteger atomicCount = new AtomicInteger(count);
            Gauge.builder("mallchat.im.push.connections", atomicCount, AtomicInteger::doubleValue)
                    .tag("channelType", normalizedChannelType)
                    .register(meterRegistry);
            return atomicCount;
        }).set(count);
    }

    private String normalize(String value) {
        return StringUtils.isBlank(value) ? UNKNOWN : value;
    }
}
