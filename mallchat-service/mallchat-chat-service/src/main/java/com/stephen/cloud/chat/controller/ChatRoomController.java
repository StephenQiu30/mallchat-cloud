package com.stephen.cloud.chat.controller;

import com.stephen.cloud.api.chat.model.dto.ChatPrivateRoomRequest;
import com.stephen.cloud.api.chat.model.dto.ChatRoomAddRequest;
import com.stephen.cloud.api.chat.model.dto.ChatRoomInviteRequest;
import com.stephen.cloud.api.chat.model.vo.ChatRoomMemberVO;
import com.stephen.cloud.api.chat.model.vo.ChatRoomVO;
import com.stephen.cloud.api.chat.model.enums.ChatRoomTypeEnum;
import com.stephen.cloud.chat.convert.ChatRoomConvert;
import com.stephen.cloud.chat.model.entity.ChatRoom;
import com.stephen.cloud.chat.service.ChatRoomService;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 聊天室接口
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/chat/room")
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
    @Operation(summary = "创建群聊", description = "创建一个新的群聊并初始化成员")
    public BaseResponse<Long> addChatRoom(@Validated @RequestBody ChatRoomAddRequest chatRoomAddRequest) {
        // 请求参数非空校验
        ThrowUtils.throwIf(chatRoomAddRequest == null, ErrorCode.PARAMS_ERROR);
        // 将 DTO 转换为实体
        ChatRoom chatRoom = ChatRoomConvert.addRequestToObj(chatRoomAddRequest);
        chatRoom.setType(ChatRoomTypeEnum.GROUP.getCode());
        // 调用业务层执行创建逻辑
        Long userId = SecurityUtils.getLoginUserId();
        Long roomId = chatRoomService.addChatRoom(chatRoom, chatRoomAddRequest.getMemberIds(),
                chatRoomAddRequest.getAnnouncement(), userId);
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
    public BaseResponse<List<ChatRoomVO>> listUserChatRooms() {
        Long userId = SecurityUtils.getLoginUserId();
        List<ChatRoomVO> rooms = chatRoomService.listUserChatRooms(userId);
        return ResultUtils.success(rooms);
    }

    /**
     * 获取房间详情
     */
    @GetMapping("/detail")
    @Operation(summary = "获取房间详情", description = "获取群聊或私聊详情")
    public BaseResponse<ChatRoomVO> getRoomDetail(@Parameter(description = "房间ID", required = true) @RequestParam Long roomId) {
        ThrowUtils.throwIf(roomId == null || roomId <= 0, ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        return ResultUtils.success(chatRoomService.getRoomDetail(roomId, userId));
    }

    /**
     * 获取房间成员
     */
    @GetMapping("/member/list")
    @Operation(summary = "获取房间成员", description = "获取指定房间的成员列表")
    public BaseResponse<List<ChatRoomMemberVO>> listRoomMembers(@Parameter(description = "房间ID", required = true) @RequestParam Long roomId) {
        ThrowUtils.throwIf(roomId == null || roomId <= 0, ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        return ResultUtils.success(chatRoomService.listRoomMembers(roomId, userId));
    }

    /**
     * 邀请成员入群
     */
    @PostMapping("/invite")
    @OperationLog(module = "聊天室管理", action = "邀请成员")
    @Operation(summary = "邀请成员入群", description = "邀请自己的好友加入指定群聊")
    public BaseResponse<Boolean> inviteMembers(@Validated @RequestBody ChatRoomInviteRequest request) {
        ThrowUtils.throwIf(request == null || request.getRoomId() == null, ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        chatRoomService.inviteMembers(request.getRoomId(), request.getMemberIds(), userId);
        return ResultUtils.success(true);
    }

    /**
     * 退出群聊
     */
    @PostMapping("/quit")
    @OperationLog(module = "聊天室管理", action = "退出群聊")
    @Operation(summary = "退出群聊", description = "当前用户退出指定群聊")
    public BaseResponse<Boolean> quitRoom(@Parameter(description = "房间ID", required = true) @RequestParam Long roomId) {
        ThrowUtils.throwIf(roomId == null || roomId <= 0, ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        chatRoomService.quitRoom(roomId, userId);
        return ResultUtils.success(true);
    }

    /**
     * 解散群聊
     */
    @PostMapping("/dismiss")
    @OperationLog(module = "聊天室管理", action = "解散群聊")
    @Operation(summary = "解散群聊", description = "群主解散指定群聊")
    public BaseResponse<Boolean> dismissRoom(@Parameter(description = "房间ID", required = true) @RequestParam Long roomId) {
        ThrowUtils.throwIf(roomId == null || roomId <= 0, ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        chatRoomService.dismissRoom(roomId, userId);
        return ResultUtils.success(true);
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
    public BaseResponse<Boolean> joinChatRoom(@Parameter(description = "房间ID", required = true) @RequestParam Long roomId) {
        ThrowUtils.throwIf(roomId == null || roomId <= 0, ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
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
    @Operation(summary = "获取或创建私聊房间", description = "获取与指定好友的唯一私聊房间，若不存在则初始化（UnionID 级别唯一）")
    public BaseResponse<Long> getOrCreatePrivateRoom(@Validated @RequestBody ChatPrivateRoomRequest request) {
        // 校验目标用户 ID
        ThrowUtils.throwIf(request == null || request.getPeerUserId() == null, ErrorCode.PARAMS_ERROR);
        // 获取当前登录用户 ID
        Long userId = SecurityUtils.getLoginUserId();
        // 执行私聊房间获取/创建逻辑 (内部包含双向同步锁)
        Long roomId = chatRoomService.getOrCreatePrivateRoom(request.getPeerUserId(), userId);
        return ResultUtils.success(roomId);
    }
}
