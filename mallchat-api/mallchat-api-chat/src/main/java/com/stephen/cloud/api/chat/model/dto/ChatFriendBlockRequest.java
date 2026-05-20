package com.stephen.cloud.api.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 好友拉黑请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "好友拉黑请求")
public class ChatFriendBlockRequest implements Serializable {

    @Schema(description = "目标用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "目标用户ID不能为空")
    private Long targetUserId;

    private static final long serialVersionUID = 1L;
}
