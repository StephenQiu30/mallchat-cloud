package com.stephen.cloud.chat.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.api.chat.model.enums.MessageStatusEnum;
import com.stephen.cloud.api.chat.model.vo.ChatSessionVO;
import com.stephen.cloud.api.user.client.UserFeignClient;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.chat.convert.ChatSessionConvert;
import com.stephen.cloud.chat.mapper.ChatSessionMapper;
import com.stephen.cloud.chat.model.entity.*;
import com.stephen.cloud.chat.service.*;
import com.stephen.cloud.common.auth.utils.SecurityUtils;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        return getChatSessionVO(sessions, (HttpServletRequest) null);
    }

    /**
     * 获取会话视图
     *
     * @param chatSession 会话实体
     * @param request     HTTP 请求
     * @return 会话视图
     */
    @Override
    public ChatSessionVO getChatSessionVO(ChatSession chatSession, HttpServletRequest request) {
        if (chatSession == null) {
            return null;
        }
        List<ChatSessionVO> vos = getChatSessionVO(Collections.singletonList(chatSession), request);
        return vos.get(0);
    }

    /**
     * 批量获取会话视图
     *
     * @param sessions 会话实体列表
     * @param request  HTTP 请求
     * @return 会话视图列表
     */
    @Override
    public List<ChatSessionVO> getChatSessionVO(List<ChatSession> sessions, HttpServletRequest request) {
        if (sessions.isEmpty()) {
            return Collections.emptyList();
        }
        Long userId = SecurityUtils.getLoginUserId();

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

            // 4.3 处理最后一条消息预览 (撤回脱敏、消息类型占位符)
            ChatMessage lastMsg = msgMap.get(session.getLastMessageId());
            if (lastMsg != null) {
                if (Objects.equals(lastMsg.getStatus(), MessageStatusEnum.RECALL.getCode())) {
                    vo.setLastMessage("[该消息已被撤回]");
                } else {
                    // 处理非文本消息的占位符展示 (inline 逻辑减少跨服务调用)
                    String preview = switch (Optional.ofNullable(lastMsg.getType()).orElse(1)) {
                        case 2 -> "[图片]";
                        case 3 -> "[文件]";
                        default -> lastMsg.getContent();
                    };
                    vo.setLastMessage(preview);
                }
            }

            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public boolean topSession(Long roomId, Long userId, Integer topStatus) {
        // 获取会话实体
        ChatSession session = this.getOne(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId)
                .eq(ChatSession::getRoomId, roomId));
        ThrowUtils.throwIf(session == null, ErrorCode.NOT_FOUND_ERROR);
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
        ChatSession session = this.getOne(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId)
                .eq(ChatSession::getRoomId, roomId));
        if (session == null) {
            session = new ChatSession();
            session.setUserId(userId);
            session.setRoomId(roomId);
            session.setUnreadCount(0);
            session.setTopStatus(0);
        }
        session.setLastMessageId(lastMessageId);
        session.setActiveTime(new Date());
        if (incrementUnread) {
            Integer currentUnread = session.getUnreadCount();
            session.setUnreadCount((currentUnread == null ? 0 : currentUnread) + 1);
        }
        this.saveOrUpdate(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSessionBatch(List<Long> userIds, Long roomId, Long lastMessageId, Long senderId) {
        if (CollUtil.isEmpty(userIds) || roomId == null) return;
        log.info("[ChatSessionServiceImpl] 批量更新会话状态, roomId: {}, messageId: {}, usersSize: {}",
                roomId, lastMessageId, userIds.size());

        // 1. 批量查询已存在的会话
        List<ChatSession> existingSessions = this.list(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getRoomId, roomId)
                .in(ChatSession::getUserId, userIds));
        Map<Long, ChatSession> sessionMap = existingSessions.stream()
                .collect(Collectors.toMap(ChatSession::getUserId, s -> s));

        List<ChatSession> toUpdate = new ArrayList<>();
        Date now = new Date();

        // 2. 遍历用户，补全或更新会话
        for (Long userId : userIds) {
            ChatSession session = sessionMap.get(userId);
            if (session == null) {
                session = new ChatSession();
                session.setUserId(userId);
                session.setRoomId(roomId);
                session.setUnreadCount(0);
                session.setTopStatus(0);
            }
            session.setLastMessageId(lastMessageId);
            session.setActiveTime(now);
            // 发送者不增加未读数
            if (!userId.equals(senderId)) {
                Integer currentUnread = session.getUnreadCount();
                session.setUnreadCount((currentUnread == null ? 0 : currentUnread) + 1);
            }
            toUpdate.add(session);
        }

        // 3. 批量保存或更新
        this.saveOrUpdateBatch(toUpdate);
    }
}
