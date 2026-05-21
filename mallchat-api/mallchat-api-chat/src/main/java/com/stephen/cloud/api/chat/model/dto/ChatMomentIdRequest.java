package com.stephen.cloud.api.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 动态 ID 请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "动态 ID 请求")
public class ChatMomentIdRequest implements Serializable {

    @Schema(description = "动态ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "动态ID不能为空")
    private Long id;

    private static final long serialVersionUID = 1L;
}
