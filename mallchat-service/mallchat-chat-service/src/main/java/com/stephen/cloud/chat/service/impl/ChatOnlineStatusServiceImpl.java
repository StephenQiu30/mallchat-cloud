package com.stephen.cloud.chat.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.stephen.cloud.chat.service.ChatOnlineStatusService;
import com.stephen.cloud.common.cache.utils.CacheUtils;
import com.stephen.cloud.common.constants.WebSocketConstant;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 在线状态服务实现
 *
 * @author StephenQiu30
 */
@Service
public class ChatOnlineStatusServiceImpl implements ChatOnlineStatusService {

    @Resource
    private CacheUtils cacheUtils;

    @Override
    public Integer getOnlineStatus(Long userId) {
        if (userId == null || userId <= 0) {
            return 0;
        }
        return CollUtil.isEmpty(cacheUtils.sMembers(WebSocketConstant.WS_USER_CONNECTIONS_KEY + userId)) ? 0 : 1;
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
