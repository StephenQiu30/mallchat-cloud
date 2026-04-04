package com.stephen.cloud.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.api.chat.model.dto.ChatFriendQueryRequest;
import com.stephen.cloud.api.chat.model.vo.ChatFriendUserVO;
import com.stephen.cloud.chat.model.entity.UserFriend;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 用户好友服务
 * <p>
 * 负责好友关系的建立与查询；跨用户校验通过 Feign 调用用户服务。
 * </p>
 *
 * @author StephenQiu30
 */
public interface UserFriendService extends IService<UserFriend> {

    /**
     * 校验好友数据
     *
     * @param userFriend 好友实体
     * @param add        是否为新增操作
     */
    void validUserFriend(UserFriend userFriend, boolean add);

    /**
     * 获取查询条件
     *
     * @param chatFriendQueryRequest 查询请求
     * @return {@link LambdaQueryWrapper<UserFriend>}
     */
    LambdaQueryWrapper<UserFriend> getQueryWrapper(ChatFriendQueryRequest chatFriendQueryRequest);

    /**
     * 获取记录视图类
     *
     * @param userFriend 好友实体
     * @param request    请求
     * @return {@link ChatFriendUserVO}
     */
    ChatFriendUserVO getUserFriendVO(UserFriend userFriend, HttpServletRequest request);

    /**
     * 批量获取记录视图类
     *
     * @param userFriendList 好友实体列表
     * @param request        请求
     * @return {@link List<ChatFriendUserVO>}
     */
    List<ChatFriendUserVO> getUserFriendVO(List<UserFriend> userFriendList, HttpServletRequest request);

    /**
     * 分页获取记录视图类
     *
     * @param userFriendPage 好友分页
     * @param request        请求
     * @return {@link Page<ChatFriendUserVO>}
     */
    Page<ChatFriendUserVO> getUserFriendVOPage(Page<UserFriend> userFriendPage, HttpServletRequest request);

    /**
     * 添加好友（双向记录，已存在则幂等返回）
     *
     * @param userId       当前用户 ID
     * @param friendUserId 好友用户 ID
     */
    void addFriend(Long userId, Long friendUserId);

    /**
     * 好友列表（装配用户昵称头像）
     *
     * @param userId 当前用户 ID
     * @return 好友视图列表
     */
    List<ChatFriendUserVO> listFriends(Long userId);

    /**
     * 是否互为好友（双向关系均存在）
     *
     * @param userId       用户 ID
     * @param friendUserId 对方用户 ID
     * @return 是否互为好友
     */
    boolean isMutualFriend(Long userId, Long friendUserId);
}
