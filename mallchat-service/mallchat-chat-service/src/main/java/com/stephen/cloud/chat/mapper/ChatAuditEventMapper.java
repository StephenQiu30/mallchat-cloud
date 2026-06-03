package com.stephen.cloud.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stephen.cloud.chat.model.entity.ChatAuditEvent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天审计事件 Mapper
 *
 * @author StephenQiu30
 */
@Mapper
public interface ChatAuditEventMapper extends BaseMapper<ChatAuditEvent> {
}
