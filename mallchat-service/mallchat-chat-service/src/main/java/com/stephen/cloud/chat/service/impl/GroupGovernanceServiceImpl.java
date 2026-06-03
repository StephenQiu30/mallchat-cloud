package com.stephen.cloud.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stephen.cloud.api.chat.model.enums.ChatRoomRoleEnum;
import com.stephen.cloud.chat.model.entity.ChatRoom;
import com.stephen.cloud.chat.model.entity.ChatRoomMember;
import com.stephen.cloud.chat.model.entity.ChatRoomMemberAudit;
import com.stephen.cloud.chat.service.ChatRoomMemberAuditService;
import com.stephen.cloud.chat.service.ChatRoomMemberService;
import com.stephen.cloud.chat.service.ChatRoomService;
import com.stephen.cloud.common.cache.constants.ChatCacheConstant;
import com.stephen.cloud.common.cache.utils.CacheUtils;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.exception.BusinessException;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 群治理服务
 * 提供成员变更审计、缓存一致性检查、群规模限制和治理指标
 */
@Service
@Slf4j
public class GroupGovernanceServiceImpl {

    @Resource
    private CacheUtils cacheUtils;

    @Resource
    private ChatRoomService chatRoomService;

    @Resource
    private ChatRoomMemberService chatRoomMemberService;

    @Resource
    private ChatRoomMemberAuditService chatRoomMemberAuditService;

    /**
     * 记录成员变更审计
     *
     * @param roomId     房间 ID
     * @param userId     被操作用户 ID
     * @param action     操作类型（JOIN/LEAVE/KICK/GRANT_ADMIN/REVOKE_ADMIN）
     * @param operatorId 操作人 ID
     */
    public void recordAudit(Long roomId, Long userId, String action, Long operatorId) {
        ThrowUtils.throwIf(roomId == null, ErrorCode.PARAMS_ERROR, "房间 ID 不能为空");
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR, "用户 ID 不能为空");
        ThrowUtils.throwIf(StringUtils.isBlank(action), ErrorCode.PARAMS_ERROR, "操作类型不能为空");
        saveAuditRecord(roomId, userId, action, operatorId);
    }

    /**
     * 持久化审计记录（可测试子类重写此方法）
     */
    protected void saveAuditRecord(Long roomId, Long userId, String action, Long operatorId) {
        ChatRoomMemberAudit audit = new ChatRoomMemberAudit();
        audit.setRoomId(roomId);
        audit.setUserId(userId);
        audit.setAction(action);
        audit.setOperatorId(operatorId);
        chatRoomMemberAuditService.save(audit);
        log.info("[GroupGovernance] 审计记录: roomId={}, userId={}, action={}, operatorId={}",
                roomId, userId, action, operatorId);
    }

    /**
     * 检查缓存一致性
     *
     * @param roomId 房间 ID
     * @return 一致性检查结果
     */
    public CacheConsistencyResult checkCacheConsistency(Long roomId) {
        ThrowUtils.throwIf(roomId == null, ErrorCode.PARAMS_ERROR, "房间 ID 不能为空");

        // 获取 DB 中的成员列表
        List<ChatRoomMember> dbMemberList = chatRoomMemberService.listByRoomId(roomId);
        Set<Long> dbUserIds = dbMemberList.stream()
                .map(ChatRoomMember::getUserId)
                .collect(Collectors.toSet());

        // 获取缓存中的成员列表
        String cacheKey = ChatCacheConstant.getRoomMemberKey(roomId);
        Set<String> cacheUserIdStrs = cacheUtils.sMembers(cacheKey);
        Set<Long> cacheUserIds = cacheUserIdStrs.stream()
                .map(Long::parseLong)
                .collect(Collectors.toSet());

        // 计算漂移
        Set<Long> missingInCache = new HashSet<>(dbUserIds);
        missingInCache.removeAll(cacheUserIds);

        Set<Long> missingInDb = new HashSet<>(cacheUserIds);
        missingInDb.removeAll(dbUserIds);

        return new CacheConsistencyResult(missingInCache, missingInDb);
    }

    /**
     * 执行群规模限制检查
     *
     * @param roomId 房间 ID
     */
    public void enforceMaxMembers(Long roomId) {
        ThrowUtils.throwIf(roomId == null, ErrorCode.PARAMS_ERROR, "房间 ID 不能为空");

        ChatRoom room = chatRoomService.getById(roomId);
        if (room == null || room.getMaxMembers() == null || room.getMaxMembers() <= 0) {
            return;
        }

        long currentCount = chatRoomMemberService.countByRoomId(roomId);
        ThrowUtils.throwIf(currentCount >= room.getMaxMembers(),
                ErrorCode.OPERATION_ERROR, "群成员已达上限，无法继续添加");
    }

    /**
     * 获取群治理指标
     *
     * @param roomId 房间 ID
     * @return 治理指标
     */
    public GovernanceMetrics getGovernanceMetrics(Long roomId) {
        ThrowUtils.throwIf(roomId == null, ErrorCode.PARAMS_ERROR, "房间 ID 不能为空");

        ChatRoom room = chatRoomService.getById(roomId);
        ThrowUtils.throwIf(room == null, ErrorCode.NOT_FOUND_ERROR, "聊天室不存在");

        List<ChatRoomMember> members = chatRoomMemberService.listByRoomId(roomId);
        int totalMembers = members.size();
        long ownerCount = members.stream()
                .filter(m -> ChatRoomRoleEnum.OWNER.getCode().equals(m.getRole()))
                .count();
        long adminCount = members.stream()
                .filter(m -> ChatRoomRoleEnum.ADMIN.getCode().equals(m.getRole()))
                .count();
        long memberCount = members.stream()
                .filter(m -> ChatRoomRoleEnum.MEMBER.getCode().equals(m.getRole()))
                .count();

        Integer maxMembers = room.getMaxMembers();
        long currentMemberCount = chatRoomMemberService.countByRoomId(roomId);

        return new GovernanceMetrics(roomId, totalMembers, (int) ownerCount, (int) adminCount,
                (int) memberCount, maxMembers, (int) currentMemberCount);
    }

    /**
     * 缓存一致性检查结果
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CacheConsistencyResult {
        /** DB 中存在但缓存中缺失的用户 ID */
        private Set<Long> missingInCache;
        /** 缓存中存在但 DB 中缺失的用户 ID */
        private Set<Long> missingInDb;

        public boolean isDrifted() {
            return (missingInCache != null && !missingInCache.isEmpty())
                    || (missingInDb != null && !missingInDb.isEmpty());
        }
    }

    /**
     * 群治理指标
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GovernanceMetrics {
        private Long roomId;
        private int totalMembers;
        private int ownerCount;
        private int adminCount;
        private int memberCount;
        private Integer maxMembers;
        private int currentMemberCount;
    }
}
