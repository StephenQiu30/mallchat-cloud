package com.stephen.cloud.chat.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 会话列表实体
 *
 * @author StephenQiu30
 * @TableName chat_session
 */
@TableName(value = "chat_session")
@Data
public class ChatSession implements Serializable {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 房间ID
     */
    private Long roomId;

    /**
     * 最后一条消息ID
     */
    private Long lastMessageId;

    /**
     * 最后一条已读消息ID
     */
    private Long lastReadMessageId;

    /**
     * 未读数
     */
    private Integer unreadCount;

    /**
     * 置顶状态：0-否，1-是
     */
    private Integer topStatus;

    /**
     * 最后活跃时间
     */
    private Date activeTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
