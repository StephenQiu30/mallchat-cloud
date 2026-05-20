package com.stephen.cloud.chat.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.chat.model.dto.ChatMomentPublishRequest;
import com.stephen.cloud.api.chat.model.vo.ChatMomentVO;
import com.stephen.cloud.chat.service.ChatMomentService;
import com.stephen.cloud.common.auth.utils.SecurityUtils;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ResultUtils;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.log.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 动态接口
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/chat/moment")
@Tag(name = "ChatMomentController", description = "动态管理")
public class ChatMomentController {

    @Resource
    private ChatMomentService chatMomentService;

    @PostMapping("/publish")
    @OperationLog(module = "动态管理", action = "发布动态")
    @Operation(summary = "发布动态", description = "发布文字或图片动态")
    public BaseResponse<Long> publish(@Validated @RequestBody ChatMomentPublishRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        return ResultUtils.success(chatMomentService.publish(userId, request));
    }

    @GetMapping("/list")
    @Operation(summary = "动态列表", description = "查询自己和好友可见动态")
    public BaseResponse<Page<ChatMomentVO>> listVisibleMoments(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = SecurityUtils.getLoginUserId();
        return ResultUtils.success(chatMomentService.listVisibleMoments(userId, current, pageSize));
    }

    @DeleteMapping("/delete")
    @OperationLog(module = "动态管理", action = "删除动态")
    @Operation(summary = "删除动态", description = "删除自己的动态")
    public BaseResponse<Boolean> deleteMoment(@Parameter(description = "动态ID", required = true) @RequestParam Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        chatMomentService.deleteMoment(userId, id);
        return ResultUtils.success(true);
    }
}
