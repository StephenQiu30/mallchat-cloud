package com.stephen.cloud.chat.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 聊天室成员表
 *
 * @author StephenQiu30
 * @TableName chat_room_member
 */
@TableName(value = "chat_room_member")
@Data
public class ChatRoomMember implements Serializable {
    /**
     * 成员ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 房间ID
     */
    private Long roomId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 角色：1-普通成员，2-管理员，3-群主
     */
    private Integer role;

    /**
     * 最后已读消息ID
     */
    private Long lastReadMessageId;

    /**
     * 加入时间
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
