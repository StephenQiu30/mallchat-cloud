package com.stephen.cloud.chat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.chat.mapper.ChatAuditEventMapper;
import com.stephen.cloud.chat.model.entity.ChatAuditEvent;
import com.stephen.cloud.chat.service.ChatAuditEventService;
import org.springframework.stereotype.Service;

/**
 * 聊天审计事件服务实现
 *
 * @author StephenQiu30
 */
@Service
public class ChatAuditEventServiceImpl extends ServiceImpl<ChatAuditEventMapper, ChatAuditEvent>
        implements ChatAuditEventService {
}
