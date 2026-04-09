package com.stephen.cloud.chat.controller;

import com.stephen.cloud.api.chat.model.dto.ChatFriendAddRequest;
import com.stephen.cloud.api.chat.model.vo.ChatFriendUserVO;
import com.stephen.cloud.chat.service.ChatRoomService;
import com.stephen.cloud.chat.service.UserFriendService;
import com.stephen.cloud.common.auth.utils.SecurityUtils;
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
@RequestMapping("/chat/friend")
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
    @Operation(summary = "直接添加好友", description = "跳过申请直接与指定用户建立双向好友关系（通常用于系统加好友或测试）")
    public BaseResponse<Boolean> addFriend(@Validated @RequestBody ChatFriendAddRequest request,
                                           HttpServletRequest servletRequest) {
        // 参数校验
        ThrowUtils.throwIf(request == null || request.getFriendUserId() == null, ErrorCode.PARAMS_ERROR);
        // 获取当前用户 ID
        Long userId = SecurityUtils.getLoginUserId();
        // 执行建立双向好友关系逻辑
        userFriendService.addFriend(userId, request.getFriendUserId());
        return ResultUtils.success(true);
    }

    /**
     * 获取好友列表
     */
    @GetMapping("/list/vo")
    @Operation(summary = "我的好友列表", description = "获取当前登录用户的所有好友基本信息（昵称、头像）")
    public BaseResponse<List<ChatFriendUserVO>> listFriends(HttpServletRequest servletRequest) {
        Long userId = SecurityUtils.getLoginUserId();
        // 批量查询好友详细信息并封装为 VO
        return ResultUtils.success(userFriendService.listFriends(userId));
    }
}
