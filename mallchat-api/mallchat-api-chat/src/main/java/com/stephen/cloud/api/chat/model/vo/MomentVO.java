package com.stephen.cloud.api.chat.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 动态视图
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "动态视图")
public class MomentVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "动态ID")
    private Long id;

    @Schema(description = "发布用户ID")
    private Long userId;

    @Schema(description = "动态正文")
    private String content;

    @Schema(description = "媒体数量")
    private Integer mediaCount;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "评论数")
    private Integer commentCount;

    @Schema(description = "可见范围：0-好友可见，1-公开")
    private Integer visibility;

    @Schema(description = "媒体列表")
    private List<ChatMomentMediaVO> mediaList;

    @Schema(description = "创建时间")
    private Date createTime;
}
