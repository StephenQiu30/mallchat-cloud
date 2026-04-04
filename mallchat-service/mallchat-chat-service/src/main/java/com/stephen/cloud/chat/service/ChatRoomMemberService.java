package com.stephen.cloud.chat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.chat.model.entity.ChatRoomMember;

/**
 * 聊天室成员服务接口
 *
 * @author StephenQiu30
 */
public interface ChatRoomMemberService extends IService<ChatRoomMember> {

    /**
     * 获取房间所有成员
     *
     * @param roomId 房间ID
     * @return 成员列表
     */
    java.util.List<ChatRoomMember> listByRoomId(Long roomId);
}
