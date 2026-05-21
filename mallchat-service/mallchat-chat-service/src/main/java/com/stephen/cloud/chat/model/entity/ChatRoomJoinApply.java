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
 * 入群申请表
 *
 * @author StephenQiu30
 * @TableName chat_room_join_apply
 */
@TableName(value = "chat_room_join_apply")
@Data
public class ChatRoomJoinApply implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roomId;

    private Long userId;

    private Long reviewerId;

    private String msg;

    private String reviewMsg;

    /**
     * 状态：1-待处理，2-已同意，3-已拒绝
     */
    private Integer status;

    /**
     * 待处理幂等键：roomId:userId
     */
    private String activeKey;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
