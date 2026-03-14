package com.stephen.cloud.chat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.api.chat.model.dto.ChatMessageSendRequest;
import com.stephen.cloud.api.chat.model.vo.ChatMessageVO;
import com.stephen.cloud.chat.model.entity.ChatMessage;

import java.util.List;

/**
 * 聊天消息服务
 *
 * @author StephenQiu30
 */
public interface ChatMessageService extends IService<ChatMessage> {

    /**
     * 发送消息
     *
     * @param chatMessageSendRequest 发送请求
     * @param userId               发送者ID
     * @return 消息ID
     */
    Long sendMessage(ChatMessageSendRequest chatMessageSendRequest, Long userId);

    /**
     * 获取聊天室历史消息 (通过 lastMessageId 实现滚动式翻页优化)
     *
     * @param roomId         房间ID
     * @param lastMessageId  上一页最后一条消息ID (用于优化深分页)
     * @param limit          数量
     * @return 消息列表
     */
    List<ChatMessageVO> listHistoryMessages(Long roomId, Long lastMessageId, Integer limit);
}
