package com.stephen.cloud.api.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 动态媒体请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "动态媒体请求")
public class ChatMomentMediaRequest implements Serializable {

    @Schema(description = "媒体URL", example = "https://example.com/1.png")
    private String url;

    @Schema(description = "图片宽度", example = "100")
    private Integer width;

    @Schema(description = "图片高度", example = "80")
    private Integer height;

    @Schema(description = "文件大小", example = "1024")
    private Long size;

    @Schema(description = "排序", example = "0")
    private Integer sortOrder;

    private static final long serialVersionUID = 1L;
}
