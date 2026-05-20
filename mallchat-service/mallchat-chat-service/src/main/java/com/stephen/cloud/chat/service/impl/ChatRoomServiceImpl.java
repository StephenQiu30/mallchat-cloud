package com.stephen.cloud.chat.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.api.chat.model.enums.ChatRoomRoleEnum;
import com.stephen.cloud.api.chat.model.enums.ChatRoomTypeEnum;
import com.stephen.cloud.api.chat.model.vo.ChatRoomMemberVO;
import com.stephen.cloud.api.chat.model.vo.ChatSessionVO;
import com.stephen.cloud.api.chat.model.vo.ChatRoomVO;
import com.stephen.cloud.api.notification.client.NotificationFeignClient;
import com.stephen.cloud.api.notification.model.dto.NotificationCreateRequest;
import com.stephen.cloud.api.notification.model.enums.NotificationTypeEnum;
import com.stephen.cloud.api.user.client.UserFeignClient;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.chat.convert.ChatRoomConvert;
import com.stephen.cloud.chat.mapper.ChatRoomMapper;
import com.stephen.cloud.chat.mq.producer.ChatMqProducer;
import com.stephen.cloud.chat.model.entity.ChatGroupInfo;
import com.stephen.cloud.chat.model.entity.ChatPrivateRoom;
import com.stephen.cloud.chat.model.entity.ChatRoom;
import com.stephen.cloud.chat.model.entity.ChatRoomMember;
import com.stephen.cloud.chat.service.ChatGroupInfoService;
import com.stephen.cloud.chat.service.ChatPrivateRoomService;
import com.stephen.cloud.chat.service.ChatRoomMemberService;
import com.stephen.cloud.chat.service.ChatRoomService;
import com.stephen.cloud.chat.service.ChatSessionService;
import com.stephen.cloud.chat.service.UserFriendService;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.exception.BusinessException;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 聊天室服务实现
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class ChatRoomServiceImpl extends ServiceImpl<ChatRoomMapper, ChatRoom>
        implements ChatRoomService {

    private static final String RELATED_TYPE_CHAT_ROOM = "chat_room";

    @Resource
    private ChatRoomMemberService chatRoomMemberService;

    @Resource
    private UserFriendService userFriendService;

    @Resource
    private ChatPrivateRoomService chatPrivateRoomService;

    @Resource
    private ChatGroupInfoService chatGroupInfoService;

    @Resource
    private UserFeignClient userFeignClient;

    @Lazy
    @Resource
    private ChatSessionService chatSessionService;

    @Resource
    private ChatMqProducer chatMqProducer;

    @Resource
    private NotificationFeignClient notificationFeignClient;

    @Override
    public void validChatRoom(ChatRoom chatRoom) {
        if (chatRoom == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (StringUtils.isNotBlank(chatRoom.getName())) {
            ThrowUtils.throwIf(chatRoom.getName().length() > 80, ErrorCode.PARAMS_ERROR, "房间名称过长");
        }
    }

    @Override
    public ChatRoomVO getChatRoomVO(ChatRoom chatRoom, HttpServletRequest request) {
        return ChatRoomConvert.objToVo(chatRoom);
    }

    @Override
    public List<ChatRoomVO> getChatRoomVO(List<ChatRoom> chatRoomList, HttpServletRequest request) {
        return ChatRoomConvert.getChatRoomVO(chatRoomList);
    }

    @Override
    public Page<ChatRoomVO> getChatRoomVOPage(Page<ChatRoom> chatRoomPage, HttpServletRequest request) {
        return ChatRoomConvert.getChatRoomVO(chatRoomPage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addChatRoom(ChatRoom chatRoom, List<Long> memberIds, String announcement, Long userId) {
        ThrowUtils.throwIf(chatRoom == null || userId == null, ErrorCode.PARAMS_ERROR);
        validChatRoom(chatRoom);
        chatRoom.setCreateUser(userId);
        chatRoom.setType(ChatRoomTypeEnum.GROUP.getCode());

        boolean result = this.save(chatRoom);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建聊天室失败");

        chatRoomMemberService.addMember(chatRoom.getId(), userId, ChatRoomRoleEnum.OWNER.getCode());
        for (Long memberId : sanitizeInviteMembers(memberIds, userId)) {
            ThrowUtils.throwIf(!userFriendService.isMutualFriend(userId, memberId), ErrorCode.NO_AUTH_ERROR, "仅支持邀请好友入群");
            chatRoomMemberService.addMember(chatRoom.getId(), memberId, ChatRoomRoleEnum.MEMBER.getCode());
            chatSessionService.updateSession(memberId, chatRoom.getId(), null, false);
            pushSessionUpdate(memberId, chatRoom.getId(), "session_join:" + chatRoom.getId() + ":" + memberId);
            trySendGroupInviteNotification(memberId, chatRoom);
        }
        chatSessionService.updateSession(userId, chatRoom.getId(), null, false);
        pushSessionUpdate(userId, chatRoom.getId(), "session_create:" + chatRoom.getId() + ":" + userId);
        chatGroupInfoService.initGroupInfo(chatRoom.getId(), chatRoom.getName(), chatRoom.getAvatar(), announcement, userId);
        return chatRoom.getId();
    }

    @Override
    public List<ChatRoomVO> listUserChatRooms(Long userId) {
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR);
        List<ChatRoomMember> memberships = chatRoomMemberService.list(new LambdaQueryWrapper<ChatRoomMember>()
                .eq(ChatRoomMember::getUserId, userId));
        if (CollUtil.isEmpty(memberships)) {
            return Collections.emptyList();
        }
        List<Long> roomIds = memberships.stream().map(ChatRoomMember::getRoomId).distinct().toList();
        List<ChatRoom> rooms = this.listByIds(roomIds);
        return buildRoomVOList(rooms, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void joinChatRoom(Long roomId, Long userId) {
        ChatRoom chatRoom = this.getById(roomId);
        ThrowUtils.throwIf(chatRoom == null, ErrorCode.NOT_FOUND_ERROR, "聊天室不存在");
        throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "当前版本不支持通过公开入口加入聊天室");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long getOrCreatePrivateRoom(Long peerUserId, Long userId) {
        log.info("[ChatRoomServiceImpl] 获取或创建私聊房间: userId={}, peerUserId={}", userId, peerUserId);
        ThrowUtils.throwIf(peerUserId == null || userId == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(Objects.equals(peerUserId, userId), ErrorCode.PARAMS_ERROR, "不能与自己私聊");
        ThrowUtils.throwIf(!userFriendService.isMutualFriend(userId, peerUserId), ErrorCode.NO_AUTH_ERROR, "非好友无法发起私聊");

        long userLow = Math.min(userId, peerUserId);
        long userHigh = Math.max(userId, peerUserId);
        ChatPrivateRoom existing = chatPrivateRoomService.getOne(new LambdaQueryWrapper<ChatPrivateRoom>()
                .eq(ChatPrivateRoom::getUserLow, userLow)
                .eq(ChatPrivateRoom::getUserHigh, userHigh)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing.getRoomId();
        }

        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setName("私聊");
        chatRoom.setType(ChatRoomTypeEnum.PRIVATE.getCode());
        chatRoom.setCreateUser(userId);
        boolean saved = this.save(chatRoom);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "创建私聊房间失败");

        chatRoomMemberService.addMember(chatRoom.getId(), userId);
        chatRoomMemberService.addMember(chatRoom.getId(), peerUserId);

        ChatPrivateRoom mapping = new ChatPrivateRoom();
        mapping.setUserLow(userLow);
        mapping.setUserHigh(userHigh);
        mapping.setRoomId(chatRoom.getId());
        chatPrivateRoomService.save(mapping);

        chatSessionService.updateSession(userId, chatRoom.getId(), null, false);
        chatSessionService.updateSession(peerUserId, chatRoom.getId(), null, false);
        return chatRoom.getId();
    }

    @Override
    public ChatRoomVO getRoomDetail(Long roomId, Long userId) {
        ChatRoom room = getAccessibleRoom(roomId, userId);
        return buildRoomVOList(Collections.singletonList(room), userId).stream().findFirst().orElse(null);
    }

    @Override
    public List<ChatRoomMemberVO> listRoomMembers(Long roomId, Long userId) {
        getAccessibleRoom(roomId, userId);
        return chatRoomMemberService.getChatRoomMemberVO(chatRoomMemberService.listByRoomId(roomId), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void inviteMembers(Long roomId, List<Long> memberIds, Long userId) {
        ChatRoom room = getAccessibleRoom(roomId, userId);
        ThrowUtils.throwIf(!ChatRoomTypeEnum.GROUP.getCode().equals(room.getType()), ErrorCode.PARAMS_ERROR, "仅支持群聊邀请成员");

        for (Long memberId : sanitizeInviteMembers(memberIds, userId)) {
            ThrowUtils.throwIf(!userFriendService.isMutualFriend(userId, memberId), ErrorCode.NO_AUTH_ERROR, "仅支持邀请好友入群");
            chatRoomMemberService.addMember(roomId, memberId, ChatRoomRoleEnum.MEMBER.getCode());
            chatSessionService.updateSession(memberId, roomId, null, false);
            pushSessionUpdate(memberId, roomId, "session_invite:" + roomId + ":" + memberId);
            trySendGroupInviteNotification(memberId, room);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGroupProfile(Long roomId, String name, String avatar, String announcement, Long userId) {
        ThrowUtils.throwIf(roomId == null || userId == null, ErrorCode.PARAMS_ERROR);

        ChatRoom room = this.getById(roomId);
        ThrowUtils.throwIf(room == null, ErrorCode.NOT_FOUND_ERROR, "聊天室不存在");
        ThrowUtils.throwIf(!ChatRoomTypeEnum.GROUP.getCode().equals(room.getType()), ErrorCode.PARAMS_ERROR, "仅群聊支持群资料更新");
        ThrowUtils.throwIf(!chatRoomMemberService.isOwner(roomId, userId), ErrorCode.NO_AUTH_ERROR, "仅群主可编辑群聊资料");

        boolean hasName = name != null;
        boolean hasAvatar = avatar != null;
        boolean hasAnnouncement = announcement != null;
        ThrowUtils.throwIf(!hasName && !hasAvatar && !hasAnnouncement, ErrorCode.PARAMS_ERROR, "至少更新一项群资料");

        if (hasName) {
            ThrowUtils.throwIf(StringUtils.isBlank(name), ErrorCode.PARAMS_ERROR, "群聊名称不能为空");
            room.setName(name);
        }
        if (hasAvatar) {
            ThrowUtils.throwIf(StringUtils.isBlank(avatar), ErrorCode.PARAMS_ERROR, "群聊头像不能为空");
            room.setAvatar(avatar);
        }

        boolean result = this.updateById(room);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "更新群聊资料失败");

        ChatGroupInfo groupInfo = chatGroupInfoService.getOne(new LambdaQueryWrapper<ChatGroupInfo>()
                .eq(ChatGroupInfo::getRoomId, roomId)
                .last("LIMIT 1"));
        if (groupInfo == null) {
            groupInfo = new ChatGroupInfo();
            groupInfo.setRoomId(roomId);
            groupInfo.setGroupName(room.getName());
            groupInfo.setGroupAvatar(room.getAvatar());
            groupInfo.setCreateUser(room.getCreateUser());
        }
        if (hasName) {
            groupInfo.setGroupName(name);
        }
        if (hasAvatar) {
            groupInfo.setGroupAvatar(avatar);
        }
        if (hasAnnouncement) {
            groupInfo.setAnnouncement(announcement);
        }
        if (StringUtils.isBlank(groupInfo.getGroupName())) {
            groupInfo.setGroupName(room.getName());
        }
        if (StringUtils.isBlank(groupInfo.getGroupAvatar())) {
            groupInfo.setGroupAvatar(room.getAvatar());
        }

        groupInfo.setCreateUser(room.getCreateUser());
        chatGroupInfoService.validChatGroupInfo(groupInfo);

        boolean groupInfoUpdated;
        if (groupInfo.getId() == null) {
            groupInfoUpdated = chatGroupInfoService.save(groupInfo);
            ThrowUtils.throwIf(!groupInfoUpdated, ErrorCode.OPERATION_ERROR, "更新群聊资料失败");
        } else {
            groupInfoUpdated = chatGroupInfoService.updateById(groupInfo);
            ThrowUtils.throwIf(!groupInfoUpdated, ErrorCode.OPERATION_ERROR, "更新群聊资料失败");
        }

        pushSessionUpdateByMember(roomId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long roomId, Long memberId, Long userId) {
        ThrowUtils.throwIf(roomId == null || memberId == null || userId == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(Objects.equals(memberId, userId), ErrorCode.PARAMS_ERROR, "群主不能通过移除成员接口移除自己");

        ChatRoom room = this.getById(roomId);
        ThrowUtils.throwIf(room == null, ErrorCode.NOT_FOUND_ERROR, "聊天室不存在");
        ThrowUtils.throwIf(!ChatRoomTypeEnum.GROUP.getCode().equals(room.getType()), ErrorCode.PARAMS_ERROR, "仅群聊支持移除成员");
        ThrowUtils.throwIf(!chatRoomMemberService.isOwner(roomId, userId), ErrorCode.NO_AUTH_ERROR, "仅群主可移除成员");

        ChatRoomMember targetMember = chatRoomMemberService.getMember(roomId, memberId);
        ThrowUtils.throwIf(targetMember == null, ErrorCode.NOT_FOUND_ERROR, "成员不在此群聊中");
        ThrowUtils.throwIf(ChatRoomRoleEnum.OWNER.getCode().equals(targetMember.getRole()),
                ErrorCode.OPERATION_ERROR, "不能移除群主");
        ThrowUtils.throwIf(!ChatRoomRoleEnum.MEMBER.getCode().equals(targetMember.getRole()),
                ErrorCode.NO_AUTH_ERROR, "当前版本仅支持移除普通成员");

        chatSessionService.remove(new LambdaQueryWrapper<com.stephen.cloud.chat.model.entity.ChatSession>()
                .eq(com.stephen.cloud.chat.model.entity.ChatSession::getUserId, memberId)
                .eq(com.stephen.cloud.chat.model.entity.ChatSession::getRoomId, roomId));
        chatRoomMemberService.leaveRoom(roomId, memberId);
        try {
            chatMqProducer.sendSessionDelete(memberId, roomId, "session_member_remove:" + roomId + ":" + memberId);
        } catch (Exception e) {
            log.warn("[ChatRoomServiceImpl] 推送成员移除会话删除失败, roomId={}, userId={}, reason={}",
                    roomId, memberId, e.toString());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void quitRoom(Long roomId, Long userId) {
        ChatRoom room = getAccessibleRoom(roomId, userId);
        ThrowUtils.throwIf(!ChatRoomTypeEnum.GROUP.getCode().equals(room.getType()), ErrorCode.PARAMS_ERROR, "仅群聊支持退群");
        ThrowUtils.throwIf(chatRoomMemberService.isOwner(roomId, userId), ErrorCode.OPERATION_ERROR, "群主不能直接退群，请先解散群聊");

        chatRoomMemberService.leaveRoom(roomId, userId);
        chatSessionService.remove(new LambdaQueryWrapper<com.stephen.cloud.chat.model.entity.ChatSession>()
                .eq(com.stephen.cloud.chat.model.entity.ChatSession::getUserId, userId)
                .eq(com.stephen.cloud.chat.model.entity.ChatSession::getRoomId, roomId));
        try {
            chatMqProducer.sendSessionDelete(userId, roomId, "session_quit:" + roomId + ":" + userId);
        } catch (Exception e) {
            log.warn("[ChatRoomServiceImpl] 推送退群会话删除失败, roomId={}, userId={}, reason={}",
                    roomId, userId, e.toString());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dismissRoom(Long roomId, Long userId) {
        ChatRoom room = getAccessibleRoom(roomId, userId);
        ThrowUtils.throwIf(!ChatRoomTypeEnum.GROUP.getCode().equals(room.getType()), ErrorCode.PARAMS_ERROR, "仅群聊支持解散");
        ThrowUtils.throwIf(!chatRoomMemberService.isOwner(roomId, userId), ErrorCode.NO_AUTH_ERROR, "仅群主可解散群聊");

        List<ChatRoomMember> members = chatRoomMemberService.listByRoomId(roomId);
        for (ChatRoomMember member : members) {
            chatRoomMemberService.leaveRoom(roomId, member.getUserId());
            try {
                chatMqProducer.sendSessionDelete(member.getUserId(), roomId, "session_dismiss:" + roomId + ":" + member.getUserId());
            } catch (Exception e) {
                log.warn("[ChatRoomServiceImpl] 推送群解散会话删除失败, roomId={}, userId={}, reason={}",
                        roomId, member.getUserId(), e.toString());
            }
        }
        chatSessionService.remove(new LambdaQueryWrapper<com.stephen.cloud.chat.model.entity.ChatSession>()
                .eq(com.stephen.cloud.chat.model.entity.ChatSession::getRoomId, roomId));
        chatGroupInfoService.remove(new LambdaQueryWrapper<ChatGroupInfo>().eq(ChatGroupInfo::getRoomId, roomId));
        this.removeById(roomId);
    }

    private ChatRoom getAccessibleRoom(Long roomId, Long userId) {
        ThrowUtils.throwIf(roomId == null || userId == null, ErrorCode.PARAMS_ERROR);
        ChatRoom room = this.getById(roomId);
        ThrowUtils.throwIf(room == null, ErrorCode.NOT_FOUND_ERROR, "聊天室不存在");
        ThrowUtils.throwIf(!chatRoomMemberService.isMember(roomId, userId), ErrorCode.NO_AUTH_ERROR, "您不在此聊天室中");
        return room;
    }

    private List<Long> sanitizeInviteMembers(List<Long> memberIds, Long currentUserId) {
        if (CollUtil.isEmpty(memberIds)) {
            return Collections.emptyList();
        }
        Set<Long> deduplicated = new LinkedHashSet<>();
        for (Long memberId : memberIds) {
            if (memberId == null || memberId <= 0 || Objects.equals(memberId, currentUserId)) {
                continue;
            }
            deduplicated.add(memberId);
        }
        return new ArrayList<>(deduplicated);
    }

    private List<ChatRoomVO> buildRoomVOList(List<ChatRoom> rooms, Long currentUserId) {
        if (CollUtil.isEmpty(rooms)) {
            return Collections.emptyList();
        }
        List<Long> roomIds = rooms.stream().map(ChatRoom::getId).toList();

        Map<Long, Integer> memberCountMap = chatRoomMemberService.list(new LambdaQueryWrapper<ChatRoomMember>()
                        .in(ChatRoomMember::getRoomId, roomIds))
                .stream()
                .collect(Collectors.groupingBy(ChatRoomMember::getRoomId, Collectors.summingInt(item -> 1)));

        Map<Long, ChatGroupInfo> groupInfoMap = chatGroupInfoService.list(new LambdaQueryWrapper<ChatGroupInfo>()
                        .in(ChatGroupInfo::getRoomId, roomIds))
                .stream()
                .collect(Collectors.toMap(ChatGroupInfo::getRoomId, Function.identity(), (left, right) -> left));

        Map<Long, ChatPrivateRoom> privateRoomMap = chatPrivateRoomService.list(new LambdaQueryWrapper<ChatPrivateRoom>()
                        .in(ChatPrivateRoom::getRoomId, roomIds))
                .stream()
                .collect(Collectors.toMap(ChatPrivateRoom::getRoomId, Function.identity(), (left, right) -> left));

        Set<Long> peerUserIds = privateRoomMap.values().stream()
                .map(privateRoom -> Objects.equals(privateRoom.getUserLow(), currentUserId) ? privateRoom.getUserHigh() : privateRoom.getUserLow())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userMap = getUserMap(peerUserIds);

        return rooms.stream().map(room -> {
            ChatRoomVO roomVO = ChatRoomConvert.objToVo(room);
            roomVO.setOwnerUserId(room.getCreateUser());
            roomVO.setMemberCount(memberCountMap.getOrDefault(room.getId(), 0));

            if (ChatRoomTypeEnum.GROUP.getCode().equals(room.getType())) {
                ChatGroupInfo groupInfo = groupInfoMap.get(room.getId());
                if (groupInfo != null) {
                    roomVO.setName(groupInfo.getGroupName());
                    roomVO.setAvatar(groupInfo.getGroupAvatar());
                    roomVO.setAnnouncement(groupInfo.getAnnouncement());
                    roomVO.setOwnerUserId(groupInfo.getCreateUser());
                }
            } else {
                ChatPrivateRoom privateRoom = privateRoomMap.get(room.getId());
                if (privateRoom != null) {
                    Long peerId = Objects.equals(privateRoom.getUserLow(), currentUserId)
                            ? privateRoom.getUserHigh() : privateRoom.getUserLow();
                    UserVO peerUser = userMap.get(peerId);
                    if (peerUser != null) {
                        roomVO.setName(peerUser.getUserName());
                        roomVO.setAvatar(peerUser.getUserAvatar());
                    }
                }
            }
            return roomVO;
        }).collect(Collectors.toList());
    }

    private Map<Long, UserVO> getUserMap(Collection<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        try {
            List<UserVO> userVOList = userFeignClient.getUserVOByIds(new ArrayList<>(userIds)).getData();
            if (CollUtil.isEmpty(userVOList)) {
                return Collections.emptyMap();
            }
            return userVOList.stream().collect(Collectors.toMap(UserVO::getId, Function.identity(), (left, right) -> left));
        } catch (Exception e) {
            log.error("[ChatRoomServiceImpl] 批量查询用户信息失败", e);
            return Collections.emptyMap();
        }
    }

    private void pushSessionUpdate(Long userId, Long roomId, String bizId) {
        ChatSessionVO sessionVO = chatSessionService.getSessionVO(roomId, userId);
        if (sessionVO != null) {
            chatMqProducer.sendSessionUpdate(userId, roomId, sessionVO, bizId);
        }
    }

    private void pushSessionUpdateByMember(Long roomId) {
        List<ChatRoomMember> members = chatRoomMemberService.listByRoomId(roomId);
        if (CollUtil.isEmpty(members)) {
            return;
        }

        for (ChatRoomMember member : members) {
            if (member == null || member.getUserId() == null) {
                continue;
            }
            try {
                pushSessionUpdate(member.getUserId(), roomId,
                        "session_room_profile_update:" + roomId + ":" + member.getUserId());
            } catch (Exception e) {
                log.warn("[ChatRoomServiceImpl] 推送群资料会话刷新失败, roomId={}, userId={}, reason={}",
                        roomId, member.getUserId(), e.toString());
            }
        }
    }

    private void trySendGroupInviteNotification(Long userId, ChatRoom room) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doTrySendGroupInviteNotification(userId, room);
                }
            });
            return;
        }
        doTrySendGroupInviteNotification(userId, room);
    }

    private void doTrySendGroupInviteNotification(Long userId, ChatRoom room) {
        try {
            sendGroupInviteNotification(userId, room);
        } catch (Exception e) {
            log.warn("[ChatRoomServiceImpl] 发送群邀请通知失败, roomId={}, userId={}, reason={}",
                    room == null ? null : room.getId(), userId, e.getMessage());
        }
    }

    private void sendGroupInviteNotification(Long userId, ChatRoom room) {
        ThrowUtils.throwIf(userId == null || room == null || room.getId() == null, ErrorCode.PARAMS_ERROR);
        NotificationCreateRequest request = new NotificationCreateRequest();
        request.setTitle("群聊邀请");
        request.setContent("你已加入群聊：" + StringUtils.defaultIfBlank(room.getName(), "群聊"));
        request.setType(NotificationTypeEnum.USER.getCode());
        request.setUserId(userId);
        request.setRelatedId(room.getId());
        request.setRelatedType(RELATED_TYPE_CHAT_ROOM);
        request.setContentUrl("/chat/room/detail?id=" + room.getId());
        request.setBizId("group_invite:" + room.getId() + ":" + userId);
        BaseResponse<Long> response = notificationFeignClient.addBusinessNotification(request);
        ThrowUtils.throwIf(response == null || response.getData() == null,
                ErrorCode.OPERATION_ERROR, "创建群邀请通知失败");
    }
}
