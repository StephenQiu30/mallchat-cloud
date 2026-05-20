package com.stephen.cloud.chat.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 聊天举报表
 *
 * @author StephenQiu30
 */
@TableName(value = "chat_report")
@Data
public class ChatReport implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long reporterUserId;

    private Integer targetType;

    private Long targetId;

    private Long targetOwnerId;

    private String reasonType;

    private String reason;

    private Integer status;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
