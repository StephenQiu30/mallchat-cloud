package com.stephen.cloud.chat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.chat.model.entity.ChatGroupInfo;

/**
 * 群组详情服务
 *
 * @author StephenQiu30
 */
public interface ChatGroupInfoService extends IService<ChatGroupInfo> {

    /**
     * 初始化群组详情
     * @param roomId 虚拟房间ID
     * @param groupName 群名称
     * @param userId 创建者
     */
    void initGroupInfo(Long roomId, String groupName, Long userId);
}
