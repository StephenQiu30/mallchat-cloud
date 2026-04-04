package com.stephen.cloud.chat.controller;

import com.stephen.cloud.api.chat.model.dto.ChatFriendAddRequest;
import com.stephen.cloud.api.chat.model.vo.ChatFriendUserVO;
import com.stephen.cloud.chat.service.ChatRoomService;
import com.stephen.cloud.chat.service.UserFriendService;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ResultUtils;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.log.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 聊天好友接口
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/chat_friend")
@Slf4j
@Tag(name = "ChatFriendController", description = "聊天好友管理")
public class ChatFriendController {

    @Resource
    private UserFriendService userFriendService;

    @Resource
    private ChatRoomService chatRoomService;

    /**
     * 添加好友（双向关系，幂等）
     *
     * @param request 好友用户 ID
     * @param servletRequest HTTP 请求
     * @return 是否成功
     */
    @PostMapping("/add")
    @OperationLog(module = "好友管理", action = "添加好友")
    @Operation(summary = "添加好友", description = "与指定用户建立好友关系")
    public BaseResponse<Boolean> addFriend(@Validated @RequestBody ChatFriendAddRequest request,
                                           HttpServletRequest servletRequest) {
        ThrowUtils.throwIf(request == null || request.getFriendUserId() == null, ErrorCode.PARAMS_ERROR);
        Long userId = chatRoomService.getLoginUserId(servletRequest);
        userFriendService.addFriend(userId, request.getFriendUserId());
        return ResultUtils.success(true);
    }

    /**
     * 当前用户好友列表
     *
     * @param servletRequest HTTP 请求
     * @return 好友视图列表
     */
    @GetMapping("/list/vo")
    @Operation(summary = "好友列表", description = "获取当前用户的好友简要信息")
    public BaseResponse<List<ChatFriendUserVO>> listFriends(HttpServletRequest servletRequest) {
        Long userId = chatRoomService.getLoginUserId(servletRequest);
        return ResultUtils.success(userFriendService.listFriends(userId));
    }
}
