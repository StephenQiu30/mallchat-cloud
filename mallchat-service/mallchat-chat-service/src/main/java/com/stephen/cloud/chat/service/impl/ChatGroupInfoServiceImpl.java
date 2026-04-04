package com.stephen.cloud.chat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.chat.mapper.ChatGroupInfoMapper;
import com.stephen.cloud.chat.model.entity.ChatGroupInfo;
import com.stephen.cloud.chat.service.ChatGroupInfoService;
import org.springframework.stereotype.Service;

/**
 * 群组详情服务项
 *
 * @author StephenQiu30
 */
@Service
public class ChatGroupInfoServiceImpl extends ServiceImpl<ChatGroupInfoMapper, ChatGroupInfo>
    implements ChatGroupInfoService {

    @Override
    public void initGroupInfo(Long roomId, String groupName, Long userId) {
        ChatGroupInfo groupInfo = new ChatGroupInfo();
        groupInfo.setRoomId(roomId);
        groupInfo.setGroupName(groupName);
        groupInfo.setCreateUser(userId);
        this.save(groupInfo);
    }
}
