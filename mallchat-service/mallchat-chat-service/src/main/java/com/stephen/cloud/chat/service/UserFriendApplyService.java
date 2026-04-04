package com.stephen.cloud.chat.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.api.chat.model.dto.ChatFriendApproveRequest;
import com.stephen.cloud.api.chat.model.vo.ChatFriendApplyVO;
import com.stephen.cloud.chat.model.entity.UserFriendApply;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 好友申请服务
 *
 * @author StephenQiu30
 */
public interface UserFriendApplyService extends IService<UserFriendApply> {

    /**
     * 校验好友申请数据
     *
     * @param userFriendApply 好友申请实体
     * @param add             是否为新增操作
     */
    void validUserFriendApply(UserFriendApply userFriendApply, boolean add);

    /**
     * 获取好友申请视图类
     *
     * @param userFriendApply 好友申请
     * @param request         请求
     * @return {@link ChatFriendApplyVO}
     */
    ChatFriendApplyVO getUserFriendApplyVO(UserFriendApply userFriendApply, HttpServletRequest request);

    /**
     * 获取好友申请视图类列表
     *
     * @param userFriendApplyList 好友申请列表
     * @param request             请求
     * @return {@link List<ChatFriendApplyVO>}
     */
    List<ChatFriendApplyVO> getUserFriendApplyVO(List<UserFriendApply> userFriendApplyList, HttpServletRequest request);

    /**
     * 分页获取好友申请视图类
     *
     * @param userFriendApplyPage 好友申请分页对象
     * @param request             请求
     * @return {@link Page<ChatFriendApplyVO>}
     */
    Page<ChatFriendApplyVO> getUserFriendApplyVOPage(Page<UserFriendApply> userFriendApplyPage, HttpServletRequest request);

    /**
     * 发起好友申请
     *
     * @param userFriendApply 好友申请实体
     * @param userId          发起用户 ID
     * @return 申请 ID
     */
    Long applyFriend(UserFriendApply userFriendApply, Long userId);

    /**
     * 审核好友申请
     *
     * @param approveRequest 审核请求
     * @param userId         当前操作用户 ID（必须是申请的目标用户）
     * @return 是否成功
     */
    boolean approveFriend(ChatFriendApproveRequest approveRequest, Long userId);

    /**
     * 分页查询当前用户的申请列表
     *
     * @param current 页码
     * @param size    每页大小
     * @param userId  当前用户 ID
     * @return 分页结果
     */
    Page<ChatFriendApplyVO> listFriendApplyPage(long current, long size, Long userId);
}
