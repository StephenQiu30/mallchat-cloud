package com.stephen.cloud.api.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "消息已读上报请求")
public class ChatMessageReadRequest implements Serializable {

    @Schema(description = "房间ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long roomId;

    @Schema(description = "已读到的最后一条消息ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long lastReadMessageId;

    private static final long serialVersionUID = 1L;
}
