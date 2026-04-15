package com.stephen.cloud.chat.config;

import com.stephen.cloud.chat.service.UserFriendService;
import com.stephen.cloud.common.websocket.manager.ChannelManager;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;

/**
 * 在线状态通知配置
 *
 * @author StephenQiu30
 */
@Configuration
public class ChatOnlineStatusConfig {

    @Resource
    private ChannelManager channelManager;

    @Resource
    private UserFriendService userFriendService;

    @PostConstruct
    public void initChannelManagerFriendResolver() {
        channelManager.setFriendIdsResolver(userFriendService::listFriendIdsForNotification);
    }
}
