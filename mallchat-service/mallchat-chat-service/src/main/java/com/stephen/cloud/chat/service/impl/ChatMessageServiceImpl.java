package com.stephen.cloud.chat.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.api.chat.model.enums.ChatMessageTypeEnum;
import com.stephen.cloud.api.chat.model.enums.ChatRoomTypeEnum;
import com.stephen.cloud.api.chat.model.enums.MessageStatusEnum;
import com.stephen.cloud.api.chat.model.vo.ChatMessageVO;
import com.stephen.cloud.api.chat.model.vo.ChatSessionVO;
import com.stephen.cloud.api.chat.model.vo.ReplyMsgVO;
import com.stephen.cloud.api.user.client.UserFeignClient;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.chat.convert.ChatMessageConvert;
import com.stephen.cloud.chat.event.ChatMessageSentEvent;
import com.stephen.cloud.chat.mapper.ChatMessageMapper;
import com.stephen.cloud.chat.model.entity.ChatMessage;
import com.stephen.cloud.chat.model.entity.ChatPrivateRoom;
import com.stephen.cloud.chat.model.entity.ChatRoom;
import com.stephen.cloud.chat.model.entity.ChatRoomMember;
import com.stephen.cloud.chat.mq.producer.ChatMqProducer;
import com.stephen.cloud.chat.service.ChatMessageService;
import com.stephen.cloud.chat.service.ChatPrivateRoomService;
import com.stephen.cloud.chat.service.ChatRoomMemberService;
import com.stephen.cloud.chat.service.ChatRoomService;
import com.stephen.cloud.chat.service.ChatSessionService;
import com.stephen.cloud.chat.service.UserFriendService;
import com.stephen.cloud.chat.support.ChatMessageHelper;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    @Resource
    private UserFeignClient userFeignClient;

    @Override
    public void validChatMessage(ChatMessage chatMessage) {
        ThrowUtils.throwIf(chatMessage == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(chatMessage.getRoomId() == null, ErrorCode.PARAMS_ERROR, "房间号不能为空");
        ChatMessageHelper.validate(chatMessage);
    }

    @Override
    public ChatMessageVO getChatMessageVO(ChatMessage chatMessage, HttpServletRequest request) {
        if (chatMessage == null) {
            return null;
        }
        List<ChatMessageVO> chatMessageVOList = getChatMessageVO(Collections.singletonList(chatMessage), request);
        return chatMessageVOList.isEmpty() ? null : chatMessageVOList.get(0);
    }

    @Override
    public List<ChatMessageVO> getChatMessageVO(List<ChatMessage> chatMessageList, HttpServletRequest request) {
        if (CollUtil.isEmpty(chatMessageList)) {
            return Collections.emptyList();
        }

        List<ChatMessageVO> voList = ChatMessageConvert.getChatMessageVO(chatMessageList);

        Map<Long, ChatMessage> replyMsgMap = loadReplyMessageMap(chatMessageList);
        Map<Long, UserVO> senderMap = loadSenderMap(chatMessageList);
        Map<Long, ChatMessage> chatMessageMap = chatMessageList.stream()
                .collect(Collectors.toMap(ChatMessage::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        voList.forEach(vo -> {
            ChatMessage message = chatMessageMap.get(vo.getId());
            if (message == null) {
                return;
            }

            if (Objects.equals(message.getStatus(), MessageStatusEnum.RECALL.getCode())) {
                vo.setContent("该消息已被撤回");
            }
            if (message.getReplyMsgId() != null) {
                vo.setReplyMsg(buildReplyMsgVO(replyMsgMap.get(message.getReplyMsgId())));
            }

            UserVO sender = senderMap.get(message.getFromUserId());
            if (sender != null) {
                vo.setFromUserName(sender.getUserName());
                vo.setFromUserAvatar(sender.getUserAvatar());
            }
        });
        return voList;
    }

    @Override
    public Page<ChatMessageVO> getChatMessageVOPage(Page<ChatMessage> chatMessagePage, HttpServletRequest request) {
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
    public ChatMessageVO sendMessage(ChatMessage chatMessage, Long userId) {
        ThrowUtils.throwIf(chatMessage == null || userId == null, ErrorCode.PARAMS_ERROR);
        validChatMessage(chatMessage);
        log.info("[ChatMessageServiceImpl] 用户发送消息: userId={}, roomId={}, clientMsgId={}",
                userId, chatMessage.getRoomId(), chatMessage.getClientMsgId());

        ChatMessage existing = this.getOne(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getFromUserId, userId)
                .eq(ChatMessage::getClientMsgId, chatMessage.getClientMsgId())
                .last("LIMIT 1"));
        if (existing != null) {
            return getChatMessageVO(existing, null);
        }

        Long roomId = chatMessage.getRoomId();
        ChatRoom chatRoom = chatRoomService.getById(roomId);
        ThrowUtils.throwIf(chatRoom == null, ErrorCode.NOT_FOUND_ERROR, "聊天室不存在");
        validateSendPermission(chatRoom, roomId, userId);

        if (chatMessage.getReplyMsgId() != null) {
            ChatMessage replyMsg = this.getById(chatMessage.getReplyMsgId());
            ThrowUtils.throwIf(replyMsg == null || !Objects.equals(replyMsg.getRoomId(), roomId), ErrorCode.PARAMS_ERROR, "回复消息非法");
        }

        chatMessage.setFromUserId(userId);
        chatMessage.setContent(ChatMessageHelper.normalizeStoredContent(chatMessage.getType(), chatMessage.getContent()));
        chatMessage.setStatus(MessageStatusEnum.NORMAL.getCode());
        boolean result = this.save(chatMessage);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "发送消息失败");

        ChatMessageVO messageVO = getChatMessageVO(chatMessage, null);
        chatMqProducer.sendChatMessageGroupPush(roomId, messageVO);
        eventPublisher.publishEvent(new ChatMessageSentEvent(this, chatMessage, userId));
        return messageVO;
    }

    @Override
    public List<ChatMessageVO> listHistoryMessages(Long roomId, Long lastMessageId, Integer limit, Long userId) {
        ThrowUtils.throwIf(roomId == null || userId == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(!chatRoomMemberService.isMember(roomId, userId), ErrorCode.NO_AUTH_ERROR, "您不在此聊天室中");
        if (limit == null || limit <= 0) {
            limit = 20;
        }

        LambdaQueryWrapper<ChatMessage> queryWrapper = new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getRoomId, roomId);
        if (lastMessageId != null && lastMessageId > 0) {
            queryWrapper.lt(ChatMessage::getId, lastMessageId);
        }

        List<ChatMessage> messages = this.list(queryWrapper.orderByDesc(ChatMessage::getId).last("limit " + limit));
        Collections.reverse(messages);
        return getChatMessageVO(messages, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markMessageRead(Long roomId, Long lastReadMessageId, Long userId) {
        ThrowUtils.throwIf(roomId == null || lastReadMessageId == null || userId == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(!chatRoomMemberService.isMember(roomId, userId), ErrorCode.NO_AUTH_ERROR, "您不在此聊天室中");

        ChatMessage msg = this.getById(lastReadMessageId);
        ThrowUtils.throwIf(msg == null, ErrorCode.NOT_FOUND_ERROR, "消息不存在");
        ThrowUtils.throwIf(!roomId.equals(msg.getRoomId()), ErrorCode.PARAMS_ERROR, "消息不属于该房间");

        ChatRoomMember member = chatRoomMemberService.getMember(roomId, userId);
        if (member == null) {
            return false;
        }

        Long oldRead = member.getLastReadMessageId();
        if (oldRead != null && lastReadMessageId <= oldRead) {
            return true;
        }
        member.setLastReadMessageId(lastReadMessageId);
        chatRoomMemberService.updateById(member);

        long unreadCount = this.count(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getRoomId, roomId)
                .gt(ChatMessage::getId, lastReadMessageId)
                .ne(ChatMessage::getFromUserId, userId));
        chatSessionService.update(new UpdateWrapper<com.stephen.cloud.chat.model.entity.ChatSession>()
                .set("unread_count", (int) unreadCount)
                .set("last_read_message_id", lastReadMessageId)
                .eq("user_id", userId)
                .eq("room_id", roomId));

        Map<String, Object> readPayload = Map.of(
                "roomId", roomId,
                "userId", userId,
                "lastReadMessageId", lastReadMessageId
        );
        chatMqProducer.sendMessageRead(roomId, readPayload, "chat_read:" + roomId + ":" + userId + ":" + lastReadMessageId);

        ChatSessionVO sessionVO = chatSessionService.getSessionVO(roomId, userId);
        if (sessionVO != null) {
            chatMqProducer.sendSessionUpdate(userId, roomId, sessionVO,
                    "session_update:" + roomId + ":" + userId + ":" + lastReadMessageId);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean recallMessage(Long messageId, Long userId) {
        log.info("[ChatMessageServiceImpl] 撤回消息请求, messageId: {}, userId: {}", messageId, userId);

        ChatMessage msg = this.getById(messageId);
        ThrowUtils.throwIf(msg == null, ErrorCode.NOT_FOUND_ERROR, "消息不存在");
        ThrowUtils.throwIf(!Objects.equals(msg.getFromUserId(), userId), ErrorCode.NO_AUTH_ERROR, "只能撤回自己的消息");

        long now = System.currentTimeMillis();
        long createTime = msg.getCreateTime().getTime();
        if (now - createTime > 2 * 60 * 1000) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "消息发送超过 2 分钟，无法撤回");
        }

        if (Objects.equals(msg.getStatus(), MessageStatusEnum.RECALL.getCode())) {
            return true;
        }
        msg.setStatus(MessageStatusEnum.RECALL.getCode());
        boolean ok = this.updateById(msg);
        if (!ok) {
            return false;
        }

        ChatMessageVO messageVO = getChatMessageVO(msg, null);
        chatMqProducer.sendMessageRecall(msg.getRoomId(), messageVO);

        List<ChatRoomMember> members = chatRoomMemberService.listByRoomId(msg.getRoomId());
        for (ChatRoomMember member : members) {
            ChatSessionVO sessionVO = chatSessionService.getSessionVO(msg.getRoomId(), member.getUserId());
            if (sessionVO != null) {
                chatMqProducer.sendSessionUpdate(member.getUserId(), msg.getRoomId(), sessionVO,
                        "session_recall:" + msg.getRoomId() + ":" + member.getUserId() + ":" + messageId);
            }
        }
        return true;
    }

    private void validateSendPermission(ChatRoom chatRoom, Long roomId, Long userId) {
        if (ChatRoomTypeEnum.PRIVATE.getCode().equals(chatRoom.getType())) {
            ChatPrivateRoom privateRoom = chatPrivateRoomService.getOne(new LambdaQueryWrapper<ChatPrivateRoom>()
                    .eq(ChatPrivateRoom::getRoomId, roomId)
                    .last("LIMIT 1"));
            ThrowUtils.throwIf(privateRoom == null, ErrorCode.SYSTEM_ERROR, "私聊房间映射不存在");

            Long peerUserId = Objects.equals(userId, privateRoom.getUserLow()) ? privateRoom.getUserHigh() : privateRoom.getUserLow();
            ThrowUtils.throwIf(!userFriendService.isMutualFriend(userId, peerUserId), ErrorCode.NO_AUTH_ERROR, "非好友无法发送消息");
            return;
        }
        ThrowUtils.throwIf(!chatRoomMemberService.isMember(roomId, userId), ErrorCode.NO_AUTH_ERROR, "您不在此聊天室中");
    }

    private Map<Long, ChatMessage> loadReplyMessageMap(List<ChatMessage> chatMessageList) {
        List<Long> replyIds = chatMessageList.stream()
                .map(ChatMessage::getReplyMsgId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (CollUtil.isEmpty(replyIds)) {
            return Collections.emptyMap();
        }
        return this.listByIds(replyIds).stream()
                .collect(Collectors.toMap(ChatMessage::getId, item -> item, (left, right) -> left));
    }

    private Map<Long, UserVO> loadSenderMap(List<ChatMessage> chatMessageList) {
        List<Long> senderIds = chatMessageList.stream()
                .map(ChatMessage::getFromUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (CollUtil.isEmpty(senderIds)) {
            return Collections.emptyMap();
        }
        try {
            List<UserVO> userVOList = userFeignClient.getUserVOByIds(new ArrayList<>(senderIds)).getData();
            if (CollUtil.isEmpty(userVOList)) {
                return Collections.emptyMap();
            }
            return userVOList.stream().collect(Collectors.toMap(UserVO::getId, item -> item, (left, right) -> left));
        } catch (Exception e) {
            log.error("[ChatMessageServiceImpl] 批量查询发送者信息失败", e);
            return Collections.emptyMap();
        }
    }

    private ReplyMsgVO buildReplyMsgVO(ChatMessage replyMsg) {
        if (replyMsg == null) {
            return null;
        }
        String content = Objects.equals(replyMsg.getStatus(), MessageStatusEnum.RECALL.getCode())
                ? "该消息已被撤回"
                : ChatMessageHelper.buildPreview(replyMsg.getType(), replyMsg.getContent());
        return ReplyMsgVO.builder()
                .id(replyMsg.getId())
                .content(content)
                .type(replyMsg.getType())
                .build();
    }
}
