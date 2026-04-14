package com.stephen.cloud.chat.service;

import java.util.Collection;
import java.util.Map;

/**
 * 在线状态服务
 *
 * @author StephenQiu30
 */
public interface ChatOnlineStatusService {

    /**
     * 获取用户在线状态
     *
     * @param userId 用户ID
     * @return 0-离线，1-在线
     */
    Integer getOnlineStatus(Long userId);

    /**
     * 批量获取用户在线状态
     *
     * @param userIds 用户ID集合
     * @return 用户在线状态映射
     */
    Map<Long, Integer> getOnlineStatusMap(Collection<Long> userIds);
}
