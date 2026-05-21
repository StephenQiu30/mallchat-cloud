package com.stephen.cloud.api.notification.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 业务通知创建请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "业务通知创建请求")
public class NotificationCreateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "通知标题不能为空")
    @Schema(description = "通知标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @NotBlank(message = "通知内容不能为空")
    @Schema(description = "通知内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @NotBlank(message = "通知类型不能为空")
    @Schema(description = "通知类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private String type;

    @Schema(description = "业务幂等ID")
    private String bizId;

    @NotNull(message = "接收用户ID不能为空")
    @Schema(description = "接收用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;

    @Schema(description = "关联对象ID")
    private Long relatedId;

    @Schema(description = "关联对象类型")
    private String relatedType;

    @Schema(description = "跳转链接")
    private String contentUrl;
}
