package com.stephen.cloud.api.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "获取或创建私聊房间请求")
public class ChatPrivateRoomRequest implements Serializable {

    @Schema(description = "对方用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long peerUserId;

    private static final long serialVersionUID = 1L;
}
