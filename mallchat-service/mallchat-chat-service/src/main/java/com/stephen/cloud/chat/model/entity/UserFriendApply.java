package com.stephen.cloud.chat.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 好友申请表
 *
 * @author StephenQiu30
 * @TableName user_friend_apply
 */
@TableName(value = "user_friend_apply")
@Data
public class UserFriendApply implements Serializable {
    /**
     * 申请ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 发起用户ID
     */
    private Long userId;

    /**
     * 目标用户ID
     */
    private Long targetId;

    /**
     * 申请消息
     */
    private String msg;

    /**
     * 状态：1-待处理，2-已同意，3-已忽略
     */
    private Integer status;

    /**
     * 申请时间
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
