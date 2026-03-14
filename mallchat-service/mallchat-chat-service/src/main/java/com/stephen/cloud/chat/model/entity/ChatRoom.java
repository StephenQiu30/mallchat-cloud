package com.stephen.cloud.chat.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 聊天室表
 *
 * @author StephenQiu30
 * @TableName chat_room
 */
@TableName(value = "chat_room")
@Data
public class ChatRoom implements Serializable {
    /**
     * 房间ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 房间名称
     */
    private String name;

    /**
     * 房间类型：1-群聊，2-私聊
     */
    private Integer type;

    /**
     * 房间头像
     */
    private String avatar;

    /**
     * 创建者用户ID
     */
    private Long createUser;

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
