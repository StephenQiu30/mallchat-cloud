package com.stephen.cloud.api.chat.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 恢复 dry-run 检查点。
 */
@Data
@Schema(description = "恢复 dry-run 检查点")
public class RecoveryCheckpointVO implements Serializable {

    @Schema(description = "检查点名称")
    private String name;

    @Schema(description = "检查点状态：planned/running/success/failure")
    private String status;

    @Schema(description = "说明")
    private String detail;
}
