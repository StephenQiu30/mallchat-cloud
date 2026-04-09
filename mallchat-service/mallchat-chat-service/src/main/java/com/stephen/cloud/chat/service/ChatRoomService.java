package com.stephen.cloud.chat.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.api.chat.model.vo.ChatRoomVO;
import com.stephen.cloud.chat.model.entity.ChatRoom;
import jakarta.servlet.http.HttpServletRequest;

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
     * 校验聊天室数据
     *
     * @param chatRoom 聊天室实体
     */
    void validChatRoom(ChatRoom chatRoom);


    /**
     * 获取聊天室视图类
     *
     * @param chatRoom 聊天室
     * @param request  请求
     * @return {@link ChatRoomVO}
     */
    ChatRoomVO getChatRoomVO(ChatRoom chatRoom, HttpServletRequest request);

    /**
     * 获取聊天室视图类列表
     *
     * @param chatRoomList 聊天室列表
     * @param request      请求
     * @return {@link List<ChatRoomVO>}
     */
    List<ChatRoomVO> getChatRoomVO(List<ChatRoom> chatRoomList, HttpServletRequest request);

    /**
     * 分页获取聊天室视图类
     *
     * @param chatRoomPage 聊天室分页对象
     * @param request      请求
     * @return {@link Page<ChatRoomVO>}
     */
    Page<ChatRoomVO> getChatRoomVOPage(Page<ChatRoom> chatRoomPage, HttpServletRequest request);

    /**
     * 创建聊天室 (群聊或私聊)
     *
     * @param chatRoom 聊天室实体
     * @return 房间 ID
     */
    Long addChatRoom(ChatRoom chatRoom);

    /**
     * 获取用户参与的聊天室列表
     *
     * @param userId 用户 ID
     * @return 聊天室列表
     */
    List<ChatRoomVO> listUserChatRooms(Long userId);

    /**
     * 加入聊天室
     *
     * @param roomId 房间 ID
     * @param userId 用户 ID
     */
    void joinChatRoom(Long roomId, Long userId);

    /**
     * 获取或创建与好友的私聊房间
     *
     * @param peerUserId 对方用户 ID
     * @param userId     当前用户 ID
     * @return 房间 ID
     */
    Long getOrCreatePrivateRoom(Long peerUserId, Long userId);
}
