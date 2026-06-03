package com.stephen.cloud.chat.ops;

import com.stephen.cloud.api.chat.model.vo.ImCoreConsistencyCheckVO;

/**
 * 恢复 dry-run 执行端口（隔离库，不触碰线上事实库）。
 */
public interface RecoveryDryRunExecutor {

    ImCoreConsistencyCheckVO restoreAndVerify(String backupFile, String recoveryDatabase);
}
