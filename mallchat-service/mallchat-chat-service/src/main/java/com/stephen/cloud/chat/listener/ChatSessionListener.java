package com.stephen.cloud.chat.listener;

import com.stephen.cloud.chat.event.ChatMessageSentEvent;
import com.stephen.cloud.chat.model.entity.ChatMessage;
import com.stephen.cloud.chat.model.entity.ChatRoomMember;
import com.stephen.cloud.chat.service.ChatRoomMemberService;
import com.stephen.cloud.chat.service.ChatSessionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 聊天会话监听器
 *
 * @author StephenQiu30
 */
@Component
@Slf4j
public class ChatSessionListener {

    @Resource
    private ChatSessionService chatSessionService;

    @Resource
    private ChatRoomMemberService chatRoomMemberService;

    /**
     * 监听消息发送事件，异步更新会话列表
     *
     * @param event 消息发送事件
     */
    @EventListener
    public void onChatMessageSent(ChatMessageSentEvent event) {
        ChatMessage chatMessage = event.getChatMessage();
        Long userId = event.getUserId();
        Long roomId = chatMessage.getRoomId();

        log.info("[ChatSessionListener] 收到消息发送事件, userId: {}, roomId: {}, messageId: {}", 
                userId, roomId, chatMessage.getId());

        // 获取房间所有成员
        List<ChatRoomMember> members = chatRoomMemberService.listByRoomId(roomId);
        if (members == null || members.isEmpty()) {
            return;
        }

        // 更新所有成员的会话列表
        for (ChatRoomMember member : members) {
            // 发送者不增加未读数，其他人增加
            boolean incrementUnread = !member.getUserId().equals(userId);
            chatSessionService.updateSession(member.getUserId(), roomId, chatMessage.getId(), incrementUnread);
        }
    }
}
