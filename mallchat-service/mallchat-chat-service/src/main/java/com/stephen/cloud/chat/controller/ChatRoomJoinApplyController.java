package com.stephen.cloud.chat.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.chat.model.dto.ChatRoomJoinApplyQueryRequest;
import com.stephen.cloud.api.chat.model.dto.ChatRoomJoinApplyRequest;
import com.stephen.cloud.api.chat.model.dto.ChatRoomJoinApproveRequest;
import com.stephen.cloud.api.chat.model.vo.ChatIdVO;
import com.stephen.cloud.api.chat.model.vo.ChatOperationResultVO;
import com.stephen.cloud.api.chat.model.vo.ChatRoomJoinApplyVO;
import com.stephen.cloud.chat.service.ChatRoomJoinApplyService;
import com.stephen.cloud.common.auth.utils.SecurityUtils;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ResultUtils;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.log.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 入群申请接口
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/chat/room/join/apply")
@Tag(name = "ChatRoomJoinApplyController", description = "入群申请管理")
public class ChatRoomJoinApplyController {

    @Resource
    private ChatRoomJoinApplyService chatRoomJoinApplyService;

    @PostMapping("/add")
    @OperationLog(module = "入群申请", action = "提交入群申请")
    @Operation(summary = "提交入群申请", description = "当前用户申请加入指定群聊")
    public BaseResponse<ChatIdVO> applyJoinRoom(@Validated @RequestBody ChatRoomJoinApplyRequest request) {
        ThrowUtils.throwIf(request == null || request.getRoomId() == null, ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        return ResultUtils.success(ChatIdVO.of(chatRoomJoinApplyService.applyJoinRoom(request, userId)));
    }

    @PostMapping("/approve")
    @OperationLog(module = "入群申请", action = "审核入群申请")
    @Operation(summary = "审核入群申请", description = "群主或管理员审核入群申请")
    public BaseResponse<ChatOperationResultVO> approveJoinRoom(@Validated @RequestBody ChatRoomJoinApproveRequest request) {
        ThrowUtils.throwIf(request == null || request.getApplyId() == null || request.getStatus() == null,
                ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        return ResultUtils.success(ChatOperationResultVO.of(chatRoomJoinApplyService.approveJoinRoom(request, userId)));
    }

    @PostMapping("/list/page/vo")
    @Operation(summary = "分页查询入群申请", description = "群主或管理员查询指定群聊的入群申请")
    public BaseResponse<Page<ChatRoomJoinApplyVO>> listRoomJoinApplyPage(
            @Validated @RequestBody ChatRoomJoinApplyQueryRequest request) {
        ThrowUtils.throwIf(request == null || request.getRoomId() == null, ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        return ResultUtils.success(chatRoomJoinApplyService.listRoomJoinApplyPage(
                request.getRoomId(), request.getCurrent(), request.getPageSize(), userId));
    }
}
