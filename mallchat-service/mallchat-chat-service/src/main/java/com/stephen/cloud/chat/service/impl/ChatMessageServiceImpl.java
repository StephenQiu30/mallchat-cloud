package com.stephen.cloud.chat.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.api.chat.model.enums.MessageStatusEnum;
import com.stephen.cloud.api.chat.model.vo.ChatMessageVO;
import com.stephen.cloud.api.chat.model.vo.ReplyMsgVO;
import com.stephen.cloud.chat.convert.ChatMessageConvert;
import com.stephen.cloud.chat.event.ChatMessageSentEvent;
import com.stephen.cloud.chat.mapper.ChatMessageMapper;
import com.stephen.cloud.chat.model.entity.*;
import com.stephen.cloud.chat.mq.producer.ChatMqProducer;
import com.stephen.cloud.chat.service.*;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.exception.BusinessException;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 聊天消息服务实现类
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage>
        implements ChatMessageService {

    @Resource
    private ChatRoomMemberService chatRoomMemberService;

    @Resource
    private ChatMqProducer chatMqProducer;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    @Resource
    private ChatRoomService chatRoomService;

    @Resource
    private ChatPrivateRoomService chatPrivateRoomService;

    @Resource
    @Lazy
    private ChatSessionService chatSessionService;

    @Resource
    private UserFriendService userFriendService;

    /**
     * 校验数据
     *
     * @param chatMessage 聊天消息实体
     */
    @Override
    public void validChatMessage(ChatMessage chatMessage) {
        // 仅保留核心业务校验
        Long roomId = chatMessage.getRoomId();
        ThrowUtils.throwIf(roomId == null, ErrorCode.PARAMS_ERROR, "房间号不能为空");
    }

    /**
     * 获取聊天消息视图类
     *
     * @param chatMessage 聊天消息
     * @param request     请求
     * @return {@link ChatMessageVO}
     */
    @Override
    public ChatMessageVO getChatMessageVO(ChatMessage chatMessage, HttpServletRequest request) {
        if (chatMessage == null) {
            return null;
        }
        ChatMessageVO vo = ChatMessageConvert.objToVo(chatMessage);

        // 1. 消息撤回内容脱敏
        if (Objects.equals(chatMessage.getStatus(), MessageStatusEnum.RECALL.getCode())) {
            vo.setContent("该消息已被撤回");
        }

        // 2. 填充回复消息内容
        if (chatMessage.getReplyMsgId() != null) {
            ChatMessage replyMsg = this.getById(chatMessage.getReplyMsgId());
            if (replyMsg != null) {
                vo.setReplyMsg(buildReplyMsgVO(replyMsg));
            }
        }

        return vo;
    }

    /**
     * 构建回复消息视图
     */
    private ReplyMsgVO buildReplyMsgVO(ChatMessage replyMsg) {
        if (replyMsg == null) return null;

        String content = replyMsg.getContent();
        // 如果是撤回状态，脱敏内容
        if (Objects.equals(replyMsg.getStatus(), MessageStatusEnum.RECALL.getCode())) {
            content = "该消息已被撤回";
            // 如果不是文本，显示占位符
            content = switch (Optional.ofNullable(replyMsg.getType()).orElse(1)) {
                case 2 -> "[图片]";
                case 3 -> "[文件]";
                default -> "[消息]";
            };
        }

        return ReplyMsgVO.builder()
                .id(replyMsg.getId())
                .content(content)
                .type(replyMsg.getType())
                .build();
    }

    /**
     * 获取聊天消息视图类列表
     *
     * @param chatMessageList 聊天消息列表
     * @param request         请求
     * @return {@link List<ChatMessageVO>}
     */
    @Override
    public List<ChatMessageVO> getChatMessageVO(List<ChatMessage> chatMessageList, HttpServletRequest request) {
        if (CollUtil.isEmpty(chatMessageList)) {
            return Collections.emptyList();
        }

        // 1. 批量获取基础 VO
        List<ChatMessageVO> voList = ChatMessageConvert.getChatMessageVO(chatMessageList);

        // 2. 批量获取被回复的消息内容 (解决 N+1 问题)
        List<Long> replyIds = chatMessageList.stream()
                .map(ChatMessage::getReplyMsgId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, ChatMessage> replyMsgMap = CollUtil.isEmpty(replyIds) ?
                Collections.emptyMap() :
                this.listByIds(replyIds).stream().collect(Collectors.toMap(ChatMessage::getId, m -> m));

        // 3. 填充扩展信息 (撤回脱敏、回复消息内容)
        Map<Long, ChatMessage> chatMessageMap = chatMessageList.stream()
                .collect(Collectors.toMap(ChatMessage::getId, m -> m));

        voList.forEach(vo -> {
            ChatMessage msg = chatMessageMap.get(vo.getId());
            if (msg != null) {
                // 撤回脱敏
                if (Objects.equals(msg.getStatus(), MessageStatusEnum.RECALL.getCode())) {
                    vo.setContent("该消息已被撤回");
                }
                // 填充回复内容
                if (msg.getReplyMsgId() != null) {
                    vo.setReplyMsg(buildReplyMsgVO(replyMsgMap.get(msg.getReplyMsgId())));
                }
            }
        });

        return voList;
    }

    /**
     * 分页获取聊天消息视图类
     *
     * @param chatMessagePage 聊天消息分页对象
     * @param request         请求
     * @return {@link Page<ChatMessageVO>}
     */
    @Override
    public Page<ChatMessageVO> getChatMessageVOPage(Page<ChatMessage> chatMessagePage, HttpServletRequest request) {
        // 使用标准化 Convert 简化逻辑
        List<ChatMessage> records = chatMessagePage.getRecords();
        Page<ChatMessageVO> voPage = new Page<>(chatMessagePage.getCurrent(), chatMessagePage.getSize(), chatMessagePage.getTotal());
        if (CollUtil.isEmpty(records)) {
            return voPage;
        }
        voPage.setRecords(getChatMessageVO(records, request));
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long sendMessage(ChatMessage chatMessage, Long userId) {
        if (chatMessage == null || userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        validChatMessage(chatMessage);
        log.info("用户发送消息: userId={}, roomId={}", userId, chatMessage.getRoomId());

        Long roomId = chatMessage.getRoomId();
        ChatRoom chatRoom = chatRoomService.getById(roomId);
        if (chatRoom == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "聊天室不存在");
        }

        // 校验发送权限：私聊需互为好友，群聊需为群成员
        if (Objects.equals(chatRoom.getType(), 2)) {
            ChatPrivateRoom privateRoom = chatPrivateRoomService.getOne(
                    new LambdaQueryWrapper<ChatPrivateRoom>()
                            .eq(ChatPrivateRoom::getRoomId, roomId)
                            .last("LIMIT 1"));
            ThrowUtils.throwIf(privateRoom == null, ErrorCode.SYSTEM_ERROR, "私聊房间映射不存在");

            Long peerUserId = Objects.equals(userId, privateRoom.getUserLow()) ? privateRoom.getUserHigh() : privateRoom.getUserLow();
            ThrowUtils.throwIf(!userFriendService.isMutualFriend(userId, peerUserId), ErrorCode.NO_AUTH_ERROR, "非好友无法发送消息");
        } else {
            ThrowUtils.throwIf(!chatRoomMemberService.isMember(roomId, userId), ErrorCode.NO_AUTH_ERROR, "您不在此聊天室中");
        }

        // 校验回复消息是否属于当前房间
        if (chatMessage.getReplyMsgId() != null) {
            ChatMessage replyMsg = this.getById(chatMessage.getReplyMsgId());
            ThrowUtils.throwIf(replyMsg == null || !Objects.equals(replyMsg.getRoomId(), roomId), ErrorCode.PARAMS_ERROR, "回复消息非法");
        }

        chatMessage.setFromUserId(userId);
        chatMessage.setStatus(MessageStatusEnum.NORMAL.getCode());
        boolean result = this.save(chatMessage);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "发送消息失败");

        // 推送消息给所有成员
        ChatMessageVO vo = ChatMessageConvert.objToVo(chatMessage);
        chatMqProducer.sendChatMessageGroupPush(chatMessage.getRoomId(), vo);

        eventPublisher.publishEvent(new ChatMessageSentEvent(this, chatMessage, userId));

        return chatMessage.getId();
    }

    @Override
    public List<ChatMessageVO> listHistoryMessages(Long roomId, Long lastMessageId, Integer limit, Long userId) {
        ThrowUtils.throwIf(roomId == null || userId == null, ErrorCode.PARAMS_ERROR);
        long memberCount = chatRoomMemberService.count(
                new LambdaQueryWrapper<ChatRoomMember>()
                        .eq(ChatRoomMember::getRoomId, roomId)
                        .eq(ChatRoomMember::getUserId, userId));
        ThrowUtils.throwIf(memberCount == 0, ErrorCode.NO_AUTH_ERROR, "您不在此聊天室中");
        if (limit == null || limit <= 0) {
            limit = 20;
        }

        LambdaQueryWrapper<ChatMessage> queryWrapper = new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getRoomId, roomId);

        // 如果提供了 lastMessageId，则加载比该 ID 更小的消息 (即历史消息)
        if (lastMessageId != null && lastMessageId > 0) {
            queryWrapper.lt(ChatMessage::getId, lastMessageId);
        }

        List<ChatMessage> messages = this.list(
                queryWrapper.orderByDesc(ChatMessage::getId) // 按 ID 倒序排列即为时间倒序
                        .last("limit " + limit)
        );

        return getChatMessageVO(messages, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markMessageRead(Long roomId, Long lastReadMessageId, Long userId) {
        if (roomId == null || lastReadMessageId == null || userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 1. 权限校验
        boolean isMember = chatRoomMemberService.isMember(roomId, userId);
        if (!isMember) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您不在此聊天室中");
        }

        // 2. 校验消息是否属于该房间
        ChatMessage msg = this.getById(lastReadMessageId);
        if (msg == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "消息不存在");
        }
        if (!roomId.equals(msg.getRoomId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息不属于该房间");
        }

        // 3. 更新成员已读游标
        ChatRoomMember member = chatRoomMemberService.getOne(
                new LambdaQueryWrapper<ChatRoomMember>()
                        .eq(ChatRoomMember::getRoomId, roomId)
                        .eq(ChatRoomMember::getUserId, userId)
                        .last("LIMIT 1"));
        if (member == null) return false;

        Long oldRead = member.getLastReadMessageId();
        if (oldRead != null && lastReadMessageId <= oldRead) {
            return true;
        }
        member.setLastReadMessageId(lastReadMessageId);
        chatRoomMemberService.updateById(member);

        // 4. 同步更新会话未读数 (重置为0)
        chatSessionService.update(new LambdaUpdateWrapper<ChatSession>()
                .set(ChatSession::getUnreadCount, 0)
                .set(ChatSession::getLastReadMessageId, lastReadMessageId)
                .eq(ChatSession::getUserId, userId)
                .eq(ChatSession::getRoomId, roomId));

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean recallMessage(Long messageId, Long userId) {
        log.info("[ChatMessageServiceImpl] 撤回消息请求, messageId: {}, userId: {}", messageId, userId);

        // 1. 获取并检查原始消息是否存在
        ChatMessage msg = this.getById(messageId);
        ThrowUtils.throwIf(msg == null, ErrorCode.NOT_FOUND_ERROR, "消息不存在");

        // 2. 权限校验：只能撤回自己发送的消息 (B1 修复：移除冗余 msg == null 检查)
        if (!Objects.equals(msg.getFromUserId(), userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能撤回自己的消息");
        }

        // 3. 时限校验：限制在消息发送后的 2 分钟内可撤回
        long now = System.currentTimeMillis();
        long createTime = msg.getCreateTime().getTime();
        if (now - createTime > 2 * 60 * 1000) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "消息发送超过 2 分钟，无法撤回");
        }

        // 4. 更新数据库状态为已撤回 (状态码 1)
        if (Objects.equals(msg.getStatus(), MessageStatusEnum.RECALL.getCode())) {
            return true;
        }
        msg.setStatus(MessageStatusEnum.RECALL.getCode());
        boolean ok = this.updateById(msg);
        if (!ok) return false;

        // 5. 发送撤回广播信号 (推送撤回消息的 VO 给所有成员)
        ChatMessageVO vo = ChatMessageConvert.objToVo(msg);
        chatMqProducer.sendChatMessageGroupPush(msg.getRoomId(), vo);

        return true;
    }

}
