package com.stephen.cloud.common.websocket.manager;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 在线状态 MQ 发布去重：同一 dedupeId 在 TTL 内只发布一次。
 */
final class OnlineStatusPublishDeduper {

    private final long ttlMillis;

    private final ConcurrentHashMap<String, Long> lastPublishedAt = new ConcurrentHashMap<>();

    OnlineStatusPublishDeduper(long ttlMillis) {
        this.ttlMillis = Math.max(ttlMillis, 0L);
    }

    boolean tryAcquire(String dedupeId) {
        if (ttlMillis <= 0 || dedupeId == null || dedupeId.isBlank()) {
            return true;
        }
        long now = System.currentTimeMillis();
        Long previous = lastPublishedAt.put(dedupeId, now);
        if (previous == null) {
            return true;
        }
        if (now - previous >= ttlMillis) {
            return true;
        }
        lastPublishedAt.put(dedupeId, previous);
        return false;
    }

    void clear() {
        lastPublishedAt.clear();
    }
}
