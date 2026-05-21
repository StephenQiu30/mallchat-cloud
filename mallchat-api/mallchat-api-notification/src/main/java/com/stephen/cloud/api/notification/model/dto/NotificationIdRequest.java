package com.stephen.cloud.api.notification.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通知 ID 请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "通知 ID 请求")
public class NotificationIdRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 通知ID
     */
    @NotNull(message = "通知ID不能为空")
    @Schema(description = "通知ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;
}
