package com.stephen.cloud.chat.support;

import com.stephen.cloud.chat.model.entity.ChatAuditEvent;
import com.stephen.cloud.chat.service.ChatAuditEventService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 聊天审计事件记录器
 * 异步记录敏感操作的审计事件
 *
 * @author StephenQiu30
 */
@Component
@Slf4j
public class ChatAuditEventRecorder {

    @Resource
    private ChatAuditEventService chatAuditEventService;

    /**
     * 记录审计事件（可被子类覆盖用于测试）
     *
     * @param event 审计事件
     */
    public void record(ChatAuditEvent event) {
        try {
            chatAuditEventService.save(event);
        } catch (Exception e) {
            log.error("[ChatAuditEventRecorder] 记录审计事件失败, action={}, targetId={}",
                    event.getAction(), event.getTargetId(), e);
        }
    }

    /**
     * 异步记录审计事件
     *
     * @param event 审计事件
     */
    @Async
    public void recordAsync(ChatAuditEvent event) {
        record(event);
    }
}
