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
        chatRoomMemberService.addMember(roomId, userId);
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

        joinChatRoom(chatRoom.getId(), userId);
        joinChatRoom(chatRoom.getId(), peerUserId);

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
        chatMqProducer.sendSessionDelete(userId, roomId, "session_quit:" + roomId + ":" + userId);
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
            chatMqProducer.sendSessionDelete(member.getUserId(), roomId, "session_dismiss:" + roomId + ":" + member.getUserId());
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
}
