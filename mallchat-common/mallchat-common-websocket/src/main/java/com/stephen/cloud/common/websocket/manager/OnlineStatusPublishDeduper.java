package com.stephen.cloud.common.websocket.manager;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 在线状态发布去重器
 * 防止短时间内重复发布同一用户的在线状态变更
 *
 * @author StephenQiu30
 */
public class OnlineStatusPublishDeduper {

    private final long ttlMillis;
    private final ConcurrentHashMap<String, Long> recentPublishes = new ConcurrentHashMap<>();

    public OnlineStatusPublishDeduper(long ttlMillis) {
        this.ttlMillis = ttlMillis;
    }

    /**
     * 尝试获取发布许可（去重）
     *
     * @param dedupeId 去重标识
     * @return true 如果可以发布，false 如果在 TTL 内已发布过
     */
    public boolean tryAcquire(String dedupeId) {
        long now = System.currentTimeMillis();
        Long lastPublish = recentPublishes.get(dedupeId);
        if (lastPublish != null && now - lastPublish < ttlMillis) {
            return false;
        }
        recentPublishes.put(dedupeId, now);
        cleanup(now);
        return true;
    }

    private void cleanup(long now) {
        if (recentPublishes.size() > 1000) {
            recentPublishes.entrySet().removeIf(entry -> now - entry.getValue() > ttlMillis);
        }
    }
}
