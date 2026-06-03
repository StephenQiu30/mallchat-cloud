package com.stephen.cloud.api.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 消息投递状态查询请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "消息投递状态查询请求")
public class ChatMessageDeliveryStatusRequest implements Serializable {

    @Schema(description = "消息ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "消息ID不能为空")
    private Long messageId;

    private static final long serialVersionUID = 1L;
}
