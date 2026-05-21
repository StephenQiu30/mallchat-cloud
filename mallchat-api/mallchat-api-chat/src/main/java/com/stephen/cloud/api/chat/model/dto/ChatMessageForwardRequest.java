package com.stephen.cloud.api.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 单条消息转发请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "单条消息转发请求")
public class ChatMessageForwardRequest implements Serializable {

    /**
     * 来源消息ID
     */
    @Schema(description = "来源消息ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "来源消息ID不能为空")
    private Long sourceMessageId;

    /**
     * 目标房间ID
     */
    @Schema(description = "目标房间ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "目标房间ID不能为空")
    private Long targetRoomId;

    /**
     * 客户端消息ID
     */
    @Schema(description = "客户端消息ID，用于幂等控制", requiredMode = Schema.RequiredMode.REQUIRED, example = "pc-1710000000000-forward-1")
    @NotNull(message = "客户端消息ID不能为空")
    private String clientMsgId;

    private static final long serialVersionUID = 1L;
}
