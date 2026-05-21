package com.stephen.cloud.api.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 游标后消息查询请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "游标后消息查询请求")
public class ChatMessageAfterQueryRequest implements Serializable {

    @Schema(description = "房间ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "房间ID不能为空")
    private Long roomId;

    @Schema(description = "客户端最后收到的消息ID")
    private Long afterMessageId;

    @Schema(description = "加载消息数量")
    private Integer limit = 100;

    private static final long serialVersionUID = 1L;
}
