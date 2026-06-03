package com.stephen.cloud.chat.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.chat.model.dto.ChatReportListRequest;
import com.stephen.cloud.api.chat.model.dto.ChatReportSubmitRequest;
import com.stephen.cloud.api.chat.model.vo.ChatIdVO;
import com.stephen.cloud.chat.model.entity.ChatReport;
import com.stephen.cloud.chat.service.ChatReportService;
import com.stephen.cloud.common.auth.utils.SecurityUtils;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ResultUtils;
import com.stephen.cloud.common.common.ThrowUtils;
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
 * 聊天举报接口
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/chat/report")
@Slf4j
@Tag(name = "ChatReportController", description = "聊天举报管理")
public class ChatReportController {

    @Resource
    private ChatReportService chatReportService;

    @PostMapping("/submit")
    @OperationLog(module = "举报管理", action = "提交举报")
    @Operation(summary = "提交举报", description = "举报用户、消息或动态")
    public BaseResponse<ChatIdVO> submitReport(@Validated @RequestBody ChatReportSubmitRequest request) {
        Long userId = SecurityUtils.getLoginUserId();
        return ResultUtils.success(ChatIdVO.of(chatReportService.submitReport(userId, request)));
    }

    @PostMapping("/list")
    @Operation(summary = "举报列表", description = "管理员查询举报列表")
    public BaseResponse<Page<ChatReport>> listReports(@Validated @RequestBody ChatReportListRequest request) {
        ThrowUtils.throwIf(!SecurityUtils.isAdmin(), ErrorCode.NO_AUTH_ERROR, "仅管理员可查询举报列表");
        return ResultUtils.success(chatReportService.listReports(request));
    }
}
