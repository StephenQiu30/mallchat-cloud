package com.stephen.cloud.chat.controller;

import com.stephen.cloud.api.chat.model.dto.ChatPrivateRoomRequest;
import com.stephen.cloud.api.chat.model.dto.ChatRoomAdminRoleRequest;
import com.stephen.cloud.api.chat.model.dto.ChatRoomDetailRequest;
import com.stephen.cloud.api.chat.model.dto.ChatRoomIdRequest;
import com.stephen.cloud.api.chat.model.dto.ChatRoomMemberRemoveRequest;
import com.stephen.cloud.api.chat.model.dto.ChatRoomMemberQueryRequest;
import com.stephen.cloud.api.chat.model.dto.ChatRoomUpdateRequest;
import com.stephen.cloud.api.chat.model.dto.ChatRoomAddRequest;
import com.stephen.cloud.api.chat.model.dto.ChatRoomInviteRequest;
import com.stephen.cloud.api.chat.model.vo.ChatIdVO;
import com.stephen.cloud.api.chat.model.vo.ChatOperationResultVO;
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
import com.stephen.cloud.common.exception.BusinessException;
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
    public BaseResponse<ChatIdVO> addChatRoom(@Validated @RequestBody ChatRoomAddRequest chatRoomAddRequest) {
        ThrowUtils.throwIf(chatRoomAddRequest == null, ErrorCode.PARAMS_ERROR);
        ChatRoom chatRoom = ChatRoomConvert.addRequestToObj(chatRoomAddRequest);
        chatRoom.setType(ChatRoomTypeEnum.GROUP.getCode());
        Long userId = SecurityUtils.getLoginUserId();
        Long roomId = chatRoomService.addChatRoom(chatRoom, chatRoomAddRequest.getMemberIds(),
                chatRoomAddRequest.getAnnouncement(), userId);
        return ResultUtils.success(ChatIdVO.of(roomId));
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
    public BaseResponse<ChatRoomVO> getRoomDetail(@Validated ChatRoomDetailRequest request) {
        ThrowUtils.throwIf(request == null || request.getRoomId() == null || request.getRoomId() <= 0,
                ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        return ResultUtils.success(chatRoomService.getRoomDetail(request.getRoomId(), userId));
    }

    /**
     * 获取房间成员
     */
    @GetMapping("/member/list")
    @Operation(summary = "获取房间成员", description = "获取指定房间的成员列表")
    public BaseResponse<List<ChatRoomMemberVO>> listRoomMembers(@Validated ChatRoomMemberQueryRequest request) {
        ThrowUtils.throwIf(request == null || request.getRoomId() == null || request.getRoomId() <= 0,
                ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        return ResultUtils.success(chatRoomService.listRoomMembers(request.getRoomId(), userId));
    }

    /**
     * 邀请成员入群
     */
    @PostMapping("/invite")
    @OperationLog(module = "聊天室管理", action = "邀请成员")
    @Operation(summary = "邀请成员入群", description = "邀请自己的好友加入指定群聊")
    public BaseResponse<ChatOperationResultVO> inviteMembers(@Validated @RequestBody ChatRoomInviteRequest request) {
        ThrowUtils.throwIf(request == null || request.getRoomId() == null, ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        chatRoomService.inviteMembers(request.getRoomId(), request.getMemberIds(), userId);
        return ResultUtils.success(ChatOperationResultVO.of(true));
    }

    /**
     * 更新群聊资料
     */
    @PostMapping("/update")
    @OperationLog(module = "聊天室管理", action = "更新群聊资料")
    @Operation(summary = "更新群聊资料", description = "仅群主可更新群名称、头像和群公告")
    public BaseResponse<ChatOperationResultVO> updateGroupProfile(@Validated @RequestBody ChatRoomUpdateRequest request) {
        ThrowUtils.throwIf(request == null || request.getRoomId() == null, ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        chatRoomService.updateGroupProfile(
                request.getRoomId(),
                request.getName(),
                request.getAvatar(),
                request.getAnnouncement(),
                userId);
        return ResultUtils.success(ChatOperationResultVO.of(true));
    }

    /**
     * 移除群成员
     */
    @PostMapping("/member/remove")
    @OperationLog(module = "聊天室管理", action = "移除群成员")
    @Operation(summary = "移除群成员", description = "仅群主可移除普通群成员")
    public BaseResponse<ChatOperationResultVO> removeMember(@Validated @RequestBody ChatRoomMemberRemoveRequest request) {
        ThrowUtils.throwIf(request == null || request.getRoomId() == null || request.getMemberId() == null,
                ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        chatRoomService.removeMember(request.getRoomId(), request.getMemberId(), userId);
        return ResultUtils.success(ChatOperationResultVO.of(true));
    }

    /**
     * 任命群管理员
     */
    @PostMapping("/member/admin/grant")
    @OperationLog(module = "聊天室管理", action = "任命群管理员")
    @Operation(summary = "任命群管理员", description = "群主任命普通成员为管理员")
    public BaseResponse<ChatOperationResultVO> grantAdmin(@Validated @RequestBody ChatRoomAdminRoleRequest request) {
        ThrowUtils.throwIf(request == null || request.getRoomId() == null || request.getMemberId() == null,
                ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        chatRoomService.grantAdmin(request.getRoomId(), request.getMemberId(), userId);
        return ResultUtils.success(ChatOperationResultVO.of(true));
    }

    /**
     * 取消群管理员
     */
    @PostMapping("/member/admin/revoke")
    @OperationLog(module = "聊天室管理", action = "取消群管理员")
    @Operation(summary = "取消群管理员", description = "群主取消管理员角色")
    public BaseResponse<ChatOperationResultVO> revokeAdmin(@Validated @RequestBody ChatRoomAdminRoleRequest request) {
        ThrowUtils.throwIf(request == null || request.getRoomId() == null || request.getMemberId() == null,
                ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        chatRoomService.revokeAdmin(request.getRoomId(), request.getMemberId(), userId);
        return ResultUtils.success(ChatOperationResultVO.of(true));
    }

    /**
     * 退出群聊
     */
    @PostMapping("/quit")
    @OperationLog(module = "聊天室管理", action = "退出群聊")
    @Operation(summary = "退出群聊", description = "当前用户退出指定群聊")
    public BaseResponse<ChatOperationResultVO> quitRoom(@Validated ChatRoomIdRequest request) {
        ThrowUtils.throwIf(request == null || request.getRoomId() == null || request.getRoomId() <= 0,
                ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        chatRoomService.quitRoom(request.getRoomId(), userId);
        return ResultUtils.success(ChatOperationResultVO.of(true));
    }

    /**
     * 解散群聊
     */
    @PostMapping("/dismiss")
    @OperationLog(module = "聊天室管理", action = "解散群聊")
    @Operation(summary = "解散群聊", description = "群主解散指定群聊")
    public BaseResponse<ChatOperationResultVO> dismissRoom(@Validated ChatRoomIdRequest request) {
        ThrowUtils.throwIf(request == null || request.getRoomId() == null || request.getRoomId() <= 0,
                ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        chatRoomService.dismissRoom(request.getRoomId(), userId);
        return ResultUtils.success(ChatOperationResultVO.of(true));
    }

    /**
     * 加入聊天室
     *
     * @param request 房间 ID 请求
     * @return 操作结果
     */
    @PostMapping("/join")
    @OperationLog(module = "聊天室管理", action = "加入聊天室")
    @Operation(summary = "加入聊天室", description = "当前 MVP 阶段不支持公开加入聊天室，成员进入需走受控路径")
    public BaseResponse<ChatOperationResultVO> joinChatRoom(@Validated ChatRoomIdRequest request) {
        ThrowUtils.throwIf(request == null || request.getRoomId() == null || request.getRoomId() <= 0,
                ErrorCode.PARAMS_ERROR);
        throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "当前版本不支持公开加入聊天室，请通过建群、邀请或私聊初始化进入房间");
    }

    /**
     * 获取或创建与好友的私聊房间
     *
     * @param request 请求参数
     * @return 房间 ID
     */
    @PostMapping("/private")
    @OperationLog(module = "聊天室管理", action = "私聊房间")
    @Operation(summary = "获取或创建私聊房间", description = "获取与指定好友的唯一私聊房间，若不存在则初始化（UnionID 级别唯一）")
    public BaseResponse<ChatIdVO> getOrCreatePrivateRoom(@Validated @RequestBody ChatPrivateRoomRequest request) {
        ThrowUtils.throwIf(request == null || request.getPeerUserId() == null, ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        Long roomId = chatRoomService.getOrCreatePrivateRoom(request.getPeerUserId(), userId);
        return ResultUtils.success(ChatIdVO.of(roomId));
    }
}
