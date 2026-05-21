package com.stephen.cloud.api.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 入群申请审核请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "入群申请审核请求")
public class ChatRoomJoinApproveRequest implements Serializable {

    @Schema(description = "申请ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "申请ID不能为空")
    private Long applyId;

    @Schema(description = "审核状态：2-同意，3-拒绝", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "审核状态不能为空")
    private Integer status;

    @Schema(description = "审核留言", example = "欢迎入群")
    private String reviewMsg;

    private static final long serialVersionUID = 1L;
}
