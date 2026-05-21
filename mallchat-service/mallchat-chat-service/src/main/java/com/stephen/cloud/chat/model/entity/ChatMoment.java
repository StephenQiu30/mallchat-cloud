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
 * 动态主体表
 *
 * @author StephenQiu30
 */
@TableName(value = "chat_moment")
@Data
public class ChatMoment implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String content;

    private Integer mediaCount;

    private Integer likeCount;

    private Integer commentCount;

    private Integer visibility;

    private Integer auditStatus;

    private Integer status;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
