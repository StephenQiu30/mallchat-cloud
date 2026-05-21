package com.stephen.cloud.api.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 房间成员查询请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "房间成员查询请求")
public class ChatRoomMemberQueryRequest implements Serializable {

    @Schema(description = "房间ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "房间ID不能为空")
    private Long roomId;

    private static final long serialVersionUID = 1L;
}
