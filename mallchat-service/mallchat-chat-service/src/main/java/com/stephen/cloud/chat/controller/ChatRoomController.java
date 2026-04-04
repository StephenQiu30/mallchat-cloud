package com.stephen.cloud.chat.controller;

import com.stephen.cloud.api.chat.model.dto.ChatPrivateRoomRequest;
import com.stephen.cloud.api.chat.model.dto.ChatRoomAddRequest;
import com.stephen.cloud.api.chat.model.vo.ChatRoomVO;
import com.stephen.cloud.chat.convert.ChatRoomConvert;
import com.stephen.cloud.chat.model.entity.ChatRoom;
import com.stephen.cloud.chat.service.ChatRoomService;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ResultUtils;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.log.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 聊天室接口
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/chat_room")
@Slf4j
@Tag(name = "ChatRoomController", description = "聊天室管理")
public class ChatRoomController {

    @Resource
    private ChatRoomService chatRoomService;

    /**
     * 创建聊天室 (群聊或私聊)
     *
     * @param chatRoomAddRequest 创建请求
     * @return 房间 ID
     */
    @PostMapping("/add")
    @OperationLog(module = "聊天室管理", action = "创建聊天室")
    @Operation(summary = "创建聊天室", description = "创建一个新的聊天室（群聊或私聊）")
    public BaseResponse<Long> addChatRoom(@Validated @RequestBody ChatRoomAddRequest chatRoomAddRequest) {
        ThrowUtils.throwIf(chatRoomAddRequest == null, ErrorCode.PARAMS_ERROR);
        ChatRoom chatRoom = ChatRoomConvert.addRequestToObj(chatRoomAddRequest);
        Long roomId = chatRoomService.addChatRoom(chatRoom);
        return ResultUtils.success(roomId);
    }

    /**
     * 获取当前用户的聊天室列表
     *
     * @param request 请求对象
     * @return 聊天室列表
     */
    @GetMapping("/list/vo")
    @Operation(summary = "获取当前用户的聊天室列表", description = "获取当前登录用户参与的所有聊天室")
    public BaseResponse<List<ChatRoomVO>> listUserChatRooms(HttpServletRequest request) {
        Long userId = chatRoomService.getLoginUserId(request);
        List<ChatRoomVO> rooms = chatRoomService.listUserChatRooms(userId);
        return ResultUtils.success(rooms);
    }

    /**
     * 加入聊天室
     *
     * @param roomId  房间 ID
     * @param request 请求对象
     * @return 是否成功
     */
    @PostMapping("/join")
    @OperationLog(module = "聊天室管理", action = "加入聊天室")
    @Operation(summary = "加入聊天室", description = "将当前用户加入到指定的聊天室")
    public BaseResponse<Boolean> joinChatRoom(@Parameter(description = "房间ID", required = true) @RequestParam Long roomId,
                                              HttpServletRequest request) {
        ThrowUtils.throwIf(roomId == null || roomId <= 0, ErrorCode.PARAMS_ERROR);
        Long userId = chatRoomService.getLoginUserId(request);
        chatRoomService.joinChatRoom(roomId, userId);
        return ResultUtils.success(true);
    }

    /**
     * 获取或创建与好友的私聊房间
     *
     * @param request        请求参数
     * @param servletRequest HTTP 请求
     * @return 房间 ID
     */
    @PostMapping("/private")
    @OperationLog(module = "聊天室管理", action = "私聊房间")
    @Operation(summary = "获取或创建私聊房间", description = "与好友建立唯一私聊会话")
    public BaseResponse<Long> getOrCreatePrivateRoom(@Validated @RequestBody ChatPrivateRoomRequest request,
                                                     HttpServletRequest servletRequest) {
        ThrowUtils.throwIf(request == null || request.getPeerUserId() == null, ErrorCode.PARAMS_ERROR);
        Long userId = chatRoomService.getLoginUserId(servletRequest);
        Long roomId = chatRoomService.getOrCreatePrivateRoom(request.getPeerUserId(), userId);
        return ResultUtils.success(roomId);
    }
}
