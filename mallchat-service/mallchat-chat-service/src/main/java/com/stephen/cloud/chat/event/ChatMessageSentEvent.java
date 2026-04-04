package com.stephen.cloud.chat.event;

import com.stephen.cloud.chat.model.entity.ChatMessage;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 聊天消息发送事件
 *
 * @author StephenQiu30
 */
@Getter
public class ChatMessageSentEvent extends ApplicationEvent {

    private final ChatMessage chatMessage;
    private final Long userId;

    public ChatMessageSentEvent(Object source, ChatMessage chatMessage, Long userId) {
        super(source);
        this.chatMessage = chatMessage;
        this.userId = userId;
    }
}
