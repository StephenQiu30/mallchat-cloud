package com.stephen.cloud.api.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 历史消息查询请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "历史消息查询请求")
public class ChatMessageHistoryQueryRequest implements Serializable {

    @Schema(description = "房间ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "房间ID不能为空")
    private Long roomId;

    @Schema(description = "上一页最后一条消息ID")
    private Long lastMessageId;

    @Schema(description = "加载消息数量")
    private Integer limit = 20;

    private static final long serialVersionUID = 1L;
}
