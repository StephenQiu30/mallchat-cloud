package com.stephen.cloud.chat.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.stephen.cloud.chat.service.ChatOnlineStatusService;
import com.stephen.cloud.common.cache.utils.CacheUtils;
import com.stephen.cloud.common.cache.utils.LocalCacheUtils;
import com.stephen.cloud.common.constants.WebSocketConstant;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 在线状态服务实现
 * <p>
 * 采用 L1 (Caffeine 本地缓存) + L2 (Redis) 两级缓存架构，
 * 确保单次查询性能 < 5ms。
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Service
public class ChatOnlineStatusServiceImpl implements ChatOnlineStatusService {

    /**
     * 本地缓存 key 前缀
     */
    private static final String LOCAL_CACHE_PREFIX = "online:status:";

    @Resource
    private CacheUtils cacheUtils;

    @Resource
    private LocalCacheUtils localCacheUtils;

    @Override
    public Integer getOnlineStatus(Long userId) {
        if (userId == null || userId <= 0) {
            return 0;
        }
        // L1: 优先查本地缓存
        String cacheKey = LOCAL_CACHE_PREFIX + userId;
        Integer cached = localCacheUtils.get(cacheKey, Integer.class);
        if (cached != null) {
            return cached;
        }
        // L2: 回源 Redis
        Integer status = CollUtil.isEmpty(cacheUtils.sMembers(WebSocketConstant.WS_USER_CONNECTIONS_KEY + userId)) ? 0 : 1;
        // 写入本地缓存
        localCacheUtils.put(cacheKey, status);
        return status;
    }

    @Override
    public Map<Long, Integer> getOnlineStatusMap(Collection<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        Map<Long, Integer> onlineStatusMap = new HashMap<>();
        userIds.forEach(userId -> onlineStatusMap.put(userId, getOnlineStatus(userId)));
        return onlineStatusMap;
    }
}
