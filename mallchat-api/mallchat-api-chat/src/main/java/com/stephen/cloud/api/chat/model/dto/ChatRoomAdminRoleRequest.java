package com.stephen.cloud.api.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 群管理员任免请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "群管理员任免请求")
public class ChatRoomAdminRoleRequest implements Serializable {

    @Schema(description = "房间ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "房间ID不能为空")
    private Long roomId;

    @Schema(description = "成员用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "成员ID不能为空")
    private Long memberId;

    private static final long serialVersionUID = 1L;
}
