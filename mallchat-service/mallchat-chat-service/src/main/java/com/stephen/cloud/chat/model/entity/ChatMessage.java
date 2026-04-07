package com.stephen.cloud.chat.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 聊天消息表
 *
 * @author StephenQiu30
 * @TableName chat_message
 */
@TableName(value = "chat_message")
@Data
public class ChatMessage implements Serializable {
    /**
     * 消息ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 房间ID
     */
    private Long roomId;

    /**
     * 发送者ID
     */
    private Long fromUserId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息扩展内容（JSON 字符串）
     */
    private String extra;

    /**
     * 消息类型：1-文本，2-图片，3-文件
     */
    private Integer type;
    
    /**
     * 回复的消息ID
     */
    private Long replyMsgId;

    /**
     * 消息状态：0-正常，1-已撤回，2-已删除
     */
    private Integer status;

    /**
     * 发送时间
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
