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

        // 1. 批量查询基础数据 (房间、消息)
        List<Long> roomIds = sessions.stream().map(ChatSession::getRoomId).toList();
        Map<Long, ChatRoom> roomMap = chatRoomService.listByIds(roomIds).stream()
                .collect(Collectors.toMap(ChatRoom::getId, r -> r));

        List<Long> msgIds = sessions.stream().map(ChatSession::getLastMessageId).filter(Objects::nonNull).toList();
        Map<Long, ChatMessage> msgMap = msgIds.isEmpty() ? Collections.emptyMap() :
                chatMessageService.listByIds(msgIds).stream().collect(Collectors.toMap(ChatMessage::getId, m -> m));

        // 2. 分类处理房间扩展信息 (批量查询群组信息和私聊映射)
        List<Long> groupRoomIds = roomMap.values().stream()
                .filter(r -> r.getType() == 1).map(ChatRoom::getId).toList();
        Map<Long, ChatGroupInfo> groupInfoMap = groupRoomIds.isEmpty() ? Collections.emptyMap() :
                chatGroupInfoService.list(new LambdaQueryWrapper<ChatGroupInfo>().in(ChatGroupInfo::getRoomId, groupRoomIds))
                        .stream().collect(Collectors.toMap(ChatGroupInfo::getRoomId, g -> g));

        List<Long> privateRoomIds = roomMap.values().stream()
                .filter(r -> r.getType() == 2).map(ChatRoom::getId).toList();
        List<ChatPrivateRoom> privateRooms = privateRoomIds.isEmpty() ? Collections.emptyList() :
                chatPrivateRoomService.list(new LambdaQueryWrapper<ChatPrivateRoom>().in(ChatPrivateRoom::getRoomId, privateRoomIds));
        
        Map<Long, Long> roomToPeerIdMap = new HashMap<>();
        for (ChatPrivateRoom pr : privateRooms) {
            Long peerId = pr.getUserLow().equals(userId) ? pr.getUserHigh() : pr.getUserLow();
            roomToPeerIdMap.put(pr.getRoomId(), peerId);
        }

        // 3. 批量获取用户信息 (Feign 调用)
        Collection<Long> peerIds = roomToPeerIdMap.values();
        Map<Long, UserVO> userMap = Collections.emptyMap();
        if (!peerIds.isEmpty()) {
            try {
                List<UserVO> users = userFeignClient.getUserVOByIds(new ArrayList<>(peerIds)).getData();
                if (users != null) {
                    userMap = users.stream().collect(Collectors.toMap(UserVO::getId, u -> u));
                }
            } catch (Exception e) {
                log.error("[ChatSessionServiceImpl] 批量获取用户信息失败", e);
            }
        }

        // 4. 组装 VO
        Map<Long, UserVO> finalUserMap = userMap;
        return sessions.stream().map(session -> {
            ChatSessionVO vo = ChatSessionConvert.objToVo(session);
            ChatRoom room = roomMap.get(session.getRoomId());
            if (room != null) {
                vo.setType(room.getType());
                if (room.getType() == 1) {
                    ChatGroupInfo groupInfo = groupInfoMap.get(room.getId());
                    if (groupInfo != null) {
                        vo.setName(groupInfo.getGroupName());
                        vo.setAvatar(groupInfo.getGroupAvatar());
                    } else {
                        vo.setName(room.getName());
                    }
                } else {
                    Long peerId = roomToPeerIdMap.get(room.getId());
                    UserVO peer = finalUserMap.get(peerId);
                    if (peer != null) {
                        vo.setName(peer.getUserName());
                        vo.setAvatar(peer.getUserAvatar());
                    }
                }
            }

            // 处理最后一条消息展示逻辑 (撤回、占位符)
            ChatMessage lastMsg = msgMap.get(session.getLastMessageId());
            if (lastMsg != null) {
                vo.setLastMessage(formatLastMessage(lastMsg));
            }

            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 格式化会话预览消息
     */
    private String formatLastMessage(ChatMessage msg) {
        if (msg == null) return "";
        // 撤回识别
        if (Objects.equals(msg.getStatus(), 1)) { // 1 为已撤回 (MessageStatusEnum.RECALL.getCode())
            return "[该消息已被撤回]";
        }
        // 类型识别
        if (!Objects.equals(msg.getType(), 1)) { // 非文本
            return switch (msg.getType()) {
                case 2 -> "[图片]";
                case 3 -> "[文件]";
                default -> "[消息]";
            };
        }
        return msg.getContent();
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
