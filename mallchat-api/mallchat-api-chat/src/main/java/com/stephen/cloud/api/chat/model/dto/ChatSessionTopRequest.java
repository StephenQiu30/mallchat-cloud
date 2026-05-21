package com.stephen.cloud.api.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 会话置顶请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "会话置顶请求")
public class ChatSessionTopRequest implements Serializable {

    @Schema(description = "房间ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "房间ID不能为空")
    private Long roomId;

    @Schema(description = "置顶状态：0-取消置顶, 1-置顶", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "置顶状态不能为空")
    private Integer status;

    private static final long serialVersionUID = 1L;
}
