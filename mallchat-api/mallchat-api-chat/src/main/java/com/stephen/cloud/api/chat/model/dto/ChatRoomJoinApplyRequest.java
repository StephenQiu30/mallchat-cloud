package com.stephen.cloud.api.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 入群申请请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "入群申请请求")
public class ChatRoomJoinApplyRequest implements Serializable {

    @Schema(description = "房间ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "房间ID不能为空")
    private Long roomId;

    @Schema(description = "申请留言", example = "我是张三")
    private String msg;

    private static final long serialVersionUID = 1L;
}
