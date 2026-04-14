package com.stephen.cloud.chat.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.api.chat.model.dto.ChatFriendApproveRequest;
import com.stephen.cloud.api.chat.model.vo.ChatFriendApplyVO;
import com.stephen.cloud.api.user.client.UserFeignClient;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.chat.convert.ChatFriendApplyConvert;
import com.stephen.cloud.chat.mapper.UserFriendApplyMapper;
import com.stephen.cloud.chat.model.entity.UserFriendApply;
import com.stephen.cloud.chat.mq.producer.ChatMqProducer;
import com.stephen.cloud.chat.service.ChatRoomService;
import com.stephen.cloud.chat.service.UserFriendApplyService;
import com.stephen.cloud.chat.service.UserFriendService;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.exception.BusinessException;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 好友申请服务实现
 *
 * @author StephenQiu30
 */
@Service
public class UserFriendApplyServiceImpl extends ServiceImpl<UserFriendApplyMapper, UserFriendApply>
        implements UserFriendApplyService {

    @Resource
    private UserFriendService userFriendService;

    @Resource
    private ChatRoomService chatRoomService;

    @Resource
    private UserFeignClient userFeignClient;

    @Resource
    private ChatMqProducer chatMqProducer;

    /**
     * 校验好友申请数据
     *
     * @param userFriendApply 好友申请实体
     */
    @Override
    public void validUserFriendApply(UserFriendApply userFriendApply) {
        if (userFriendApply == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 基础校验由 @Validated 处理，此处保持轻量
        if (StringUtils.isNotBlank(userFriendApply.getMsg())) {
            ThrowUtils.throwIf(userFriendApply.getMsg().length() > 256, ErrorCode.PARAMS_ERROR, "留言过长");
        }
    }

    /**
     * 获取好友申请视图类
     *
     * @param userFriendApply 好友申请
     * @param request         请求
     * @return {@link ChatFriendApplyVO}
     */
    @Override
    public ChatFriendApplyVO getUserFriendApplyVO(UserFriendApply userFriendApply, HttpServletRequest request) {
        if (userFriendApply == null) {
            return null;
        }
        ChatFriendApplyVO chatFriendApplyVO = ChatFriendApplyConvert.objToVo(userFriendApply);
        // 关联查询用户信息
        Long userId = userFriendApply.getUserId();
        if (userId != null && userId > 0) {
            UserVO userVO = userFeignClient.getUserVOById(userId).getData();
            if (userVO != null) {
                chatFriendApplyVO.setUserName(userVO.getUserName());
                chatFriendApplyVO.setUserAvatar(userVO.getUserAvatar());
            }
        }
        return chatFriendApplyVO;
    }

    /**
     * 获取好友申请视图类列表
     *
     * @param userFriendApplyList 好友申请列表
     * @param request             请求
     * @return {@link List<ChatFriendApplyVO>}
     */
    @Override
    public List<ChatFriendApplyVO> getUserFriendApplyVO(List<UserFriendApply> userFriendApplyList, HttpServletRequest request) {
        if (CollUtil.isEmpty(userFriendApplyList)) {
            return Collections.emptyList();
        }
        // 批量获取用户信息
        List<Long> userIdList = userFriendApplyList.stream().map(UserFriendApply::getUserId).collect(Collectors.toList());
        Map<Long, List<UserVO>> userIdUserVOListMap = userFeignClient.getUserVOByIds(userIdList).getData().stream()
                .collect(Collectors.groupingBy(UserVO::getId));
        // 填充信息
        return userFriendApplyList.stream().map(userFriendApply -> {
            ChatFriendApplyVO chatFriendApplyVO = ChatFriendApplyConvert.objToVo(userFriendApply);
            Long userId = userFriendApply.getUserId();
            UserVO userVO = null;
            if (userIdUserVOListMap.containsKey(userId)) {
                userVO = userIdUserVOListMap.get(userId).get(0);
            }
            if (userVO != null) {
                chatFriendApplyVO.setUserName(userVO.getUserName());
                chatFriendApplyVO.setUserAvatar(userVO.getUserAvatar());
            }
            return chatFriendApplyVO;
        }).collect(Collectors.toList());
    }

    /**
     * 分页获取好友申请视图类
     *
     * @param userFriendApplyPage 好友申请分页对象
     * @param request             请求
     * @return {@link Page<ChatFriendApplyVO>}
     */
    @Override
    public Page<ChatFriendApplyVO> getUserFriendApplyVOPage(Page<UserFriendApply> userFriendApplyPage, HttpServletRequest request) {
        List<UserFriendApply> records = userFriendApplyPage.getRecords();
        Page<ChatFriendApplyVO> voPage = new Page<>(userFriendApplyPage.getCurrent(), userFriendApplyPage.getSize(), userFriendApplyPage.getTotal());
        if (CollUtil.isEmpty(records)) {
            return voPage;
        }
        voPage.setRecords(getUserFriendApplyVO(records, request));
        return voPage;
    }

    @Override
    public Long applyFriend(UserFriendApply apply, Long userId) {
        if (apply == null || userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        validUserFriendApply(apply);

        Long targetId = apply.getTargetId();
        ThrowUtils.throwIf(targetId.equals(userId), ErrorCode.PARAMS_ERROR, "不能添加自己为好友");

        // 校验是否已经是好友，避免重复申请
        ThrowUtils.throwIf(userFriendService.isMutualFriend(userId, targetId), ErrorCode.OPERATION_ERROR, "已经是好友了");

        // 幂等校验：检查是否有待审核的相同申请
        UserFriendApply existing = this.getOne(new LambdaQueryWrapper<UserFriendApply>()
                .eq(UserFriendApply::getUserId, userId)
                .eq(UserFriendApply::getTargetId, targetId)
                .eq(UserFriendApply::getStatus, 1));
        if (existing != null) {
            return existing.getId();
        }

        apply.setUserId(userId);
        apply.setStatus(1); // 1-待处理
        this.save(apply);
        chatMqProducer.sendFriendApply(targetId, getUserFriendApplyVO(apply, null), "friend_apply:" + apply.getId());
        return apply.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approveFriend(ChatFriendApproveRequest request, Long userId) {
        if (request == null || userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long applyId = request.getApplyId();
        Integer status = request.getStatus();
        ThrowUtils.throwIf(status != 2 && status != 3, ErrorCode.PARAMS_ERROR);

        UserFriendApply apply = this.getById(applyId);
        ThrowUtils.throwIf(apply == null, ErrorCode.NOT_FOUND_ERROR, "申请记录不存在");

        // 权限与状态校验
        ThrowUtils.throwIf(!apply.getTargetId().equals(userId), ErrorCode.NO_AUTH_ERROR);
        ThrowUtils.throwIf(apply.getStatus() != 1, ErrorCode.PARAMS_ERROR, "已处理过该申请");

        if (status == 3) {
            apply.setStatus(status);
            return this.updateById(apply);
        }

        // 同意申请：建立双向好友并初始化私聊房间
        apply.setStatus(status);
        userFriendService.addFriend(apply.getUserId(), apply.getTargetId());
        chatRoomService.getOrCreatePrivateRoom(apply.getUserId(), apply.getTargetId());

        // 发送 WebSocket 通知
        String bizId = "friend_approve:" + apply.getId();
        chatMqProducer.sendFriendApprove(apply.getUserId(), getUserFriendApplyVO(apply, null), bizId);

        return this.updateById(apply);
    }

    @Override
    public Page<ChatFriendApplyVO> listFriendApplyPage(long current, long size, Long userId) {
        Page<UserFriendApply> page = this.page(new Page<>(current, size),
                new LambdaQueryWrapper<UserFriendApply>()
                        .eq(UserFriendApply::getTargetId, userId)
                        .orderByDesc(UserFriendApply::getCreateTime));

        return getUserFriendApplyVOPage(page, null);
    }
}
