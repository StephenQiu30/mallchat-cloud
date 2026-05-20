package com.stephen.cloud.api.notification.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "通知标题")
    private String title;

    @Schema(description = "通知内容")
    private String content;

    @Schema(description = "通知类型")
    private String type;

    @Schema(description = "业务幂等ID")
    private String bizId;

    @Schema(description = "接收用户ID")
    private Long userId;

    @Schema(description = "关联对象ID")
    private Long relatedId;

    @Schema(description = "关联对象类型")
    private String relatedType;

    @Schema(description = "跳转链接")
    private String contentUrl;
}
