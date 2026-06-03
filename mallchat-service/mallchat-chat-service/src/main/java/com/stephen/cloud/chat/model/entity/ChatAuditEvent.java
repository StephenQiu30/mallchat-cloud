package com.stephen.cloud.chat.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 聊天审计事件实体
 *
 * @author StephenQiu30
 */
@Data
@TableName("chat_audit_event")
public class ChatAuditEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 操作用户 ID
     */
    private Long userId;

    /**
     * 操作类型（MESSAGE_SEND, MESSAGE_RECALL, USER_BLOCK, MEMBER_REMOVE 等）
     */
    private String action;

    /**
     * 目标类型（MESSAGE, USER, ROOM_MEMBER 等）
     */
    private String targetType;

    /**
     * 目标 ID
     */
    private Long targetId;

    /**
     * 房间 ID（可选）
     */
    private Long roomId;

    /**
     * 操作详情（JSON）
     */
    private String detail;

    /**
     * 客户端 IP
     */
    private String clientIp;

    /**
     * 创建时间
     */
    private Date createTime;
}
