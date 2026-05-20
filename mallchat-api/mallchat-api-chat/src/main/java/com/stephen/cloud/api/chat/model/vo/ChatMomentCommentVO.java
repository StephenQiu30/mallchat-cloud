package com.stephen.cloud.api.chat.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 动态评论视图
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "动态评论视图")
public class ChatMomentCommentVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "评论ID")
    private Long id;

    @Schema(description = "动态ID")
    private Long momentId;

    @Schema(description = "评论用户ID")
    private Long userId;

    @Schema(description = "评论正文")
    private String content;

    @Schema(description = "创建时间")
    private Date createTime;
}
