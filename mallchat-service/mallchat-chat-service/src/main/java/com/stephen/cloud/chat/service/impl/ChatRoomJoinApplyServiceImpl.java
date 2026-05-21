package com.stephen.cloud.chat.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.api.chat.model.dto.ChatRoomJoinApplyRequest;
import com.stephen.cloud.api.chat.model.dto.ChatRoomJoinApproveRequest;
import com.stephen.cloud.api.chat.model.enums.ChatRoomRoleEnum;
import com.stephen.cloud.api.chat.model.enums.ChatRoomTypeEnum;
import com.stephen.cloud.api.chat.model.vo.ChatRoomJoinApplyVO;
import com.stephen.cloud.api.notification.client.NotificationFeignClient;
import com.stephen.cloud.api.notification.model.dto.NotificationCreateRequest;
import com.stephen.cloud.api.notification.model.enums.NotificationTypeEnum;
import com.stephen.cloud.chat.convert.ChatRoomJoinApplyConvert;
import com.stephen.cloud.chat.mapper.ChatRoomJoinApplyMapper;
import com.stephen.cloud.chat.model.entity.ChatRoom;
import com.stephen.cloud.chat.model.entity.ChatRoomJoinApply;
import com.stephen.cloud.chat.model.entity.ChatRoomMember;
import com.stephen.cloud.chat.service.ChatRoomJoinApplyService;
import com.stephen.cloud.chat.service.ChatRoomMemberService;
import com.stephen.cloud.chat.service.ChatRoomService;
import com.stephen.cloud.chat.service.ChatSessionService;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Objects;

/**
 * 入群申请服务实现
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class ChatRoomJoinApplyServiceImpl extends ServiceImpl<ChatRoomJoinApplyMapper, ChatRoomJoinApply>
        implements ChatRoomJoinApplyService {

    private static final int STATUS_PENDING = 1;
    private static final int STATUS_APPROVED = 2;
    private static final int STATUS_REJECTED = 3;
    private static final String RELATED_TYPE_JOIN_APPLY = "chat_room_join_apply";

    @Resource
    private ChatRoomService chatRoomService;

    @Resource
    private ChatRoomMemberService chatRoomMemberService;

    @Resource
    private ChatSessionService chatSessionService;

    @Resource
    private NotificationFeignClient notificationFeignClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long applyJoinRoom(ChatRoomJoinApplyRequest request, Long userId) {
        ThrowUtils.throwIf(request == null || request.getRoomId() == null || userId == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(StringUtils.length(request.getMsg()) > 256, ErrorCode.PARAMS_ERROR, "申请留言过长");
        ChatRoom room = requireGroupRoom(request.getRoomId());
        ThrowUtils.throwIf(chatRoomMemberService.isMember(room.getId(), userId), ErrorCode.OPERATION_ERROR, "已经在群聊中");

        ChatRoomJoinApply existing = getPendingApply(room.getId(), userId);
        if (existing != null) {
            return existing.getId();
        }

        ChatRoomJoinApply apply = new ChatRoomJoinApply();
        apply.setRoomId(room.getId());
        apply.setUserId(userId);
        apply.setMsg(StringUtils.defaultString(request.getMsg()));
        apply.setStatus(STATUS_PENDING);
        apply.setActiveKey(buildActiveKey(room.getId(), userId));
        try {
            boolean saved = this.save(apply);
            ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "创建入群申请失败");
        } catch (DuplicateKeyException e) {
            ChatRoomJoinApply duplicated = getPendingApply(room.getId(), userId);
            ThrowUtils.throwIf(duplicated == null, ErrorCode.OPERATION_ERROR, "创建入群申请失败");
            return duplicated.getId();
        }
        scheduleManagerNotification(room, apply);
        return apply.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approveJoinRoom(ChatRoomJoinApproveRequest request, Long reviewerId) {
        ThrowUtils.throwIf(request == null || request.getApplyId() == null || reviewerId == null, ErrorCode.PARAMS_ERROR);
        Integer status = request.getStatus();
        ThrowUtils.throwIf(!Objects.equals(status, STATUS_APPROVED) && !Objects.equals(status, STATUS_REJECTED),
                ErrorCode.PARAMS_ERROR, "审核状态非法");
        ThrowUtils.throwIf(StringUtils.length(request.getReviewMsg()) > 256, ErrorCode.PARAMS_ERROR, "审核留言过长");

        ChatRoomJoinApply apply = this.getById(request.getApplyId());
        ThrowUtils.throwIf(apply == null, ErrorCode.NOT_FOUND_ERROR, "入群申请不存在");
        ThrowUtils.throwIf(!Objects.equals(apply.getStatus(), STATUS_PENDING), ErrorCode.PARAMS_ERROR, "申请已处理");
        requireManager(apply.getRoomId(), reviewerId);

        boolean updated = this.update(new LambdaUpdateWrapper<ChatRoomJoinApply>()
                .eq(ChatRoomJoinApply::getId, apply.getId())
                .eq(ChatRoomJoinApply::getStatus, STATUS_PENDING)
                .set(ChatRoomJoinApply::getStatus, status)
                .set(ChatRoomJoinApply::getReviewerId, reviewerId)
                .set(ChatRoomJoinApply::getReviewMsg, request.getReviewMsg())
                .set(ChatRoomJoinApply::getActiveKey, null));
        ThrowUtils.throwIf(!updated, ErrorCode.PARAMS_ERROR, "申请已处理");

        apply.setStatus(status);
        apply.setReviewerId(reviewerId);
        apply.setReviewMsg(request.getReviewMsg());
        apply.setActiveKey(null);

        if (Objects.equals(status, STATUS_APPROVED)) {
            chatRoomMemberService.addMember(apply.getRoomId(), apply.getUserId(), ChatRoomRoleEnum.MEMBER.getCode());
            chatSessionService.updateSession(apply.getUserId(), apply.getRoomId(), null, false);
        }
        scheduleApplicantNotification(apply);
        return true;
    }

    @Override
    public Page<ChatRoomJoinApplyVO> listRoomJoinApplyPage(Long roomId, long current, long size, Long reviewerId) {
        ThrowUtils.throwIf(roomId == null || current <= 0 || size <= 0 || size > 50 || reviewerId == null,
                ErrorCode.PARAMS_ERROR);
        requireManager(roomId, reviewerId);
        Page<ChatRoomJoinApply> page = this.page(new Page<>(current, size),
                new LambdaQueryWrapper<ChatRoomJoinApply>()
                        .eq(ChatRoomJoinApply::getRoomId, roomId)
                        .orderByDesc(ChatRoomJoinApply::getCreateTime)
                        .orderByDesc(ChatRoomJoinApply::getId));
        return ChatRoomJoinApplyConvert.getVOPage(page);
    }

    private ChatRoom requireGroupRoom(Long roomId) {
        ChatRoom room = chatRoomService.getById(roomId);
        ThrowUtils.throwIf(room == null, ErrorCode.NOT_FOUND_ERROR, "聊天室不存在");
        ThrowUtils.throwIf(!ChatRoomTypeEnum.GROUP.getCode().equals(room.getType()),
                ErrorCode.PARAMS_ERROR, "仅群聊支持入群申请");
        return room;
    }

    private ChatRoomMember requireManager(Long roomId, Long userId) {
        ChatRoomMember member = chatRoomMemberService.getMember(roomId, userId);
        ThrowUtils.throwIf(member == null, ErrorCode.NO_AUTH_ERROR, "您不在此群聊中");
        boolean manager = ChatRoomRoleEnum.OWNER.getCode().equals(member.getRole())
                || ChatRoomRoleEnum.ADMIN.getCode().equals(member.getRole());
        ThrowUtils.throwIf(!manager, ErrorCode.NO_AUTH_ERROR, "仅群主或管理员可审核入群申请");
        return member;
    }

    private ChatRoomJoinApply getPendingApply(Long roomId, Long userId) {
        return this.getOne(new LambdaQueryWrapper<ChatRoomJoinApply>()
                .eq(ChatRoomJoinApply::getRoomId, roomId)
                .eq(ChatRoomJoinApply::getUserId, userId)
                .eq(ChatRoomJoinApply::getStatus, STATUS_PENDING)
                .last("LIMIT 1"));
    }

    private String buildActiveKey(Long roomId, Long userId) {
        return roomId + ":" + userId;
    }

    private void scheduleManagerNotification(ChatRoom room, ChatRoomJoinApply apply) {
        List<ChatRoomMember> members = chatRoomMemberService.listByRoomId(room.getId());
        if (CollUtil.isEmpty(members)) {
            return;
        }
        List<Long> managerIds = members.stream()
                .filter(member -> ChatRoomRoleEnum.OWNER.getCode().equals(member.getRole())
                        || ChatRoomRoleEnum.ADMIN.getCode().equals(member.getRole()))
                .map(ChatRoomMember::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        managerIds.forEach(managerId -> scheduleNotification(() -> sendNotification(managerId, "入群申请",
                "收到新的入群申请：" + StringUtils.defaultIfBlank(room.getName(), "群聊"),
                "room_join_apply:" + apply.getId() + ":" + managerId, apply.getId())));
    }

    private void scheduleApplicantNotification(ChatRoomJoinApply apply) {
        String title = Objects.equals(apply.getStatus(), STATUS_APPROVED) ? "入群申请已通过" : "入群申请已拒绝";
        String bizPrefix = Objects.equals(apply.getStatus(), STATUS_APPROVED) ? "room_join_approve:" : "room_join_reject:";
        scheduleNotification(() -> sendNotification(apply.getUserId(), title, title,
                bizPrefix + apply.getId(), apply.getId()));
    }

    private void scheduleNotification(Runnable runnable) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runNotification(runnable);
                }
            });
            return;
        }
        runNotification(runnable);
    }

    private void runNotification(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.warn("[ChatRoomJoinApplyServiceImpl] 发送入群申请通知失败, reason={}", e.getMessage());
        }
    }

    private void sendNotification(Long userId, String title, String content, String bizId, Long applyId) {
        NotificationCreateRequest notification = new NotificationCreateRequest();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(NotificationTypeEnum.USER.getCode());
        notification.setRelatedType(RELATED_TYPE_JOIN_APPLY);
        notification.setRelatedId(applyId);
        notification.setBizId(bizId);
        notification.setContentUrl("/chat/room/join/apply?id=" + applyId);
        BaseResponse<Long> response = notificationFeignClient.addBusinessNotification(notification);
        ThrowUtils.throwIf(response == null || response.getData() == null,
                ErrorCode.OPERATION_ERROR, "创建入群申请通知失败");
    }
}
