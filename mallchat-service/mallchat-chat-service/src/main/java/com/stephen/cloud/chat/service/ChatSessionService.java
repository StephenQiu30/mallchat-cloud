package com.stephen.cloud.chat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.api.chat.model.vo.ChatSessionVO;
import com.stephen.cloud.chat.model.entity.ChatSession;

import java.util.List;

/**
 * 聊天会话服务
 *
 * @author StephenQiu30
 */
public interface ChatSessionService extends IService<ChatSession> {

    /**
     * 获取我的会话列表
     *
     * @param userId 当前用户ID
     * @return 会话VO列表
     */
    List<ChatSessionVO> listMySessions(Long userId);

    /**
     * 获取会话视图
     *
     * @param chatSession 会话实体
     * @param userId      当前用户ID
     * @return 会话视图
     */
    ChatSessionVO getChatSessionVO(ChatSession chatSession, Long userId);

    /**
     * 批量获取会话视图
     *
     * @param chatSessionList 会话实体列表
     * @param userId          当前用户ID
     * @return 会话视图列表
     */
    List<ChatSessionVO> getChatSessionVO(List<ChatSession> chatSessionList, Long userId);

    /**
     * 置顶会话
     *
     * @param roomId    房间ID
     * @param userId    当前用户ID
     * @param topStatus 置顶状态
     * @return 是否成功
     */
    boolean topSession(Long roomId, Long userId, Integer topStatus);

    /**
     * 删除会话
     *
     * @param roomId 房间ID
     * @param userId 当前用户ID
     * @return 是否成功
     */
    boolean deleteSession(Long roomId, Long userId);

    /**
     * 自动更新会话（用于消息发送时）
     *
     * @param userId         谁的会话
     * @param roomId         哪个会话
     * @param lastMessageId  最后一条消息ID
     * @param incrementUnread 是否增加未读数
     */
    void updateSession(Long userId, Long roomId, Long lastMessageId, boolean incrementUnread);
}
