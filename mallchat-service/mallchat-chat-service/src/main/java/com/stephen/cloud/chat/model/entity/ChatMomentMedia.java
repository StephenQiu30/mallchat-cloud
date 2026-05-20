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
 * 动态媒体表
 *
 * @author StephenQiu30
 */
@TableName(value = "chat_moment_media")
@Data
public class ChatMomentMedia implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long momentId;

    private String url;

    private Integer width;

    private Integer height;

    private Long size;

    private Integer sortOrder;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
