package com.stephen.cloud.api.log.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 日志资源 ID 请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "日志资源 ID 请求")
public class LogIdRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 日志ID
     */
    @NotNull(message = "日志ID不能为空")
    @Schema(description = "日志ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;
}
