package com.stephen.cloud.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.api.chat.model.vo.ChatSessionVO;
import com.stephen.cloud.api.user.client.UserFeignClient;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.chat.convert.ChatSessionConvert;
import com.stephen.cloud.chat.mapper.ChatSessionMapper;
import com.stephen.cloud.chat.model.entity.*;
import com.stephen.cloud.chat.service.*;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 会话服务实现
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession>
        implements ChatSessionService {

    @Resource
    private ChatRoomService chatRoomService;

    @Resource
    private ChatMessageService chatMessageService;

    @Resource
    private UserFeignClient userFeignClient;

    @Resource
    private ChatGroupInfoService chatGroupInfoService;

    @Resource
    private ChatPrivateRoomService chatPrivateRoomService;

    @Override
    public List<ChatSessionVO> listMySessions(Long userId) {
        log.info("[ChatSessionServiceImpl] 获取用户会话列表, userId: {}", userId);
        LambdaQueryWrapper<ChatSession> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatSession::getUserId, userId)
                .orderByDesc(ChatSession::getTopStatus)
                .orderByDesc(ChatSession::getActiveTime);

        List<ChatSession> sessions = this.list(queryWrapper);
        return getChatSessionVO(sessions, userId);
    }

    @Override
    public ChatSessionVO getChatSessionVO(ChatSession chatSession, Long userId) {
        if (chatSession == null) {
            return null;
        }
        List<ChatSessionVO> vos = getChatSessionVO(Collections.singletonList(chatSession), userId);
        return vos.get(0);
    }

    @Override
    public List<ChatSessionVO> getChatSessionVO(List<ChatSession> sessions, Long userId) {
        if (sessions.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询房间信息
        List<Long> roomIds = sessions.stream().map(ChatSession::getRoomId).toList();
        Map<Long, ChatRoom> roomMap = chatRoomService.listByIds(roomIds).stream()
                .collect(Collectors.toMap(ChatRoom::getId, r -> r));

        // 批量查询消息记录 (用于内容展示)
        List<Long> msgIds = sessions.stream().map(ChatSession::getLastMessageId).filter(Objects::nonNull).toList();
        Map<Long, ChatMessage> msgMap = msgIds.isEmpty() ? Collections.emptyMap() :
                chatMessageService.listByIds(msgIds).stream().collect(Collectors.toMap(ChatMessage::getId, m -> m));

        return sessions.stream().map(session -> {
            ChatSessionVO vo = ChatSessionConvert.objToVo(session);
            ChatRoom room = roomMap.get(session.getRoomId());
            if (room != null) {
                vo.setType(room.getType());
                if (room.getType() == 1) {
                    // 群聊：取群组详情
                    ChatGroupInfo groupInfo = chatGroupInfoService.getOne(new LambdaQueryWrapper<ChatGroupInfo>()
                            .eq(ChatGroupInfo::getRoomId, room.getId()));
                    if (groupInfo != null) {
                        vo.setName(groupInfo.getGroupName());
                        vo.setAvatar(groupInfo.getGroupAvatar());
                    } else {
                        vo.setName(room.getName());
                    }
                } else {
                    // 私聊：取对方头像昵称
                    ChatPrivateRoom privateRoom = chatPrivateRoomService.getOne(new LambdaQueryWrapper<ChatPrivateRoom>()
                            .eq(ChatPrivateRoom::getRoomId, room.getId()));
                    if (privateRoom != null) {
                        Long peerId = privateRoom.getUserLow().equals(userId) ? privateRoom.getUserHigh() : privateRoom.getUserLow();
                        UserVO peer = userFeignClient.getUserVOById(peerId).getData();
                        if (peer != null) {
                            vo.setName(peer.getUserName());
                            vo.setAvatar(peer.getUserAvatar());
                        }
                    }
                }
            }

            ChatMessage lastMsg = msgMap.get(session.getLastMessageId());
            if (lastMsg != null) {
                vo.setLastMessage(lastMsg.getContent());
            }

            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public boolean topSession(Long roomId, Long userId, Integer topStatus) {
        ChatSession session = getSession(userId, roomId);
        ThrowUtils.throwIf(session == null, ErrorCode.NOT_FOUND_ERROR);
        if (session == null) return false;
        session.setTopStatus(topStatus);
        return this.updateById(session);
    }

    @Override
    public boolean deleteSession(Long roomId, Long userId) {
        LambdaQueryWrapper<ChatSession> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatSession::getRoomId, roomId).eq(ChatSession::getUserId, userId);
        return this.remove(queryWrapper);
    }

    @Override
    public void updateSession(Long userId, Long roomId, Long lastMessageId, boolean incrementUnread) {
        if (userId == null || roomId == null) return;
        log.info("[ChatSessionServiceImpl] 更新会话状态, userId: {}, roomId: {}, messageId: {}, incrementUnread: {}", 
                userId, roomId, lastMessageId, incrementUnread);
        ChatSession session = getSession(userId, roomId);
        if (session == null) {
            session = new ChatSession();
            session.setUserId(userId);
            session.setRoomId(roomId);
            session.setUnreadCount(0);
            session.setTopStatus(0);
        }
        session.setLastMessageId(lastMessageId);
        session.setActiveTime(new java.util.Date());
        if (incrementUnread) {
            Integer currentUnread = session.getUnreadCount();
            session.setUnreadCount((currentUnread == null ? 0 : currentUnread) + 1);
        }
        this.saveOrUpdate(session);
    }

    private ChatSession getSession(Long userId, Long roomId) {
        return this.getOne(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId)
                .eq(ChatSession::getRoomId, roomId));
    }
}
