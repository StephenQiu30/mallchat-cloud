package com.stephen.cloud.api.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "消息已读上报请求")
public class ChatMessageReadRequest implements Serializable {

    @Schema(description = "房间ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "房间ID不能为空")
    private Long roomId;

    @Schema(description = "已读到的最后一条消息ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "已读消息ID不能为空")
    private Long lastReadMessageId;

    private static final long serialVersionUID = 1L;
}
