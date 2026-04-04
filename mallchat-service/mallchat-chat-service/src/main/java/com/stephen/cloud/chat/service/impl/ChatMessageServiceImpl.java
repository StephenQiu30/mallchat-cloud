package com.stephen.cloud.chat.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.api.chat.model.vo.ChatMessageVO;
import com.stephen.cloud.chat.convert.ChatMessageConvert;
import com.stephen.cloud.chat.event.ChatMessageSentEvent;
import com.stephen.cloud.chat.mapper.ChatMessageMapper;
import com.stephen.cloud.chat.model.entity.ChatMessage;
import com.stephen.cloud.chat.model.entity.ChatRoomMember;
import com.stephen.cloud.chat.service.ChatMessageService;
import com.stephen.cloud.chat.service.ChatRoomMemberService;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.chat.mq.producer.ChatMqProducer;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
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

    /**
     * 校验数据
     *
     * @param chatMessage 聊天消息实体
     * @param add         是否为新增操作
     */
    @Override
    public void validChatMessage(ChatMessage chatMessage, boolean add) {
        if (chatMessage == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 从实体中获取数据
        String content = chatMessage.getContent();
        Long roomId = chatMessage.getRoomId();
        // 修改数据时，id 不能为空
        if (!add) {
            ThrowUtils.throwIf(chatMessage.getId() == null, ErrorCode.PARAMS_ERROR);
        }
        // 补充校验规则
        if (StringUtils.isNotBlank(content)) {
            ThrowUtils.throwIf(content.length() > 1024, ErrorCode.PARAMS_ERROR, "内容过长");
        }
        if (roomId == null) {
            ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR, "房间号不能为空");
        }
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
        return ChatMessageConvert.objToVo(chatMessage);
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
        return chatMessageList.stream().map(chatMessage -> getChatMessageVO(chatMessage, request)).collect(Collectors.toList());
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
        List<ChatMessage> chatMessageList = chatMessagePage.getRecords();
        Page<ChatMessageVO> chatMessageVOPage = new Page<>(chatMessagePage.getCurrent(), chatMessagePage.getSize(), chatMessagePage.getTotal());
        if (CollUtil.isEmpty(chatMessageList)) {
            return chatMessageVOPage;
        }
        List<ChatMessageVO> chatMessageVOList = getChatMessageVO(chatMessageList, request);
        chatMessageVOPage.setRecords(chatMessageVOList);
        return chatMessageVOPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long sendMessage(ChatMessage chatMessage, Long userId) {
        if (chatMessage == null || userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        validChatMessage(chatMessage, true);
        log.info("用户发送消息: userId={}, roomId={}", userId, chatMessage.getRoomId());

        Long roomId = chatMessage.getRoomId();

        // 1. 校验用户是否在房间中
        long count = chatRoomMemberService.count(
                new LambdaQueryWrapper<ChatRoomMember>()
                        .eq(ChatRoomMember::getRoomId, roomId)
                        .eq(ChatRoomMember::getUserId, userId)
        );
        ThrowUtils.throwIf(count == 0, ErrorCode.NO_AUTH_ERROR, "您不在此聊天室中");

        // 2. 保存消息到数据库
        chatMessage.setFromUserId(userId);
        boolean result = this.save(chatMessage);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "发送消息失败");

        // 3. 异步推送消息到所有房间成员 (RabbitMQ) 并更新会话列表
        List<ChatRoomMember> members = chatRoomMemberService.listByRoomId(roomId);
        pushMessageToRoomMembers(chatMessage, members);

        // 4. 发布消息发送事件 (异步更新会话等操作)
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

        return messages.stream()
                .map(ChatMessageConvert::objToVo)
                .collect(Collectors.toList());
    }

    @Override
    public boolean markMessageRead(Long roomId, Long lastReadMessageId, Long userId) {
        ThrowUtils.throwIf(roomId == null || lastReadMessageId == null || userId == null, ErrorCode.PARAMS_ERROR);
        long memberCount = chatRoomMemberService.count(
                new LambdaQueryWrapper<ChatRoomMember>()
                        .eq(ChatRoomMember::getRoomId, roomId)
                        .eq(ChatRoomMember::getUserId, userId));
        ThrowUtils.throwIf(memberCount == 0, ErrorCode.NO_AUTH_ERROR, "您不在此聊天室中");

        ChatMessage msg = this.getById(lastReadMessageId);
        ThrowUtils.throwIf(msg == null, ErrorCode.NOT_FOUND_ERROR, "消息不存在");
        if (msg == null) return false;
        if (roomId == null) return false;
        ThrowUtils.throwIf(!roomId.equals(msg.getRoomId()), ErrorCode.PARAMS_ERROR, "消息不属于该房间");

        ChatRoomMember member = chatRoomMemberService.getOne(
                new LambdaQueryWrapper<ChatRoomMember>()
                        .eq(ChatRoomMember::getRoomId, roomId)
                        .eq(ChatRoomMember::getUserId, userId)
                        .last("LIMIT 1"));
        ThrowUtils.throwIf(member == null, ErrorCode.NOT_FOUND_ERROR);
        if (member == null) return false;

        Long oldRead = member.getLastReadMessageId();
        if (oldRead != null && lastReadMessageId != null && lastReadMessageId < oldRead) {
            return true;
        }
        member.setLastReadMessageId(lastReadMessageId);
        return chatRoomMemberService.updateById(member);
    }

    /**
     * 推送消息给房间所有成员
     */
    private void pushMessageToRoomMembers(ChatMessage chatMessage, List<ChatRoomMember> members) {
        if (members == null || members.isEmpty()) {
            return;
        }

        List<Long> memberIds = members.stream()
                .map(ChatRoomMember::getUserId)
                .collect(Collectors.toList());

        // 构建推送消息内容
        ChatMessageVO vo = ChatMessageConvert.objToVo(chatMessage);

        // 包装为 WebSocketMessage 进行集群分发
        chatMqProducer.sendChatMessagePush(memberIds, vo);
    }
}
