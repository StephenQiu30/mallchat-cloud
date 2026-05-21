package com.stephen.cloud.api.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 会话免打扰请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "会话免打扰请求")
public class ChatSessionMuteRequest implements Serializable {

    @Schema(description = "房间ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "房间ID不能为空")
    private Long roomId;

    @Schema(description = "免打扰状态：0-关闭，1-开启", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "免打扰状态不能为空")
    private Integer muteStatus;

    private static final long serialVersionUID = 1L;
}
