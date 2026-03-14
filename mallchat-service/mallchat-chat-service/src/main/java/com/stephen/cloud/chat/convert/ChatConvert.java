package com.stephen.cloud.chat.convert;

import cn.hutool.core.bean.BeanUtil;
import com.stephen.cloud.api.chat.model.dto.ChatMessageSendRequest;
import com.stephen.cloud.api.chat.model.dto.ChatRoomAddRequest;
import com.stephen.cloud.api.chat.model.vo.ChatMessageVO;
import com.stephen.cloud.api.chat.model.vo.ChatRoomVO;
import com.stephen.cloud.chat.model.entity.ChatMessage;
import com.stephen.cloud.chat.model.entity.ChatRoom;

/**
 * 聊天相关对象转换器
 *
 * @author StephenQiu30
 */
public class ChatConvert {

    /**
     * ChatRoomAddRequest -> ChatRoom
     *
     * @param request 请求参数
     * @return 聊天室实体
     */
    public static ChatRoom requestToChatRoom(ChatRoomAddRequest request) {
        if (request == null) {
            return null;
        }
        ChatRoom chatRoom = new ChatRoom();
        BeanUtil.copyProperties(request, chatRoom);
        return chatRoom;
    }

    /**
     * ChatRoom -> ChatRoomVO
     *
     * @param chatRoom 聊天室实体
     * @return 聊天室 VO
     */
    public static ChatRoomVO objToVo(ChatRoom chatRoom) {
        if (chatRoom == null) {
            return null;
        }
        return ChatRoomVO.builder()
                .id(chatRoom.getId())
                .name(chatRoom.getName())
                .type(chatRoom.getType())
                .avatar(chatRoom.getAvatar())
                .createTime(chatRoom.getCreateTime())
                .build();
    }

    /**
     * ChatMessageSendRequest -> ChatMessage
     *
     * @param request 发送消息请求
     * @return 消息实体
     */
    public static ChatMessage requestToChatMessage(ChatMessageSendRequest request) {
        if (request == null) {
            return null;
        }
        ChatMessage chatMessage = new ChatMessage();
        BeanUtil.copyProperties(request, chatMessage);
        return chatMessage;
    }

    /**
     * ChatMessage -> ChatMessageVO
     *
     * @param chatMessage 消息实体
     * @return 消息 VO
     */
    public static ChatMessageVO objToVo(ChatMessage chatMessage) {
        if (chatMessage == null) {
            return null;
        }
        return ChatMessageVO.builder()
                .id(chatMessage.getId())
                .roomId(chatMessage.getRoomId())
                .fromUserId(chatMessage.getFromUserId())
                .content(chatMessage.getContent())
                .type(chatMessage.getType())
                .createTime(chatMessage.getCreateTime())
                .build();
    }
}
