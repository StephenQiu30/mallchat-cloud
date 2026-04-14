package com.stephen.cloud.chat.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.api.chat.model.vo.ChatMessageVO;
import com.stephen.cloud.chat.model.entity.ChatMessage;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 聊天消息服务
 * <p>
 * 负责消息持久化、历史拉取（成员校验）与已读游标更新；实时下发由 MQ 推送。
 * </p>
 *
 * @author StephenQiu30
 */
public interface ChatMessageService extends IService<ChatMessage> {

    /**
     * 校验聊天消息数据
     *
     * @param chatMessage 聊天消息实体
     */
    void validChatMessage(ChatMessage chatMessage);

    /**
     * 获取聊天消息视图类
     *
     * @param chatMessage 聊天消息
     * @param request     请求
     * @return {@link ChatMessageVO}
     */
    ChatMessageVO getChatMessageVO(ChatMessage chatMessage, HttpServletRequest request);

    /**
     * 获取聊天消息视图类列表
     *
     * @param chatMessageList 聊天消息列表
     * @param request         请求
     * @return {@link List<ChatMessageVO>}
     */
    List<ChatMessageVO> getChatMessageVO(List<ChatMessage> chatMessageList, HttpServletRequest request);

    /**
     * 分页获取聊天消息视图类
     *
     * @param chatMessagePage 聊天消息分页对象
     * @param request         请求
     * @return {@link Page<ChatMessageVO>}
     */
    Page<ChatMessageVO> getChatMessageVOPage(Page<ChatMessage> chatMessagePage, HttpServletRequest request);

    /**
     * 发送消息
     *
     * @param chatMessage 消息实体
     * @param userId      发送者 ID
     * @return 消息视图
     */
    ChatMessageVO sendMessage(ChatMessage chatMessage, Long userId);

    /**
     * 获取聊天室历史消息
     *
     * @param roomId        房间 ID
     * @param lastMessageId 上一页最后一条消息 ID
     * @param limit         数量
     * @param userId        当前用户 ID
     * @return 历史消息列表
     */
    List<ChatMessageVO> listHistoryMessages(Long roomId, Long lastMessageId, Integer limit, Long userId);

    /**
     * 上报已读游标
     *
     * @param roomId            房间 ID
     * @param lastReadMessageId 已读到的消息 ID
     * @param userId            当前用户 ID
     * @return 是否更新成功
     */
    boolean markMessageRead(Long roomId, Long lastReadMessageId, Long userId);

    /**
     * 撤回消息
     *
     * @param messageId 消息 ID
     * @param userId    发送者用户 ID
     * @return 是否撤回成功
     */
    boolean recallMessage(Long messageId, Long userId);
}
