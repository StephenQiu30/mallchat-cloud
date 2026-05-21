package com.stephen.cloud.api.chat.model.dto;

import com.stephen.cloud.common.common.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 入群申请分页查询请求
 *
 * @author StephenQiu30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "入群申请分页查询请求")
public class ChatRoomJoinApplyQueryRequest extends PageRequest implements Serializable {

    @Schema(description = "房间ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "房间ID不能为空")
    private Long roomId;

    private static final long serialVersionUID = 1L;
}
