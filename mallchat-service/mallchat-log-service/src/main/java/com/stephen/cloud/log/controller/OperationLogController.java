package com.stephen.cloud.log.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.log.model.dto.LogIdRequest;
import com.stephen.cloud.api.log.model.dto.operation.OperationLogAddRequest;
import com.stephen.cloud.api.log.model.dto.operation.OperationLogQueryRequest;
import com.stephen.cloud.api.log.model.vo.LogOperationResultVO;
import com.stephen.cloud.api.log.model.vo.OperationLogVO;
import com.stephen.cloud.common.auth.annotation.InternalAuth;
import com.stephen.cloud.common.common.*;
import com.stephen.cloud.common.constants.UserConstant;
import com.stephen.cloud.log.convert.LogConvert;
import com.stephen.cloud.log.model.entity.OperationLog;
import com.stephen.cloud.log.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 操作日志接口
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/log/operation")
@Slf4j
@Tag(name = "OperationLogController", description = "操作日志接口")
public class OperationLogController {

    @Resource
    private OperationLogService operationLogService;

    /**
     * 创建操作日志
     *
     * @param request 日志创建请求
     * @return 是否创建成功
     */
    @PostMapping("/add")
    @InternalAuth
    @Operation(summary = "创建操作日志", description = "记录用户操作日志")
    public BaseResponse<LogOperationResultVO> addOperationLog(@RequestBody OperationLogAddRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = operationLogService.addLog(request);
        return ResultUtils.success(LogOperationResultVO.of(result));
    }

    /**
     * 删除操作日志
     *
     * @param request 删除请求
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @Operation(summary = "删除操作日志", description = "删除指定操作日志（仅管理员）")
    public BaseResponse<LogOperationResultVO> deleteOperationLog(@Validated @RequestBody LogIdRequest request) {
        ThrowUtils.throwIf(request == null || request.getId() == null || request.getId() <= 0,
                ErrorCode.PARAMS_ERROR);
        long id = request.getId();
        OperationLog oldLog = operationLogService.getById(id);
        ThrowUtils.throwIf(oldLog == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = operationLogService.removeById(id);
        return ResultUtils.success(LogOperationResultVO.of(result));
    }

    /**
     * 分页获取操作日志列表（仅管理员）
     *
     * @param queryRequest 查询请求
     * @return 分页日志列表
     */
    @PostMapping("/list/page")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @Operation(summary = "分页获取操作日志列表", description = "分页查询操作日志（仅管理员）")
    public BaseResponse<Page<OperationLogVO>> listLogByPage(@RequestBody OperationLogQueryRequest queryRequest) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();
        Page<OperationLog> logPage = operationLogService.page(new Page<>(current, size),
                operationLogService.getQueryWrapper(queryRequest));
        Page<OperationLogVO> voPage = new Page<>(current, size, logPage.getTotal());
        List<OperationLogVO> voList = logPage.getRecords().stream()
                .map(LogConvert::operationLogToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return ResultUtils.success(voPage);
    }
}
