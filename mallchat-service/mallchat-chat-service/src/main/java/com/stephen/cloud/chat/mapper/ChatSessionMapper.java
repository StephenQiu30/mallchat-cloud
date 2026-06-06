package com.stephen.cloud.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stephen.cloud.chat.model.entity.ChatSession;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 会话详情数据库操作
 *
 * @author StephenQiu30
 * @Entity com.stephen.cloud.chat.model.entity.ChatSession
 */
public interface ChatSessionMapper extends BaseMapper<ChatSession> {

    /**
     * 原子更新会话 - 仅当新消息ID大于等于当前lastMessageId时才更新
     * 防止旧消息或重复消息覆盖新状态
     *
     * @param userId          用户ID
     * @param roomId          房间ID
     * @param lastMessageId   最后一条消息ID
     * @param incrementUnread 是否增加未读数
     * @return 影响行数，0表示条件不满足未更新
     */
    @Update("<script>" +
            "UPDATE chat_session SET " +
            "  last_message_id = #{lastMessageId}, " +
            "  active_time = NOW()" +
            "  <if test='incrementUnread'>" +
            "    , unread_count = unread_count + 1" +
            "  </if>" +
            "WHERE user_id = #{userId} " +
            "  AND room_id = #{roomId} " +
            "  AND (last_message_id IS NULL OR last_message_id &lt; #{lastMessageId})" +
            "</script>")
    int atomicUpdateSession(@Param("userId") Long userId,
                            @Param("roomId") Long roomId,
                            @Param("lastMessageId") Long lastMessageId,
                            @Param("incrementUnread") boolean incrementUnread);

    /**
     * 原子批量更新会话 - 使用条件更新防止重复和乱序
     * 使用 CASE WHEN 在 SQL 层面按行区分发送者和接收者，避免 MyBatis if 标签只求值一次的问题
     *
     * @param roomId        房间ID
     * @param lastMessageId 最后一条消息ID
     * @param senderId      发送者ID（不增加未读数）
     * @return 影响行数
     */
    @Update("UPDATE chat_session SET " +
            "  last_message_id = #{lastMessageId}, " +
            "  active_time = NOW(), " +
            "  unread_count = unread_count + CASE WHEN user_id != #{senderId} THEN 1 ELSE 0 END " +
            "WHERE room_id = #{roomId} " +
            "  AND (last_message_id IS NULL OR last_message_id < #{lastMessageId})")
    int atomicUpdateSessionBatch(@Param("roomId") Long roomId,
                                  @Param("lastMessageId") Long lastMessageId,
                                  @Param("senderId") Long senderId);
}
