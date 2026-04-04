package com.stephen.cloud.chat.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.chat.model.dto.ChatFriendApplyRequest;
import com.stephen.cloud.api.chat.model.dto.ChatFriendApproveRequest;
import com.stephen.cloud.api.chat.model.vo.ChatFriendApplyVO;
import com.stephen.cloud.chat.convert.ChatFriendApplyConvert;
import com.stephen.cloud.chat.model.entity.UserFriendApply;
import com.stephen.cloud.chat.service.ChatRoomService;
import com.stephen.cloud.chat.service.UserFriendApplyService;
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

/**
 * 聊天好友申请接口
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/chat_friend_apply")
@Slf4j
@Tag(name = "ChatFriendApplyController", description = "聊天好友申请管理")
public class ChatFriendApplyController {

    @Resource
    private UserFriendApplyService userFriendApplyService;

    @Resource
    private ChatRoomService chatRoomService;

    /**
     * 申请添加好友
     *
     * @param request 申请请求
     * @param servletRequest HTTP 请求
     * @return 申请ID
     */
    @PostMapping("/add")
    @OperationLog(module = "好友申请管理", action = "申请好友")
    @Operation(summary = "申请好友", description = "发起好友添加申请")
    public BaseResponse<Long> applyFriend(@Validated @RequestBody ChatFriendApplyRequest request,
                                           HttpServletRequest servletRequest) {
        ThrowUtils.throwIf(request == null || request.getTargetId() == null, ErrorCode.PARAMS_ERROR);
        Long userId = chatRoomService.getLoginUserId(servletRequest);
        UserFriendApply userFriendApply = ChatFriendApplyConvert.addRequestToObj(request);
        Long applyId = userFriendApplyService.applyFriend(userFriendApply, userId);
        return ResultUtils.success(applyId);
    }

    /**
     * 审核好友申请
     *
     * @param request 审核请求
     * @param servletRequest HTTP 请求
     * @return 是否成功
     */
    @PostMapping("/approve")
    @OperationLog(module = "好友申请管理", action = "审核好友")
    @Operation(summary = "审核好友", description = "同意或拒绝好友申请")
    public BaseResponse<Boolean> approveFriend(@Validated @RequestBody ChatFriendApproveRequest request,
                                               HttpServletRequest servletRequest) {
        ThrowUtils.throwIf(request == null || request.getApplyId() == null || request.getStatus() == null, ErrorCode.PARAMS_ERROR);
        Long userId = chatRoomService.getLoginUserId(servletRequest);
        boolean result = userFriendApplyService.approveFriend(request, userId);
        return ResultUtils.success(result);
    }

    /**
     * 获取好友申请列表 (分页)
     *
     * @param current 页码
     * @param size    每页大小
     * @param servletRequest HTTP 请求
     * @return 申请列表
     */
    @GetMapping("/list/vo")
    @Operation(summary = "好友申请列表", description = "获取当前收到的好友申请记录")
    public BaseResponse<Page<ChatFriendApplyVO>> listFriendApply(@RequestParam(defaultValue = "1") long current,
                                                                   @RequestParam(defaultValue = "10") long size,
                                                                   HttpServletRequest servletRequest) {
        Long userId = chatRoomService.getLoginUserId(servletRequest);
        Page<ChatFriendApplyVO> list = userFriendApplyService.listFriendApplyPage(current, size, userId);
        return ResultUtils.success(list);
    }
}
