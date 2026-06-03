package com.stephen.cloud.chat.service;

import com.stephen.cloud.api.chat.model.dto.RecoveryDryRunRequest;
import com.stephen.cloud.api.chat.model.vo.RecoveryDryRunVO;

/**
 * IM 核心数据恢复 dry-run 编排服务。
 */
public interface RecoveryDryRunService {

    RecoveryDryRunVO run(RecoveryDryRunRequest request);
}
