package com.stephen.cloud.chat.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 群成员变更审计表
 *
 * @author StephenQiu30
 */
@TableName(value = "chat_room_member_audit")
@Data
public class ChatRoomMemberAudit implements Serializable {
    /**
     * 审计ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 房间ID
     */
    private Long roomId;

    /**
     * 被操作用户ID
     */
    private Long userId;

    /**
     * 操作类型：JOIN/LEAVE/KICK/GRANT_ADMIN/REVOKE_ADMIN
     */
    private String action;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作时间
     */
    private Date createTime;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
