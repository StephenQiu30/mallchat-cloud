package com.stephen.cloud.chat.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.api.chat.model.vo.ChatMessageReadStatusVO;
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
     * 转发单条消息
     *
     * @param sourceMessageId 来源消息 ID
     * @param targetRoomId    目标房间 ID
     * @param clientMsgId     新消息客户端幂等 ID
     * @param userId          当前用户 ID
     * @return 消息视图
     */
    ChatMessageVO forwardMessage(Long sourceMessageId, Long targetRoomId, String clientMsgId, Long userId);

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
     * 获取接收游标之后的新消息
     *
     * @param roomId         房间 ID
     * @param afterMessageId 客户端最后收到的消息 ID
     * @param limit          数量
     * @param userId         当前用户 ID
     * @return 游标后的消息列表
     */
    List<ChatMessageVO> listMessagesAfter(Long roomId, Long afterMessageId, Integer limit, Long userId);

    /**
     * 搜索当前用户可访问房间内的文本消息
     *
     * @param roomId   房间 ID
     * @param keyword  搜索关键词
     * @param current  当前页
     * @param pageSize 每页数量
     * @param userId   当前用户 ID
     * @return 消息分页
     */
    Page<ChatMessageVO> searchMessages(Long roomId, String keyword, long current, long pageSize, Long userId);

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
     * 获取消息已读统计摘要
     *
     * @param roomId    房间 ID
     * @param messageId 消息 ID
     * @param userId    当前用户 ID
     * @return 已读统计摘要
     */
    ChatMessageReadStatusVO getMessageReadStatus(Long roomId, Long messageId, Long userId);

    /**
     * 撤回消息
     *
     * @param messageId 消息 ID
     * @param userId    发送者用户 ID
     * @return 是否撤回成功
     */
    boolean recallMessage(Long messageId, Long userId);
}
