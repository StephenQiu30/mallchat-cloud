package com.stephen.cloud.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.api.chat.model.dto.ChatMessageSendRequest;
import com.stephen.cloud.api.chat.model.vo.ChatMessageVO;
import com.stephen.cloud.chat.convert.ChatConvert;
import com.stephen.cloud.chat.mapper.ChatMessageMapper;
import com.stephen.cloud.chat.model.entity.ChatMessage;
import com.stephen.cloud.chat.model.entity.ChatRoomMember;
import com.stephen.cloud.chat.service.ChatMessageService;
import com.stephen.cloud.chat.service.ChatRoomMemberService;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.WebSocketMessage;
import com.stephen.cloud.common.rabbitmq.producer.RabbitMqSender;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private RabbitMqSender mqSender;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long sendMessage(ChatMessageSendRequest chatMessageSendRequest, Long userId) {
        log.info("用户发送消息: userId={}, roomId={}", userId, chatMessageSendRequest != null ? chatMessageSendRequest.getRoomId() : null);
        ThrowUtils.throwIf(chatMessageSendRequest == null || userId == null, ErrorCode.PARAMS_ERROR);
        
        Long roomId = chatMessageSendRequest.getRoomId();
        
        // 1. 校验用户是否在房间中
        long count = chatRoomMemberService.count(
                new LambdaQueryWrapper<ChatRoomMember>()
                        .eq(ChatRoomMember::getRoomId, roomId)
                        .eq(ChatRoomMember::getUserId, userId)
        );
        ThrowUtils.throwIf(count == 0, ErrorCode.NO_AUTH_ERROR, "您不在此聊天室中");
        
        // 2. 保存消息到数据库
        ChatMessage chatMessage = ChatConvert.requestToChatMessage(chatMessageSendRequest);
        chatMessage.setFromUserId(userId);
        boolean result = this.save(chatMessage);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "发送消息失败");
        
        // 3. 异步推送消息到所有房间成员 (RabbitMQ)
        pushMessageToRoomMembers(chatMessage);
        
        return chatMessage.getId();
    }

    @Override
    public List<ChatMessageVO> listHistoryMessages(Long roomId, Long lastMessageId, Integer limit) {
        ThrowUtils.throwIf(roomId == null, ErrorCode.PARAMS_ERROR);
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
                .map(ChatConvert::objToVo)
                .collect(Collectors.toList());
    }

    /**
     * 推送消息给房间所有成员
     */
    private void pushMessageToRoomMembers(ChatMessage chatMessage) {
        List<ChatRoomMember> members = chatRoomMemberService.list(
                new LambdaQueryWrapper<ChatRoomMember>()
                        .eq(ChatRoomMember::getRoomId, chatMessage.getRoomId())
        );
        
        List<Long> memberIds = members.stream()
                .map(ChatRoomMember::getUserId)
                .collect(Collectors.toList());
        
        // 构建推送消息内容
        ChatMessageVO vo = ChatConvert.objToVo(chatMessage);
        
        // 包装为 WebSocketMessage 进行集群分发
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .userIds(memberIds)
                .data(vo)
                .build();
        
        String bizId = "chat_msg:" + chatMessage.getId();
        mqSender.send(MqBizTypeEnum.CHAT_MESSAGE_PUSH, bizId, wsMessage);
        log.info("[ChatMessageServiceImpl] 聊天消息已发送到 MQ, id: {}, roomId: {}", chatMessage.getId(), chatMessage.getRoomId());
    }
}
