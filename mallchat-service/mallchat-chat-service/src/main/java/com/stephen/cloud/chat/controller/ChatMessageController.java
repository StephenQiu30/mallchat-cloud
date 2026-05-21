package com.stephen.cloud.chat.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.chat.model.dto.ChatMessageAfterQueryRequest;
import com.stephen.cloud.api.chat.model.dto.ChatMessageForwardRequest;
import com.stephen.cloud.api.chat.model.dto.ChatMessageHistoryQueryRequest;
import com.stephen.cloud.api.chat.model.dto.ChatMessageReadRequest;
import com.stephen.cloud.api.chat.model.dto.ChatMessageReadStatusRequest;
import com.stephen.cloud.api.chat.model.dto.ChatMessageRecallRequest;
import com.stephen.cloud.api.chat.model.dto.ChatMessageSearchRequest;
import com.stephen.cloud.api.chat.model.dto.ChatMessageSendRequest;
import com.stephen.cloud.api.chat.model.vo.ChatMessageReadStatusVO;
import com.stephen.cloud.api.chat.model.vo.ChatMessageVO;
import com.stephen.cloud.api.chat.model.vo.ChatOperationResultVO;
import com.stephen.cloud.chat.convert.ChatMessageConvert;
import com.stephen.cloud.chat.model.entity.ChatMessage;
import com.stephen.cloud.chat.service.ChatMessageService;
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
 * 聊天消息接口
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/chat/message")
@Slf4j
@Tag(name = "ChatMessageController", description = "聊天消息管理")
public class ChatMessageController {

    @Resource
    private ChatMessageService chatMessageService;

    /**
     * 发送聊天记录
     *
     * @param chatMessageSendRequest 发送消息请求
     * @param request                HTTP 请求
     * @return 消息视图
     */
    @PostMapping("/send")
    @OperationLog(module = "消息管理", action = "发送消息")
    @Operation(summary = "发送消息", description = "向指定房间发送一条消息（支持文本、图片、文件、语音、视频、表情）")
    public BaseResponse<ChatMessageVO> sendMessage(@Validated @RequestBody ChatMessageSendRequest chatMessageSendRequest) {
        ThrowUtils.throwIf(chatMessageSendRequest == null, ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        ChatMessage chatMessage = ChatMessageConvert.addRequestToObj(chatMessageSendRequest);
        ChatMessageVO messageVO = chatMessageService.sendMessage(chatMessage, userId);
        return ResultUtils.success(messageVO);
    }

    /**
     * 单条消息转发
     *
     * @param request 转发请求
     * @return 消息视图
     */
    @PostMapping("/forward")
    @OperationLog(module = "消息管理", action = "转发消息")
    @Operation(summary = "转发单条消息", description = "将当前用户可见的单条正常消息转发到目标房间")
    public BaseResponse<ChatMessageVO> forwardMessage(@Validated @RequestBody ChatMessageForwardRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        ChatMessageVO messageVO = chatMessageService.forwardMessage(
                request.getSourceMessageId(), request.getTargetRoomId(), request.getClientMsgId(), userId);
        return ResultUtils.success(messageVO);
    }

    /**
     * 标记消息已读
     *
     * @param request 房间与已读游标
     * @return 操作结果
     */
    @PostMapping("/read")
    @OperationLog(module = "消息管理", action = "消息已读")
    @Operation(summary = "上报消息已读", description = "更新当前用户在该房间的已读消息 ID")
    public BaseResponse<ChatOperationResultVO> markMessageRead(@Validated @RequestBody ChatMessageReadRequest request) {
        ThrowUtils.throwIf(request == null || request.getRoomId() == null || request.getLastReadMessageId() == null,
                ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        boolean ok = chatMessageService.markMessageRead(request.getRoomId(), request.getLastReadMessageId(), userId);
        return ResultUtils.success(ChatOperationResultVO.of(ok));
    }

    /**
     * 获取消息已读统计摘要
     *
     * @param request 查询请求
     * @return 已读统计摘要
     */
    @GetMapping("/read/status")
    @Operation(summary = "获取消息已读统计", description = "发送者查询单条消息的聚合已读/未读人数")
    public BaseResponse<ChatMessageReadStatusVO> getMessageReadStatus(@Validated ChatMessageReadStatusRequest request) {
        ThrowUtils.throwIf(request == null || request.getRoomId() == null || request.getRoomId() <= 0
                || request.getMessageId() == null || request.getMessageId() <= 0, ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        ChatMessageReadStatusVO statusVO = chatMessageService.getMessageReadStatus(
                request.getRoomId(), request.getMessageId(), userId);
        return ResultUtils.success(statusVO);
    }

    /**
     * 获取聊天室历史消息
     *
     * @param request 历史消息查询请求
     * @return 历史消息列表
     */
    @GetMapping("/list/history/vo")
    @Operation(summary = "获取历史消息", description = "获取指定房间的历史聊天记录（支持滚动翻页优化）")
    public BaseResponse<List<ChatMessageVO>> listHistoryMessages(@Validated ChatMessageHistoryQueryRequest request) {
        ThrowUtils.throwIf(request == null || request.getRoomId() == null || request.getRoomId() <= 0,
                ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        List<ChatMessageVO> history = chatMessageService.listHistoryMessages(
                request.getRoomId(), request.getLastMessageId(), request.getLimit(), userId);
        return ResultUtils.success(history);
    }

    /**
     * 获取接收游标后的新消息
     *
     * @param request 游标后消息查询请求
     * @return 游标后的消息列表
     */
    @GetMapping("/list/after/vo")
    @Operation(summary = "获取游标后的新消息", description = "获取指定房间中客户端最后收到消息之后的新消息，用于重连补偿")
    public BaseResponse<List<ChatMessageVO>> listMessagesAfter(@Validated ChatMessageAfterQueryRequest request) {
        ThrowUtils.throwIf(request == null || request.getRoomId() == null || request.getRoomId() <= 0,
                ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        List<ChatMessageVO> messages = chatMessageService.listMessagesAfter(
                request.getRoomId(), request.getAfterMessageId(), request.getLimit(), userId);
        return ResultUtils.success(messages);
    }

    /**
     * 搜索文本消息
     */
    @GetMapping("/search/vo")
    @Operation(summary = "搜索文本消息", description = "在指定房间内按关键词搜索文本消息")
    public BaseResponse<Page<ChatMessageVO>> searchMessages(@Validated ChatMessageSearchRequest request) {
        Long userId = SecurityUtils.getLoginUserId();
        return ResultUtils.success(chatMessageService.searchMessages(
                request.getRoomId(), request.getKeyword(), request.getCurrent(), request.getPageSize(), userId));
    }

    /**
     * 撤回消息
     *
     * @param request 撤回请求
     * @return 操作结果
     */
    @PostMapping("/recall")
    @OperationLog(module = "消息管理", action = "撤回消息")
    @Operation(summary = "撤回消息", description = "撤回指定消息（限时 2 分钟内）")
    public BaseResponse<ChatOperationResultVO> recallMessage(@Validated @RequestBody ChatMessageRecallRequest request) {
        ThrowUtils.throwIf(request == null || request.getMessageId() == null, ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        boolean ok = chatMessageService.recallMessage(request.getMessageId(), userId);
        return ResultUtils.success(ChatOperationResultVO.of(ok));
    }
}
