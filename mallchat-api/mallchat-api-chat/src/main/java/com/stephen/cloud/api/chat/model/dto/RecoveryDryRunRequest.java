package com.stephen.cloud.api.chat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * IM 核心数据恢复 dry-run 请求。
 */
@Data
@Schema(description = "IM 核心数据恢复 dry-run 请求")
public class RecoveryDryRunRequest implements Serializable {

    @Schema(description = "备份 SQL 文件路径（dry-run 预览时可空）")
    private String backupFile;

    @Schema(description = "隔离恢复库名，仅允许字母数字下划线")
    private String recoveryDatabase;

    @Schema(description = "是否仅 dry-run（不创建/写入恢复库）", defaultValue = "true")
    private Boolean dryRun = Boolean.TRUE;
}
