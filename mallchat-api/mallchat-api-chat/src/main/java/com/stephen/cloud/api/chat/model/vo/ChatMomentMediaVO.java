package com.stephen.cloud.api.chat.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 动态媒体视图
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "动态媒体视图")
public class ChatMomentMediaVO implements Serializable {

    @Schema(description = "媒体ID")
    private Long id;

    @Schema(description = "动态ID")
    private Long momentId;

    @Schema(description = "媒体URL")
    private String url;

    @Schema(description = "图片宽度")
    private Integer width;

    @Schema(description = "图片高度")
    private Integer height;

    @Schema(description = "文件大小")
    private Long size;

    @Schema(description = "排序")
    private Integer sortOrder;

    private static final long serialVersionUID = 1L;
}
