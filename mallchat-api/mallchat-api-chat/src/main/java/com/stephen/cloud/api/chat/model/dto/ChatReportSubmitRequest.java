package com.stephen.cloud.api.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 举报提交请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "举报提交请求")
public class ChatReportSubmitRequest implements Serializable {

    @Schema(description = "举报对象类型：1-用户，2-消息，3-动态", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "举报对象类型不能为空")
    private Integer targetType;

    @Schema(description = "举报对象ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "举报对象ID不能为空")
    private Long targetId;

    @Schema(description = "举报原因类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "举报原因类型不能为空")
    private String reasonType;

    @Schema(description = "举报说明")
    private String reason;

    private static final long serialVersionUID = 1L;
}
