package com.stephen.cloud.chat.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.api.chat.model.vo.ChatRoomMemberVO;
import com.stephen.cloud.chat.model.entity.ChatRoomMember;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 聊天室成员服务接口
 *
 * @author StephenQiu30
 */
public interface ChatRoomMemberService extends IService<ChatRoomMember> {

    /**
     * 校验房间成员
     *
     * @param chatRoomMember 房间成员
     */
    void validChatRoomMember(ChatRoomMember chatRoomMember);

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
    List<ChatRoomMember> listByRoomId(Long roomId);

    /**
     * 添加成员（带幂等检查与缓存同步）
     *
     * @param roomId 房间 ID
     * @param userId 用户 ID
     */
    void addMember(Long roomId, Long userId);

    /**
     * 添加成员（指定角色）
     *
     * @param roomId 房间 ID
     * @param userId 用户 ID
     * @param role   成员角色
     */
    void addMember(Long roomId, Long userId, Integer role);

    /**
     * 获取指定成员
     *
     * @param roomId 房间 ID
     * @param userId 用户 ID
     * @return 房间成员
     */
    ChatRoomMember getMember(Long roomId, Long userId);

    /**
     * 是否为群主
     *
     * @param roomId 房间 ID
     * @param userId 用户 ID
     * @return 是否为群主
     */
    boolean isOwner(Long roomId, Long userId);

    /**
     * 获取成员视图类
     *
     * @param chatRoomMember 房间成员
     * @param request        请求
     * @return {@link ChatRoomMemberVO}
     */
    ChatRoomMemberVO getChatRoomMemberVO(ChatRoomMember chatRoomMember, HttpServletRequest request);

    /**
     * 批量获取成员视图类
     *
     * @param chatRoomMemberList 房间成员列表
     * @param request            请求
     * @return {@link List<ChatRoomMemberVO>}
     */
    List<ChatRoomMemberVO> getChatRoomMemberVO(List<ChatRoomMember> chatRoomMemberList, HttpServletRequest request);

    /**
     * 分页获取成员视图类
     *
     * @param chatRoomMemberPage 房间成员分页对象
     * @param request            请求
     * @return {@link Page<ChatRoomMemberVO>}
     */
    Page<ChatRoomMemberVO> getChatRoomMemberVOPage(Page<ChatRoomMember> chatRoomMemberPage, HttpServletRequest request);

    /**
     * 退出房间（同步清除缓存）
     *
     * @param roomId 房间 ID
     * @param userId 用户 ID
     */
    void leaveRoom(Long roomId, Long userId);
}
