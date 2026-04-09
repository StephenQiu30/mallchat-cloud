package com.stephen.cloud.chat.convert;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.chat.model.dto.ChatFriendApplyRequest;
import com.stephen.cloud.api.chat.model.vo.ChatFriendApplyVO;
import com.stephen.cloud.chat.model.entity.UserFriendApply;
import org.springframework.beans.BeanUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 好友申请转换器
 *
 * @author StephenQiu30
 */
public class ChatFriendApplyConvert {

    /**
     * 对象转视图
     *
     * @param userFriendApply 好友申请实体
     * @return 好友申请视图
     */
    public static ChatFriendApplyVO objToVo(UserFriendApply userFriendApply) {
        if (userFriendApply == null) {
            return null;
        }
        ChatFriendApplyVO chatFriendApplyVO = new ChatFriendApplyVO();
        BeanUtils.copyProperties(userFriendApply, chatFriendApplyVO);
        return chatFriendApplyVO;
    }

    /**
     * 对象列表转视图列表
     *
     * @param userFriendApplyList 好友申请对象列表
     * @return 好友申请视图列表
     */
    public static List<ChatFriendApplyVO> getChatFriendApplyVO(List<UserFriendApply> userFriendApplyList) {
        if (CollUtil.isEmpty(userFriendApplyList)) {
            return Collections.emptyList();
        }
        return userFriendApplyList.stream().map(ChatFriendApplyConvert::objToVo).collect(Collectors.toList());
    }

    /**
     * 分页对象转视图分页对象
     *
     * @param userFriendApplyPage 好友申请分页对象
     * @return 好友申请视图分页对象
     */
    public static Page<ChatFriendApplyVO> getChatFriendApplyVO(Page<UserFriendApply> userFriendApplyPage) {
        Page<ChatFriendApplyVO> chatFriendApplyVOPage = new Page<>(userFriendApplyPage.getCurrent(), userFriendApplyPage.getSize(), userFriendApplyPage.getTotal());
        chatFriendApplyVOPage.setRecords(getChatFriendApplyVO(userFriendApplyPage.getRecords()));
        return chatFriendApplyVOPage;
    }

    /**
     * 申请请求转实体对象
     *
     * @param chatFriendApplyRequest 申请请求
     * @return 好友申请实体
     */
    public static UserFriendApply addRequestToObj(ChatFriendApplyRequest chatFriendApplyRequest) {
        if (chatFriendApplyRequest == null) {
            return null;
        }
        UserFriendApply userFriendApply = new UserFriendApply();
        // 映射：ChatFriendApplyRequest.targetId -> UserFriendApply.targetId
        // BeanUtils.copyProperties handles this if field names match
        BeanUtils.copyProperties(chatFriendApplyRequest, userFriendApply);
        return userFriendApply;
    }
}
