package com.stephen.cloud.api.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 删除好友请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "删除好友请求")
public class ChatFriendDeleteRequest implements Serializable {

    @Schema(description = "好友用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "好友用户ID不能为空")
    private Long friendUserId;

    private static final long serialVersionUID = 1L;
}
