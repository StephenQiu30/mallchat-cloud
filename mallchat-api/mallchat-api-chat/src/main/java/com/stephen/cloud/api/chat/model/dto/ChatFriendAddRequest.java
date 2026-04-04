package com.stephen.cloud.api.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "添加好友请求")
public class ChatFriendAddRequest implements Serializable {

    @Schema(description = "好友用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long friendUserId;

    private static final long serialVersionUID = 1L;
}
