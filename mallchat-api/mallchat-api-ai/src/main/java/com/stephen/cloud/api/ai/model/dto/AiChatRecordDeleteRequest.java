package com.stephen.cloud.api.ai.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI 对话记录删除请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "AI 对话记录删除请求")
public class AiChatRecordDeleteRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 记录ID
     */
    @NotNull(message = "记录ID不能为空")
    @Positive(message = "记录ID必须大于0")
    @Schema(description = "记录ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;
}
