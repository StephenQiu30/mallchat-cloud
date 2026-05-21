package com.stephen.cloud.api.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 消息搜索请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "消息搜索请求")
public class ChatMessageSearchRequest implements Serializable {

    @Schema(description = "房间ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "房间ID不能为空")
    private Long roomId;

    @Schema(description = "关键词", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "关键词不能为空")
    private String keyword;

    @Schema(description = "当前页号")
    private Long current = 1L;

    @Schema(description = "页面大小")
    private Long pageSize = 20L;

    private static final long serialVersionUID = 1L;
}
