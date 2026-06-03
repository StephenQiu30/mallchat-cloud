package com.stephen.cloud.chat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.chat.mapper.ChatRoomMemberAuditMapper;
import com.stephen.cloud.chat.model.entity.ChatRoomMemberAudit;
import com.stephen.cloud.chat.service.ChatRoomMemberAuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 群成员变更审计服务实现
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class ChatRoomMemberAuditServiceImpl extends ServiceImpl<ChatRoomMemberAuditMapper, ChatRoomMemberAudit>
        implements ChatRoomMemberAuditService {
}
