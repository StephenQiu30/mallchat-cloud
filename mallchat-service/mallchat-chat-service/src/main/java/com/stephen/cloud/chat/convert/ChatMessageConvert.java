package com.stephen.cloud.chat.convert;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.chat.model.dto.ChatMessageSendRequest;
import com.stephen.cloud.api.chat.model.vo.ChatMessageVO;
import com.stephen.cloud.chat.model.entity.ChatMessage;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天消息转换器
 *
 * @author StephenQiu30
 */
public class ChatMessageConvert {

    /**
     * 对象转视图
     *
     * @param chatMessage 聊天消息实体
     * @return 聊天消息视图
     */
    public static ChatMessageVO objToVo(ChatMessage chatMessage) {
        if (chatMessage == null) {
            return null;
        }
        ChatMessageVO chatMessageVO = new ChatMessageVO();
        BeanUtils.copyProperties(chatMessage, chatMessageVO);
        return chatMessageVO;
    }

    /**
     * 对象列表转视图列表
     *
     * @param chatMessageList 聊天消息对象列表
     * @return 聊天消息视图列表
     */
    public static List<ChatMessageVO> getChatMessageVO(List<ChatMessage> chatMessageList) {
        return chatMessageList.stream().map(ChatMessageConvert::objToVo).collect(Collectors.toList());
    }

    /**
     * 分页对象转视图分页对象
     *
     * @param chatMessagePage 聊天消息分页对象
     * @return 聊天消息视图分页对象
     */
    public static Page<ChatMessageVO> getChatMessageVO(Page<ChatMessage> chatMessagePage) {
        Page<ChatMessageVO> chatMessageVOPage = new Page<>(chatMessagePage.getCurrent(), chatMessagePage.getSize(), chatMessagePage.getTotal());
        chatMessageVOPage.setRecords(getChatMessageVO(chatMessagePage.getRecords()));
        return chatMessageVOPage;
    }

    /**
     * 发送请求转实体对象
     *
     * @param chatMessageSendRequest 发送请求
     * @return 聊天消息实体
     */
    public static ChatMessage addRequestToObj(ChatMessageSendRequest chatMessageSendRequest) {
        if (chatMessageSendRequest == null) {
            return null;
        }
        ChatMessage chatMessage = new ChatMessage();
        BeanUtils.copyProperties(chatMessageSendRequest, chatMessage);
        return chatMessage;
    }
}
