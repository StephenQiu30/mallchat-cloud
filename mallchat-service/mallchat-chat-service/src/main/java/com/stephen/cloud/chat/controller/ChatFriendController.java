package com.stephen.cloud.chat.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.chat.model.dto.ChatFriendAddRequest;
import com.stephen.cloud.api.chat.model.vo.ChatFriendUserVO;
import com.stephen.cloud.chat.service.UserFriendService;
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

    /**
     * 添加好友（双向关系，幂等）
     *
     * @param request        好友用户 ID
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
        throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "MVP阶段请通过好友申请与审批流程建立好友关系");
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

    /**
     * 搜索好友候选用户（带关系状态）
     *
     * @param searchText 搜索文本
     * @param current    页码
     * @param pageSize   页大小
     * @param servletRequest 请求
     * @return 好友候选列表
     */
    @GetMapping("/search")
    @Operation(summary = "搜索候选用户", description = "按昵称/简介搜索用户并返回与当前用户关系状态")
    public BaseResponse<Page<ChatFriendUserVO>> search(
            @RequestParam(value = "searchText", required = false) String searchText,
            @RequestParam(value = "current", defaultValue = "1") int current,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            HttpServletRequest servletRequest) {
        Long userId = SecurityUtils.getLoginUserId();
        return ResultUtils.success(userFriendService.searchFriends(userId, searchText, current, pageSize));
    }

    /**
     * 删除好友
     *
     * @param friendUserId 好友用户 ID
     * @param servletRequest 请求
     * @return 是否成功
     */
    @DeleteMapping("/delete")
    @OperationLog(module = "好友管理", action = "删除好友")
    @Operation(summary = "删除好友", description = "移除好友关系（双向）")
    public BaseResponse<Boolean> deleteFriend(@RequestParam Long friendUserId, HttpServletRequest servletRequest) {
        ThrowUtils.throwIf(friendUserId == null, ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        userFriendService.removeFriend(userId, friendUserId);
        return ResultUtils.success(true);
    }
}
