package com.stephen.cloud.chat.controller;

import com.stephen.cloud.api.chat.model.dto.ChatSessionDeleteRequest;
import com.stephen.cloud.api.chat.model.dto.ChatSessionMuteRequest;
import com.stephen.cloud.api.chat.model.dto.ChatSessionTopRequest;
import com.stephen.cloud.api.chat.model.vo.ChatOperationResultVO;
import com.stephen.cloud.api.chat.model.vo.ChatSessionVO;
import com.stephen.cloud.chat.service.ChatSessionService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会话列表接口
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/chat/session")
@Slf4j
@Tag(name = "ChatSessionController", description = "会话管理")
public class ChatSessionController {

    @Resource
    private ChatSessionService chatSessionService;

    /**
     * 获取自己的会话列表
     *
     * @return 会话列表
     */
    @GetMapping("/list/vo")
    @Operation(summary = "用户消息列表", description = "获取当前登录用户的所有消息会话列表（包含未读数、最后一条消息概览）")
    public BaseResponse<List<ChatSessionVO>> listMySessions() {
        Long userId = SecurityUtils.getLoginUserId();
        List<ChatSessionVO> list = chatSessionService.listMySessions(userId);
        return ResultUtils.success(list);
    }

    /**
     * 置顶/取消置顶会话
     *
     * @param request 置顶请求
     * @return 操作结果
     */
    @PostMapping("/top")
    @OperationLog(module = "会话管理", action = "置顶会话")
    @Operation(summary = "置顶会话", description = "修改会话置顶状态")
    public BaseResponse<ChatOperationResultVO> topSession(@Validated ChatSessionTopRequest request) {
        ThrowUtils.throwIf(request == null || request.getRoomId() == null || request.getStatus() == null,
                ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        boolean result = chatSessionService.topSession(request.getRoomId(), userId, request.getStatus());
        return ResultUtils.success(ChatOperationResultVO.of(result));
    }

    /**
     * 开启/关闭会话免打扰
     */
    @PostMapping("/mute")
    @OperationLog(module = "会话管理", action = "会话免打扰")
    @Operation(summary = "会话免打扰", description = "修改指定会话的免打扰状态")
    public BaseResponse<ChatOperationResultVO> muteSession(@Validated @RequestBody ChatSessionMuteRequest request) {
        ThrowUtils.throwIf(request == null || request.getRoomId() == null || request.getMuteStatus() == null,
                ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        return ResultUtils.success(ChatOperationResultVO.of(
                chatSessionService.muteSession(request.getRoomId(), userId, request.getMuteStatus())));
    }

    /**
     * 删除会话
     *
     * @param request 删除请求
     * @return 操作结果
     */
    @PostMapping("/delete")
    @OperationLog(module = "会话管理", action = "删除会话")
    @Operation(summary = "删除会话", description = "在列表中移除选中的会话")
    public BaseResponse<ChatOperationResultVO> deleteSession(@Validated @RequestBody ChatSessionDeleteRequest request) {
        ThrowUtils.throwIf(request == null || request.getRoomId() == null || request.getRoomId() <= 0,
                ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        boolean ok = chatSessionService.deleteSession(request.getRoomId(), userId);
        return ResultUtils.success(ChatOperationResultVO.of(ok));
    }
}
