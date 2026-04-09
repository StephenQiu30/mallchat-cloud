package com.stephen.cloud.chat.controller;

import com.stephen.cloud.api.chat.model.vo.ChatSessionVO;
import com.stephen.cloud.chat.service.ChatSessionService;
import com.stephen.cloud.common.auth.utils.SecurityUtils;
import com.stephen.cloud.common.common.*;
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
        // 获取当前用户 ID
        Long userId = SecurityUtils.getLoginUserId();
        // 查询该用户的全量会话记录，并进行排序处理 (置顶优先，时间倒序)
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
                                            @Parameter(description = "置顶状态：0-取消置顶, 1-置顶", required = true) @RequestParam Integer status) {
        // 参数非空校验
        ThrowUtils.throwIf(roomId == null || status == null, ErrorCode.PARAMS_ERROR);
        // 获取当前用户 ID
        Long userId = SecurityUtils.getLoginUserId();
        // 执行置顶状态更新
        boolean result = chatSessionService.topSession(roomId, userId, status);
        return ResultUtils.success(result);
    }

    /**
     * 删除会话
     *
     * @param deleteRequest 删除请求
     * @return 是否成功
     */
    @PostMapping("/delete")
    @OperationLog(module = "会话管理", action = "删除会话")
    @Operation(summary = "删除会话", description = "在列表中移除选中的会话")
    public BaseResponse<Boolean> deleteSession(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        boolean ok = chatSessionService.deleteSession(deleteRequest.getId(), userId);
        return ResultUtils.success(ok);
    }
}
