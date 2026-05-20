package com.stephen.cloud.common.rabbitmq.producer;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * RabbitMQ 发布观测记录器。
 */
@Slf4j
@Component
public class RabbitMqPublishObservation {

    private static final String UNKNOWN = "UNKNOWN";

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    public RabbitMqPublishObservation() {
    }

    public RabbitMqPublishObservation(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordPublish(String bizType, String bizId, String result) {
        increment("mallchat.rabbitmq.publish.total", normalize(bizType), normalize(result));
        log.debug("[RabbitMqPublishObservation] 发布结果, bizType={}, bizId={}, result={}",
                bizType, bizId, result);
    }

    public void recordConfirm(String correlationId, boolean ack, String cause) {
        PublishContext context = parseCorrelationId(correlationId);
        String result = ack ? "ack" : "nack";
        increment("mallchat.rabbitmq.confirm.total", context.bizType(), result);
        log.debug("[RabbitMqPublishObservation] 发布确认, bizType={}, bizId={}, result={}, cause={}",
                context.bizType(), context.bizId(), result, cause);
    }

    public void recordReturned(Map<String, Object> headers, int replyCode) {
        String bizType = normalizeValue(headers == null ? null : headers.get("bizType"));
        String bizId = normalizeValue(headers == null ? null : headers.get("bizId"));
        increment("mallchat.rabbitmq.return.total", bizType, String.valueOf(replyCode));
        log.debug("[RabbitMqPublishObservation] 发布退回, bizType={}, bizId={}, replyCode={}",
                bizType, bizId, replyCode);
    }

    private void increment(String metricName, String bizType, String result) {
        if (meterRegistry == null) {
            return;
        }
        meterRegistry.counter(metricName, "bizType", normalize(bizType), "result", normalize(result)).increment();
    }

    private PublishContext parseCorrelationId(String correlationId) {
        if (StringUtils.isBlank(correlationId)) {
            return new PublishContext(UNKNOWN, UNKNOWN);
        }
        int splitIndex = correlationId.indexOf(':');
        if (splitIndex <= 0 || splitIndex == correlationId.length() - 1) {
            return new PublishContext(normalize(correlationId), UNKNOWN);
        }
        return new PublishContext(normalize(correlationId.substring(0, splitIndex)),
                normalize(correlationId.substring(splitIndex + 1)));
    }

    private String normalizeValue(Object value) {
        return value == null ? UNKNOWN : normalize(value.toString());
    }

    private String normalize(String value) {
        return StringUtils.isBlank(value) ? UNKNOWN : value;
    }

    private record PublishContext(String bizType, String bizId) {
    }
}
