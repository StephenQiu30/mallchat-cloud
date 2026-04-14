package com.stephen.cloud.api.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 群聊邀请成员请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "群聊邀请成员请求")
public class ChatRoomInviteRequest implements Serializable {

    @Schema(description = "房间ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "房间ID不能为空")
    private Long roomId;

    @Schema(description = "待邀请成员ID列表", requiredMode = Schema.RequiredMode.REQUIRED, example = "[2,3]")
    @NotEmpty(message = "邀请成员不能为空")
    private List<Long> memberIds;

    private static final long serialVersionUID = 1L;
}
