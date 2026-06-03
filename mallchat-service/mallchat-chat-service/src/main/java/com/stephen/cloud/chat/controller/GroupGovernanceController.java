package com.stephen.cloud.chat.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.chat.model.dto.ChatRoomGovernanceRequest;
import com.stephen.cloud.api.chat.model.enums.ChatRoomRoleEnum;
import com.stephen.cloud.api.chat.model.enums.ChatRoomTypeEnum;
import com.stephen.cloud.api.chat.model.vo.CacheConsistencyVO;
import com.stephen.cloud.api.chat.model.vo.GovernanceMetricsVO;
import com.stephen.cloud.api.chat.model.vo.MemberAuditVO;
import com.stephen.cloud.chat.model.entity.ChatRoom;
import com.stephen.cloud.chat.model.entity.ChatRoomMember;
import com.stephen.cloud.chat.model.entity.ChatRoomMemberAudit;
import com.stephen.cloud.chat.service.ChatRoomMemberAuditService;
import com.stephen.cloud.chat.service.ChatRoomMemberService;
import com.stephen.cloud.chat.service.ChatRoomService;
import com.stephen.cloud.chat.service.impl.GroupGovernanceServiceImpl;
import com.stephen.cloud.chat.service.impl.GroupGovernanceServiceImpl.CacheConsistencyResult;
import com.stephen.cloud.chat.service.impl.GroupGovernanceServiceImpl.GovernanceMetrics;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 群治理接口
 * 提供缓存一致性检查、治理指标和审计日志查询
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/chat/room/governance")
@Slf4j
@Tag(name = "GroupGovernanceController", description = "群治理管理")
public class GroupGovernanceController {

    @Resource
    private GroupGovernanceServiceImpl groupGovernanceService;

    @Resource
    private ChatRoomService chatRoomService;

    @Resource
    private ChatRoomMemberService chatRoomMemberService;

    @Resource
    private ChatRoomMemberAuditService chatRoomMemberAuditService;

    /**
     * 检查群成员缓存一致性（仅群主）
     */
    @GetMapping("/cache-consistency")
    @OperationLog(module = "群治理", action = "缓存一致性检查")
    @Operation(summary = "缓存一致性检查", description = "检查 Redis 缓存与数据库成员列表是否一致，仅群主可操作")
    public BaseResponse<CacheConsistencyVO> checkCacheConsistency(@Validated ChatRoomGovernanceRequest request) {
        ThrowUtils.throwIf(request == null || request.getRoomId() == null, ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        ensureOwner(request.getRoomId(), userId);

        CacheConsistencyResult result = groupGovernanceService.checkCacheConsistency(request.getRoomId());
        CacheConsistencyVO vo = new CacheConsistencyVO();
        vo.setDrifted(result.isDrifted());
        vo.setMissingInCache(result.getMissingInCache());
        vo.setMissingInDb(result.getMissingInDb());
        return ResultUtils.success(vo);
    }

    /**
     * 获取群治理指标（仅群主）
     */
    @GetMapping("/metrics")
    @OperationLog(module = "群治理", action = "获取治理指标")
    @Operation(summary = "获取群治理指标", description = "获取群成员统计和规模信息，仅群主可操作")
    public BaseResponse<GovernanceMetricsVO> getGovernanceMetrics(@Validated ChatRoomGovernanceRequest request) {
        ThrowUtils.throwIf(request == null || request.getRoomId() == null, ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        ensureOwner(request.getRoomId(), userId);

        GovernanceMetrics metrics = groupGovernanceService.getGovernanceMetrics(request.getRoomId());
        GovernanceMetricsVO vo = new GovernanceMetricsVO(
                metrics.getRoomId(), metrics.getTotalMembers(), metrics.getOwnerCount(),
                metrics.getAdminCount(), metrics.getMemberCount(), metrics.getMaxMembers(),
                metrics.getCurrentMemberCount());
        return ResultUtils.success(vo);
    }

    /**
     * 查询成员变更审计日志（群主或管理员）
     */
    @GetMapping("/audit-log")
    @Operation(summary = "查询成员变更审计日志", description = "分页查询成员变更审计记录，群主和管理员可操作")
    public BaseResponse<Page<MemberAuditVO>> listAuditLog(@Validated ChatRoomGovernanceRequest request) {
        ThrowUtils.throwIf(request == null || request.getRoomId() == null, ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        ensureOwnerOrAdmin(request.getRoomId(), userId);

        int pageNum = request.getPageNum() == null || request.getPageNum() <= 0 ? 1 : request.getPageNum();
        int pageSize = request.getPageSize() == null || request.getPageSize() <= 0 ? 20 : Math.min(request.getPageSize(), 100);

        Page<ChatRoomMemberAudit> page = new Page<>(pageNum, pageSize);
        Page<ChatRoomMemberAudit> result = chatRoomMemberAuditService.page(page,
                new LambdaQueryWrapper<ChatRoomMemberAudit>()
                        .eq(ChatRoomMemberAudit::getRoomId, request.getRoomId())
                        .orderByDesc(ChatRoomMemberAudit::getCreateTime));

        Page<MemberAuditVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(audit -> {
            MemberAuditVO vo = new MemberAuditVO();
            vo.setId(audit.getId());
            vo.setRoomId(audit.getRoomId());
            vo.setUserId(audit.getUserId());
            vo.setAction(audit.getAction());
            vo.setOperatorId(audit.getOperatorId());
            vo.setCreateTime(audit.getCreateTime());
            return vo;
        }).collect(Collectors.toList()));
        return ResultUtils.success(voPage);
    }

    /**
     * 校验当前用户是否为群主
     */
    private void ensureOwner(Long roomId, Long userId) {
        ChatRoom room = chatRoomService.getById(roomId);
        ThrowUtils.throwIf(room == null, ErrorCode.NOT_FOUND_ERROR, "聊天室不存在");
        ThrowUtils.throwIf(!ChatRoomTypeEnum.GROUP.getCode().equals(room.getType()),
                ErrorCode.PARAMS_ERROR, "仅群聊支持治理操作");
        ThrowUtils.throwIf(!chatRoomMemberService.isOwner(roomId, userId),
                ErrorCode.NO_AUTH_ERROR, "仅群主可执行此操作");
    }

    /**
     * 校验当前用户是否为群主或管理员
     */
    private void ensureOwnerOrAdmin(Long roomId, Long userId) {
        ChatRoom room = chatRoomService.getById(roomId);
        ThrowUtils.throwIf(room == null, ErrorCode.NOT_FOUND_ERROR, "聊天室不存在");
        ThrowUtils.throwIf(!ChatRoomTypeEnum.GROUP.getCode().equals(room.getType()),
                ErrorCode.PARAMS_ERROR, "仅群聊支持治理操作");

        ChatRoomMember member = chatRoomMemberService.getMember(roomId, userId);
        ThrowUtils.throwIf(member == null, ErrorCode.NO_AUTH_ERROR, "您不在此群聊中");
        boolean hasPermission = ChatRoomRoleEnum.OWNER.getCode().equals(member.getRole())
                || ChatRoomRoleEnum.ADMIN.getCode().equals(member.getRole());
        ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR, "仅群主或管理员可查看审计日志");
    }
}
