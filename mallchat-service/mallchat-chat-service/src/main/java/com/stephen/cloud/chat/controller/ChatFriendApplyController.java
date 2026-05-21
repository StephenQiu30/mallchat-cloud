package com.stephen.cloud.chat.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.chat.model.dto.ChatFriendApplyQueryRequest;
import com.stephen.cloud.api.chat.model.dto.ChatFriendApplyRequest;
import com.stephen.cloud.api.chat.model.dto.ChatFriendApproveRequest;
import com.stephen.cloud.api.chat.model.vo.ChatFriendApplyVO;
import com.stephen.cloud.api.chat.model.vo.ChatIdVO;
import com.stephen.cloud.api.chat.model.vo.ChatOperationResultVO;
import com.stephen.cloud.chat.convert.ChatFriendApplyConvert;
import com.stephen.cloud.chat.model.entity.UserFriendApply;
import com.stephen.cloud.chat.service.UserFriendApplyService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 聊天好友申请接口
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/chat/friend/apply")
@Slf4j
@Tag(name = "ChatFriendApplyController", description = "聊天好友申请管理")
public class ChatFriendApplyController {

    @Resource
    private UserFriendApplyService userFriendApplyService;

    /**
     * 申请添加好友
     *
     * @param request 申请请求
     * @return 申请 ID
     */
    @PostMapping("/add")
    @OperationLog(module = "好友申请管理", action = "申请好友")
    @Operation(summary = "申请好友", description = "向目标用户发起好友添加申请")
    public BaseResponse<ChatIdVO> applyFriend(@Validated @RequestBody ChatFriendApplyRequest request) {
        ThrowUtils.throwIf(request == null || request.getTargetId() == null, ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        UserFriendApply userFriendApply = ChatFriendApplyConvert.addRequestToObj(request);
        Long applyId = userFriendApplyService.applyFriend(userFriendApply, userId);
        return ResultUtils.success(ChatIdVO.of(applyId));
    }

    /**
     * 审核好友申请
     *
     * @param request 审核请求
     * @return 操作结果
     */
    @PostMapping("/approve")
    @OperationLog(module = "好友申请管理", action = "审核好友")
    @Operation(summary = "审核好友", description = "同意或拒绝好友申请，同意后将自动建立双向好友关系并创建私聊房间")
    public BaseResponse<ChatOperationResultVO> approveFriend(@Validated @RequestBody ChatFriendApproveRequest request) {
        ThrowUtils.throwIf(request == null || request.getApplyId() == null || request.getStatus() == null, ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        boolean result = userFriendApplyService.approveFriend(request, userId);
        return ResultUtils.success(ChatOperationResultVO.of(result));
    }

    /**
     * 获取好友申请列表 (分页)
     *
     * @param queryRequest 分页查询请求
     * @return 申请列表
     */
    @PostMapping("/list/page/vo")
    @Operation(summary = "好友申请列表", description = "获取当前收到的好友申请记录")
    public BaseResponse<Page<ChatFriendApplyVO>> listFriendApply(
            @RequestBody ChatFriendApplyQueryRequest queryRequest) {
        Long userId = SecurityUtils.getLoginUserId();
        Page<ChatFriendApplyVO> list = userFriendApplyService.listFriendApplyPage(queryRequest.getCurrent(), queryRequest.getPageSize(), userId);
        return ResultUtils.success(list);
    }
}
