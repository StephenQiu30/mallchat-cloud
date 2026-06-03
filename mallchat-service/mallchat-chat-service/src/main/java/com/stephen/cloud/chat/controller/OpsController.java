package com.stephen.cloud.chat.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.stephen.cloud.api.chat.model.dto.RecoveryDryRunRequest;
import com.stephen.cloud.api.chat.model.vo.ImCoreConsistencyCheckVO;
import com.stephen.cloud.api.chat.model.vo.RecoveryDryRunVO;
import com.stephen.cloud.chat.service.ImCoreConsistencyCheckService;
import com.stephen.cloud.chat.service.RecoveryDryRunService;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.ResultUtils;
import com.stephen.cloud.common.constants.UserConstant;
import com.stephen.cloud.common.log.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * IM 运维接口：一致性检查与恢复 dry-run。
 */
@RestController
@RequestMapping("/chat/ops")
@Slf4j
@Tag(name = "OpsController", description = "IM 运维接口（管理员专用：一致性检查、恢复 dry-run、检查点）")
public class OpsController {

    @Resource
    private ImCoreConsistencyCheckService consistencyCheckService;

    @Resource
    private RecoveryDryRunService recoveryDryRunService;

    @PostMapping("/consistency/check")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @OperationLog(module = "IM运维", action = "核心表一致性检查")
    @Operation(summary = "核心表一致性检查", description = "只读检查好友、房间、消息、会话、动态核心表引用完整性，不修改业务数据")
    public BaseResponse<ImCoreConsistencyCheckVO> checkCoreConsistency() {
        return ResultUtils.success(consistencyCheckService.checkAll());
    }

    @PostMapping("/recovery/dry-run")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @OperationLog(module = "IM运维", action = "恢复 dry-run")
    @Operation(summary = "恢复 dry-run", description = "默认 dry-run 仅输出检查点计划；execute 模式在隔离库演练恢复，不修改线上事实库")
    public BaseResponse<RecoveryDryRunVO> recoveryDryRun(@Validated @RequestBody RecoveryDryRunRequest request) {
        return ResultUtils.success(recoveryDryRunService.run(request));
    }
}
