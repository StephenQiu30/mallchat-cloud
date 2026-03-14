package com.stephen.cloud.chat.controller;

import com.stephen.cloud.api.chat.model.dto.ChatMessageSendRequest;
import com.stephen.cloud.api.chat.model.dto.ChatRoomAddRequest;
import com.stephen.cloud.api.chat.model.vo.ChatMessageVO;
import com.stephen.cloud.api.chat.model.vo.ChatRoomVO;
import com.stephen.cloud.chat.service.ChatMessageService;
import com.stephen.cloud.chat.service.ChatRoomService;
import com.stephen.cloud.common.auth.utils.SecurityUtils;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.ResultUtils;
import com.stephen.cloud.common.log.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 聊天室接口
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/chat")
@Slf4j
@Tag(name = "ChatRoomController", description = "聊天室管理")
public class ChatRoomController {

    @Resource
    private ChatRoomService chatRoomService;

    @Resource
    private ChatMessageService chatMessageService;

    /**
     * 创建聊天室 (群聊或私聊)
     *
     * @param chatRoomAddRequest 创建请求
     * @return 房间 ID
     */
    @PostMapping("/room/add")
    @OperationLog(module = "聊天管理", action = "创建聊天室")
    @Operation(summary = "创建聊天室", description = "创建一个新的聊天室（群聊或私聊）")
    public BaseResponse<Long> addChatRoom(@RequestBody ChatRoomAddRequest chatRoomAddRequest) {
        Long roomId = chatRoomService.addChatRoom(chatRoomAddRequest);
        return ResultUtils.success(roomId);
    }

    /**
     * 获取当前用户的聊天室列表
     *
     * @return 聊天室列表
     */
    @GetMapping("/room/list")
    @Operation(summary = "获取当前用户的聊天室列表", description = "获取当前登录用户参与的所有聊天室")
    public BaseResponse<List<ChatRoomVO>> listUserChatRooms() {
        Long userId = SecurityUtils.getLoginUserId();
        List<ChatRoomVO> rooms = chatRoomService.listUserChatRooms(userId);
        return ResultUtils.success(rooms);
    }

    /**
     * 加入聊天室
     *
     * @param roomId 房间 ID
     * @return 是否成功
     */
    @PostMapping("/room/join")
    @OperationLog(module = "聊天管理", action = "加入聊天室")
    @Operation(summary = "加入聊天室", description = "将当前用户加入到指定的聊天室")
    public BaseResponse<Boolean> joinChatRoom(@Parameter(description = "房间ID", required = true) @RequestParam Long roomId) {
        Long userId = SecurityUtils.getLoginUserId();
        chatRoomService.joinChatRoom(roomId, userId);
        return ResultUtils.success(true);
    }

    /**
     * 发送聊天记录
     *
     * @param chatMessageSendRequest 发送消息请求
     * @return 消息 ID
     */
    @PostMapping("/message/send")
    @OperationLog(module = "聊天管理", action = "发送消息")
    @Operation(summary = "发送消息", description = "向指定房间发送一条消息（支持文本、图片、文件）")
    public BaseResponse<Long> sendMessage(@RequestBody ChatMessageSendRequest chatMessageSendRequest) {
        Long userId = SecurityUtils.getLoginUserId();
        Long messageId = chatMessageService.sendMessage(chatMessageSendRequest, userId);
        return ResultUtils.success(messageId);
    }

    /**
     * 获取聊天室历史消息
     *
     * @param roomId        房间 ID
     * @param lastMessageId 上一页最后一条消息 ID
     * @param limit         数量
     * @return 历史消息列表
     */
    @GetMapping("/message/history")
    @Operation(summary = "获取历史消息", description = "获取指定房间的历史聊天记录（支持滚动翻页优化）")
    public BaseResponse<List<ChatMessageVO>> listHistoryMessages(
            @Parameter(description = "房间ID", required = true) @RequestParam Long roomId,
            @Parameter(description = "上一页最后一条消息ID") @RequestParam(required = false) Long lastMessageId,
            @Parameter(description = "加载消息数量", example = "20") @RequestParam(defaultValue = "20") Integer limit) {
        List<ChatMessageVO> history = chatMessageService.listHistoryMessages(roomId, lastMessageId, limit);
        return ResultUtils.success(history);
    }
}
