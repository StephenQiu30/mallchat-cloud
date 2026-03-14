package com.stephen.cloud.chat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.api.chat.model.dto.ChatRoomAddRequest;
import com.stephen.cloud.api.chat.model.vo.ChatRoomVO;
import com.stephen.cloud.chat.model.entity.ChatRoom;

import java.util.List;

/**
 * 聊天室服务接口
 * <p>
 * 提供聊天室的创建、查询及成员管理功能。
 * 遵循项目中统一的业务规范与异常处理。
 * </p>
 *
 * @author StephenQiu30
 */
public interface ChatRoomService extends IService<ChatRoom> {

    /**
     * 创建聊天室
     *
     * @param chatRoomAddRequest 创建请求
     * @return 聊天室ID
     */
    Long addChatRoom(ChatRoomAddRequest chatRoomAddRequest);

    /**
     * 获取用户参与的聊天室列表
     *
     * @param userId 用户ID
     * @return 聊天室列表
     */
    List<ChatRoomVO> listUserChatRooms(Long userId);

    /**
     * 加入聊天室
     *
     * @param roomId 房间ID
     * @param userId 用户ID
     */
    void joinChatRoom(Long roomId, Long userId);
}
