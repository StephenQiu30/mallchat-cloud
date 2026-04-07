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
     * 判断用户是否在房间中
     *
     * @param roomId 房间ID
     * @param userId 用户ID
     * @return 是否在房间中
     */
    boolean isMember(Long roomId, Long userId);

    /**
     * 获取房间所有成员
     *
     * @param roomId 房间ID
     * @return 成员列表
     */
    java.util.List<ChatRoomMember> listByRoomId(Long roomId);

    /**
     * 退出房间（同步清除缓存）
     *
     * @param roomId 房间 ID
     * @param userId 用户 ID
     */
    void leaveRoom(Long roomId, Long userId);
}
