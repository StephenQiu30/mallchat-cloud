package com.stephen.cloud.api.chat.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * IM 核心数据恢复 dry-run 结果。
 */
@Data
@Schema(description = "IM 核心数据恢复 dry-run 结果")
public class RecoveryDryRunVO implements Serializable {

    @Schema(description = "是否 dry-run（未修改线上库）")
    private boolean dryRun;

    @Schema(description = "是否全部检查点成功")
    private boolean passed;

    @Schema(description = "隔离恢复库名")
    private String recoveryDatabase;

    @Schema(description = "备份文件路径")
    private String backupFile;

    @Schema(description = "检查点列表")
    private List<RecoveryCheckpointVO> checkpoints;

    @Schema(description = "恢复库一致性检查结果（execute 模式）")
    private ImCoreConsistencyCheckVO consistencyCheck;
}
