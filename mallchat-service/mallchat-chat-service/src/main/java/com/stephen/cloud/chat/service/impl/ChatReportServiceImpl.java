package com.stephen.cloud.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.api.chat.model.dto.ChatReportListRequest;
import com.stephen.cloud.api.chat.model.dto.ChatReportSubmitRequest;
import com.stephen.cloud.api.chat.model.enums.ChatReportTargetTypeEnum;
import com.stephen.cloud.api.user.client.UserFeignClient;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.chat.mapper.ChatReportMapper;
import com.stephen.cloud.chat.model.entity.ChatMessage;
import com.stephen.cloud.chat.model.entity.ChatMoment;
import com.stephen.cloud.chat.model.entity.ChatReport;
import com.stephen.cloud.chat.service.ChatMessageService;
import com.stephen.cloud.chat.service.ChatMomentService;
import com.stephen.cloud.chat.service.ChatReportService;
import com.stephen.cloud.chat.service.ChatRoomMemberService;
import com.stephen.cloud.chat.service.UserFriendService;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 聊天举报服务实现
 *
 * @author StephenQiu30
 */
@Service
public class ChatReportServiceImpl extends ServiceImpl<ChatReportMapper, ChatReport>
        implements ChatReportService {

    private static final int STATUS_PENDING = 0;
    private static final int MOMENT_STATUS_NORMAL = 0;
    private static final int MOMENT_VISIBILITY_PUBLIC = 1;
    private static final int MOMENT_AUDIT_STATUS_PASS = 1;
    private static final int MAX_REASON_TYPE_LENGTH = 64;
    private static final int MAX_REASON_LENGTH = 500;

    @Resource
    private UserFeignClient userFeignClient;

    @Resource
    private ChatMessageService chatMessageService;

    @Resource
    private ChatMomentService chatMomentService;

    @Resource
    private ChatRoomMemberService chatRoomMemberService;

    @Resource
    private UserFriendService userFriendService;

    @Override
    public Long submitReport(Long reporterUserId, ChatReportSubmitRequest request) {
        ThrowUtils.throwIf(reporterUserId == null || request == null, ErrorCode.PARAMS_ERROR);
        ChatReportTargetTypeEnum targetType = ChatReportTargetTypeEnum.getEnumByCode(request.getTargetType());
        ThrowUtils.throwIf(targetType == null || request.getTargetId() == null, ErrorCode.PARAMS_ERROR);
        String reasonType = normalizeReasonType(request.getReasonType());
        String reason = normalizeReason(request.getReason());

        ChatReport existing = getExistingReport(reporterUserId, targetType.getCode(), request.getTargetId());
        if (existing != null) {
            return existing.getId();
        }

        Long targetOwnerId = resolveTargetOwnerId(reporterUserId, targetType, request.getTargetId());
        ChatReport report = new ChatReport();
        report.setReporterUserId(reporterUserId);
        report.setTargetType(targetType.getCode());
        report.setTargetId(request.getTargetId());
        report.setTargetOwnerId(targetOwnerId);
        report.setReasonType(reasonType);
        report.setReason(reason);
        report.setStatus(STATUS_PENDING);
        try {
            ThrowUtils.throwIf(!this.save(report), ErrorCode.OPERATION_ERROR, "提交举报失败");
            return report.getId();
        } catch (DuplicateKeyException e) {
            ChatReport duplicate = getExistingReport(reporterUserId, targetType.getCode(), request.getTargetId());
            ThrowUtils.throwIf(duplicate == null, ErrorCode.OPERATION_ERROR, "提交举报失败");
            return duplicate.getId();
        }
    }

    @Override
    public Page<ChatReport> listReports(ChatReportListRequest request) {
        return this.page(new Page<>(request.getCurrent(), request.getPageSize()));
    }

    private Long resolveTargetOwnerId(Long reporterUserId, ChatReportTargetTypeEnum targetType, Long targetId) {
        if (ChatReportTargetTypeEnum.USER.equals(targetType)) {
            ThrowUtils.throwIf(Objects.equals(reporterUserId, targetId), ErrorCode.PARAMS_ERROR, "不能举报自己");
            ThrowUtils.throwIf(getUserById(targetId) == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
            return targetId;
        }
        if (ChatReportTargetTypeEnum.MESSAGE.equals(targetType)) {
            ChatMessage message = getMessageById(targetId);
            ThrowUtils.throwIf(message == null, ErrorCode.NOT_FOUND_ERROR, "消息不存在");
            ThrowUtils.throwIf(!chatRoomMemberService.isMember(message.getRoomId(), reporterUserId),
                    ErrorCode.NO_AUTH_ERROR, "无权举报该消息");
            return message.getFromUserId();
        }
        ChatMoment moment = getMomentById(targetId);
        ThrowUtils.throwIf(moment == null || !Objects.equals(moment.getStatus(), MOMENT_STATUS_NORMAL)
                || Objects.equals(moment.getIsDelete(), 1), ErrorCode.NOT_FOUND_ERROR, "动态不存在");
        if (!Objects.equals(moment.getUserId(), reporterUserId)) {
            ThrowUtils.throwIf(userFriendService == null, ErrorCode.NO_AUTH_ERROR, "无权举报该动态");
            ThrowUtils.throwIf(userFriendService.isBlockedBetween(reporterUserId, moment.getUserId()),
                    ErrorCode.NO_AUTH_ERROR, "无权举报该动态");
            boolean publicVisible = Objects.equals(moment.getVisibility(), MOMENT_VISIBILITY_PUBLIC)
                    && Objects.equals(moment.getAuditStatus(), MOMENT_AUDIT_STATUS_PASS);
            ThrowUtils.throwIf(!publicVisible
                            && !userFriendService.listMutualFriendIds(reporterUserId).contains(moment.getUserId()),
                    ErrorCode.NO_AUTH_ERROR, "无权举报该动态");
        }
        return moment.getUserId();
    }

    private ChatReport getExistingReport(Long reporterUserId, Integer targetType, Long targetId) {
        return this.getOne(new LambdaQueryWrapper<ChatReport>()
                .eq(ChatReport::getReporterUserId, reporterUserId)
                .eq(ChatReport::getTargetType, targetType)
                .eq(ChatReport::getTargetId, targetId)
                .last("LIMIT 1"));
    }

    private String normalizeReasonType(String reasonType) {
        String normalized = StringUtils.trimToNull(reasonType);
        ThrowUtils.throwIf(normalized == null, ErrorCode.PARAMS_ERROR, "举报原因类型不能为空");
        ThrowUtils.throwIf(normalized.length() > MAX_REASON_TYPE_LENGTH, ErrorCode.PARAMS_ERROR, "举报原因类型过长");
        return normalized;
    }

    private String normalizeReason(String reason) {
        String normalized = StringUtils.trimToNull(reason);
        ThrowUtils.throwIf(normalized != null && normalized.length() > MAX_REASON_LENGTH,
                ErrorCode.PARAMS_ERROR, "举报说明过长");
        return normalized;
    }

    protected UserVO getUserById(Long userId) {
        return userFeignClient.getUserVOById(userId).getData();
    }

    protected ChatMessage getMessageById(Long messageId) {
        return chatMessageService.getById(messageId);
    }

    protected ChatMoment getMomentById(Long momentId) {
        return chatMomentService.getById(momentId);
    }
}
