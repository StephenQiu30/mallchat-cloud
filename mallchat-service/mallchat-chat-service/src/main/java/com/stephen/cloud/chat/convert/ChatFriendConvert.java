package com.stephen.cloud.chat.convert;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.chat.model.vo.ChatFriendUserVO;
import com.stephen.cloud.chat.model.entity.UserFriend;
import org.springframework.beans.BeanUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天好友转换器
 *
 * @author StephenQiu30
 */
public class ChatFriendConvert {

    /**
     * 对象转视图
     *
     * @param userFriend 好友实体
     * @return {@link ChatFriendUserVO}
     */
    public static ChatFriendUserVO objToVo(UserFriend userFriend) {
        if (userFriend == null) {
            return null;
        }
        ChatFriendUserVO chatFriendUserVO = new ChatFriendUserVO();
        BeanUtils.copyProperties(userFriend, chatFriendUserVO);
        return chatFriendUserVO;
    }

    /**
     * 对象列表转视图列表
     *
     * @param userFriendList 好友对象列表
     * @return 好友视图列表
     */
    public static List<ChatFriendUserVO> objToVo(List<UserFriend> userFriendList) {
        if (CollUtil.isEmpty(userFriendList)) {
            return Collections.emptyList();
        }
        return userFriendList.stream().map(ChatFriendConvert::objToVo).collect(Collectors.toList());
    }

    /**
     * 分页对象转视图分页对象
     *
     * @param userFriendPage 好友分页对象
     * @return 好友视图分页对象
     */
    public static Page<ChatFriendUserVO> objToVo(Page<UserFriend> userFriendPage) {
        Page<ChatFriendUserVO> chatFriendUserVOPage = new Page<>(userFriendPage.getCurrent(), userFriendPage.getSize(), userFriendPage.getTotal());
        chatFriendUserVOPage.setRecords(objToVo(userFriendPage.getRecords()));
        return chatFriendUserVOPage;
    }
}
