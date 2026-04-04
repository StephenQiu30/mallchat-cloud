package com.stephen.cloud.chat.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 私聊房间映射表（user_low 为较小用户 ID，user_high 为较大用户 ID，成对唯一）
 *
 * @author StephenQiu30
 * @TableName chat_private_room
 */
@TableName(value = "chat_private_room")
@Data
public class ChatPrivateRoom implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userLow;

    private Long userHigh;

    private Long roomId;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
