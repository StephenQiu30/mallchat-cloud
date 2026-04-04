package com.stephen.cloud.chat.controller;

import com.stephen.cloud.api.chat.model.vo.ChatSessionVO;
import com.stephen.cloud.chat.service.ChatSessionService;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 会话列表接口
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/session")
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
    @GetMapping("/user/list")
    @Operation(summary = "用户消息列表", description = "获取当前登录用户的所有消息会话列表")
    public BaseResponse<List<ChatSessionVO>> listMySessions() {
        Long userId = SecurityUtils.getLoginUserId();
        List<ChatSessionVO> list = chatSessionService.listMySessions(userId);
        return ResultUtils.success(list);
    }

    /**
     * 置顶/取消置顶会话
     *
     * @param roomId 房间 ID
     * @param status 状态 (0-取消置顶, 1-置顶)
     * @return 是否成功
     */
    @PostMapping("/top")
    @OperationLog(module = "会话管理", action = "置顶会话")
    @Operation(summary = "置顶会话", description = "修改会话置顶状态")
    public BaseResponse<Boolean> topSession(@Parameter(description = "房间ID", required = true) @RequestParam Long roomId,
                                         @Parameter(description = "置顶状态", required = true) @RequestParam Integer status) {
        ThrowUtils.throwIf(roomId == null || status == null, ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        boolean result = chatSessionService.topSession(roomId, userId, status);
        return ResultUtils.success(result);
    }

    /**
     * 删除会话
     *
     * @param roomId 房间ID
     * @return 是否成功
     */
    @DeleteMapping("/remove")
    @OperationLog(module = "会话管理", action = "删除会话")
    @Operation(summary = "删除会话", description = "在列表中移除选中的会话")
    public BaseResponse<Boolean> deleteSession(@Parameter(description = "房间ID", required = true) @RequestParam Long roomId) {
        ThrowUtils.throwIf(roomId == null || roomId <= 0, ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        boolean ok = chatSessionService.deleteSession(roomId, userId);
        return ResultUtils.success(ok);
    }
}
