package com.stephen.cloud.chat.controller;

import com.stephen.cloud.api.chat.model.dto.ChatMessageReadRequest;
import com.stephen.cloud.api.chat.model.dto.ChatMessageSendRequest;
import com.stephen.cloud.api.chat.model.vo.ChatMessageVO;
import com.stephen.cloud.chat.convert.ChatMessageConvert;
import com.stephen.cloud.chat.model.entity.ChatMessage;
import com.stephen.cloud.chat.service.ChatMessageService;
import com.stephen.cloud.common.auth.utils.SecurityUtils;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ResultUtils;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.log.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 聊天消息接口
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/chat_message")
@Slf4j
@Tag(name = "ChatMessageController", description = "聊天消息管理")
public class ChatMessageController {

    @Resource
    private ChatMessageService chatMessageService;

    /**
     * 发送聊天记录
     *
     * @param chatMessageSendRequest 发送消息请求
     * @param request HTTP 请求
     * @return 消息 ID
     */
    @PostMapping("/send")
    @OperationLog(module = "消息管理", action = "发送消息")
    @Operation(summary = "发送消息", description = "向指定房间发送一条消息（支持文本、图片、文件）")
    public BaseResponse<Long> sendMessage(@Validated @RequestBody ChatMessageSendRequest chatMessageSendRequest) {
        ThrowUtils.throwIf(chatMessageSendRequest == null, ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        ChatMessage chatMessage = ChatMessageConvert.addRequestToObj(chatMessageSendRequest);
        Long messageId = chatMessageService.sendMessage(chatMessage, userId);
        return ResultUtils.success(messageId);
    }

    /**
     * 标记消息已读
     *
     * @param request 房间与已读游标
     * @param servletRequest HTTP 请求
     * @return 是否更新成功
     */
    @PostMapping("/read")
    @OperationLog(module = "消息管理", action = "消息已读")
    @Operation(summary = "上报消息已读", description = "更新当前用户在该房间的已读消息 ID")
    public BaseResponse<Boolean> markMessageRead(@Validated @RequestBody ChatMessageReadRequest request) {
        if (request == null || request.getRoomId() == null || request.getLastReadMessageId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long userId = SecurityUtils.getLoginUserId();
        boolean ok = chatMessageService.markMessageRead(request.getRoomId(), request.getLastReadMessageId(), userId);
        return ResultUtils.success(ok);
    }

    /**
     * 获取聊天室历史消息
     *
     * @param roomId        房间 ID
     * @param lastMessageId 上一页最后一条消息 ID
     * @param limit         数量
     * @param request HTTP 请求
     * @return 历史消息列表
     */
    @GetMapping("/history")
    @Operation(summary = "获取历史消息", description = "获取指定房间的历史聊天记录（支持滚动翻页优化）")
    public BaseResponse<List<ChatMessageVO>> listHistoryMessages(
            @Parameter(description = "房间ID", required = true) @RequestParam Long roomId,
            @Parameter(description = "上一页最后一条消息ID") @RequestParam(required = false) Long lastMessageId,
            @Parameter(description = "加载消息数量", example = "20") @RequestParam(defaultValue = "20") Integer limit) {
        ThrowUtils.throwIf(roomId == null || roomId <= 0, ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        List<ChatMessageVO> history = chatMessageService.listHistoryMessages(roomId, lastMessageId, limit, userId);
        return ResultUtils.success(history);
    }

    /**
     * 撤回消息
     *
     * @param id 消息 ID
     * @return 是否成功
     */
    @PutMapping("/recall/{id}")
    @OperationLog(module = "消息管理", action = "撤回消息")
    @Operation(summary = "撤回消息", description = "撤回指定消息（限时 2 分钟内）")
    public BaseResponse<Boolean> recallMessage(@PathVariable("id") Long id) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        boolean ok = chatMessageService.recallMessage(id, userId);
        return ResultUtils.success(ok);
    }
}
