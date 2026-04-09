package com.stephen.cloud.chat.listener;

import cn.hutool.core.collection.CollUtil;
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
import java.util.stream.Collectors;

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
     * 监听消息发送事件，自动更新会话列表
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

        // 1. 获取房间所有成员
        List<ChatRoomMember> members = chatRoomMemberService.listByRoomId(roomId);
        if (CollUtil.isEmpty(members)) {
            return;
        }

        // 2. 批量更新所有成员的会话列表 (优化：由循环单次更新改为批量更新)
        List<Long> userIds = members.stream().map(ChatRoomMember::getUserId).collect(Collectors.toList());
        chatSessionService.updateSessionBatch(userIds, roomId, chatMessage.getId(), userId);
    }
}
