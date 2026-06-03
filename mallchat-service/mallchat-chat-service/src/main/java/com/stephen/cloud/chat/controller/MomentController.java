package com.stephen.cloud.chat.controller;

import com.stephen.cloud.api.chat.model.dto.MomentCreateRequest;
import com.stephen.cloud.api.chat.model.vo.MomentVO;
import com.stephen.cloud.chat.service.ChatMomentService;
import com.stephen.cloud.common.auth.utils.SecurityUtils;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ResultUtils;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.log.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 动态发布接口
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/moments")
@Tag(name = "MomentController", description = "动态发布")
public class MomentController {

    @Resource
    private ChatMomentService chatMomentService;

    @PostMapping
    @OperationLog(module = "动态管理", action = "创建动态")
    @Operation(summary = "创建动态", description = "发布文字或图文动态，可见性默认公开")
    public ResponseEntity<BaseResponse<MomentVO>> createMoment(
            @Validated @RequestBody MomentCreateRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        MomentVO vo = chatMomentService.createMoment(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResultUtils.success(vo));
    }
}
