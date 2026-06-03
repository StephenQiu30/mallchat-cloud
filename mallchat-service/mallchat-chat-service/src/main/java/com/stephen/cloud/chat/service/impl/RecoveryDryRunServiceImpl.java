package com.stephen.cloud.chat.service.impl;

import com.stephen.cloud.api.chat.model.dto.RecoveryDryRunRequest;
import com.stephen.cloud.api.chat.model.vo.ImCoreConsistencyCheckVO;
import com.stephen.cloud.api.chat.model.vo.RecoveryCheckpointVO;
import com.stephen.cloud.api.chat.model.vo.RecoveryDryRunVO;
import com.stephen.cloud.chat.ops.ImCoreConsistencyAssertions;
import com.stephen.cloud.chat.ops.RecoveryDryRunExecutor;
import com.stephen.cloud.chat.service.RecoveryDryRunService;
import com.stephen.cloud.chat.support.OpsMetricsRecorder;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.exception.BusinessException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * IM 核心数据恢复 dry-run 编排实现。
 */
@Slf4j
@Service
public class RecoveryDryRunServiceImpl implements RecoveryDryRunService {

    private static final List<String> CHECKPOINT_NAMES = List.of(
            "validate_inputs",
            "create_recovery_database",
            "restore_backup",
            "run_consistency_assertions",
            "cleanup_recovery_database"
    );

    @Resource
    private RecoveryDryRunExecutor recoveryDryRunExecutor;

    @Resource
    private OpsMetricsRecorder opsMetricsRecorder;

    @Override
    public RecoveryDryRunVO run(RecoveryDryRunRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean dryRun = request.getDryRun() == null || Boolean.TRUE.equals(request.getDryRun());
        String recoveryDatabase = normalizeRecoveryDatabase(request.getRecoveryDatabase());
        String backupFile = StringUtils.trimToNull(request.getBackupFile());

        List<RecoveryCheckpointVO> checkpoints = new ArrayList<>();
        checkpoints.add(checkpoint("validate_inputs", "running", "校验备份路径与恢复库名"));

        if (!dryRun) {
            ThrowUtils.throwIf(backupFile == null, ErrorCode.PARAMS_ERROR, "非 dry-run 模式必须提供 backupFile");
            Path backupPath = Path.of(backupFile);
            if (!Files.isRegularFile(backupPath)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "备份文件不存在: " + backupFile);
            }
        }

        markCheckpoint(checkpoints, 0, "success", dryRun ? "dry-run 参数校验通过" : "execute 参数校验通过");
        opsMetricsRecorder.recordRecovery("validate_inputs", "success");

        if (dryRun) {
            for (int i = 1; i < CHECKPOINT_NAMES.size(); i++) {
                String detail = switch (CHECKPOINT_NAMES.get(i)) {
                    case "create_recovery_database" -> "计划在隔离库 " + recoveryDatabase + " 执行";
                    case "restore_backup" -> backupFile == null ? "等待 backupFile" : "计划从 " + backupFile + " 恢复";
                    case "run_consistency_assertions" ->
                            "计划执行 " + ImCoreConsistencyAssertions.ALL.size() + " 条一致性断言";
                    case "cleanup_recovery_database" -> "计划清理隔离库 " + recoveryDatabase;
                    default -> "planned";
                };
                checkpoints.add(checkpoint(CHECKPOINT_NAMES.get(i), "planned", detail));
            }
            opsMetricsRecorder.recordRecovery("plan", "success");

            RecoveryDryRunVO vo = new RecoveryDryRunVO();
            vo.setDryRun(true);
            vo.setPassed(true);
            vo.setRecoveryDatabase(recoveryDatabase);
            vo.setBackupFile(backupFile);
            vo.setCheckpoints(checkpoints);
            return vo;
        }

        checkpoints.add(checkpoint("create_recovery_database", "running", "创建隔离恢复库"));
        checkpoints.add(checkpoint("restore_backup", "running", "导入备份"));
        checkpoints.add(checkpoint("run_consistency_assertions", "running", "执行一致性断言"));
        checkpoints.add(checkpoint("cleanup_recovery_database", "planned", "完成后清理隔离库"));

        ImCoreConsistencyCheckVO consistencyCheck;
        try {
            consistencyCheck = recoveryDryRunExecutor.restoreAndVerify(backupFile, recoveryDatabase);
            markCheckpoint(checkpoints, 1, "success", "隔离库已创建");
            markCheckpoint(checkpoints, 2, "success", "备份已导入");
            markCheckpoint(checkpoints, 3, consistencyCheck.isPassed() ? "success" : "failure",
                    "断言完成，passed=" + consistencyCheck.isPassed());
            markCheckpoint(checkpoints, 4, "success", "隔离库已清理");
            opsMetricsRecorder.recordRecovery("execute", consistencyCheck.isPassed() ? "success" : "failure");
        } catch (RuntimeException ex) {
            opsMetricsRecorder.recordRecovery("execute", "failure");
            log.error("[RecoveryDryRun] 隔离库恢复演练失败, recoveryDatabase={}", recoveryDatabase, ex);
            throw ex;
        }

        RecoveryDryRunVO vo = new RecoveryDryRunVO();
        vo.setDryRun(false);
        vo.setPassed(consistencyCheck.isPassed());
        vo.setRecoveryDatabase(recoveryDatabase);
        vo.setBackupFile(backupFile);
        vo.setCheckpoints(checkpoints);
        vo.setConsistencyCheck(consistencyCheck);
        return vo;
    }

    static String normalizeRecoveryDatabase(String recoveryDatabase) {
        String normalized = StringUtils.defaultIfBlank(recoveryDatabase, "mallchat_recovery_dryrun");
        if (!normalized.matches("^[A-Za-z0-9_]+$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "RECOVERY_DATABASE 只能包含字母、数字和下划线");
        }
        return normalized;
    }

    private static RecoveryCheckpointVO checkpoint(String name, String status, String detail) {
        RecoveryCheckpointVO checkpoint = new RecoveryCheckpointVO();
        checkpoint.setName(name);
        checkpoint.setStatus(status);
        checkpoint.setDetail(detail);
        return checkpoint;
    }

    private static void markCheckpoint(List<RecoveryCheckpointVO> checkpoints, int index, String status, String detail) {
        RecoveryCheckpointVO checkpoint = checkpoints.get(index);
        checkpoint.setStatus(status);
        checkpoint.setDetail(detail);
    }
}
