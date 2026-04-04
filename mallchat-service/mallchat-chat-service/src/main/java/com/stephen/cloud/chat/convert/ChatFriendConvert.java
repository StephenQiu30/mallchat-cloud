package com.stephen.cloud.chat.convert;

import com.stephen.cloud.api.chat.model.vo.ChatFriendUserVO;
import com.stephen.cloud.chat.model.entity.UserFriend;
import org.springframework.beans.BeanUtils;

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
}
