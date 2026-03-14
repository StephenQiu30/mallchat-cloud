package com.stephen.cloud.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.api.chat.model.dto.ChatRoomAddRequest;
import com.stephen.cloud.api.chat.model.vo.ChatRoomVO;
import com.stephen.cloud.chat.convert.ChatConvert;
import com.stephen.cloud.chat.mapper.ChatRoomMapper;
import com.stephen.cloud.chat.model.entity.ChatRoom;
import com.stephen.cloud.chat.model.entity.ChatRoomMember;
import com.stephen.cloud.chat.service.ChatRoomMemberService;
import com.stephen.cloud.chat.service.ChatRoomService;
import com.stephen.cloud.common.auth.utils.SecurityUtils;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天室服务实现类
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class ChatRoomServiceImpl extends ServiceImpl<ChatRoomMapper, ChatRoom>
    implements ChatRoomService {

    @Resource
    private ChatRoomMemberService chatRoomMemberService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addChatRoom(ChatRoomAddRequest chatRoomAddRequest) {
        log.info("创建聊天室: {}", chatRoomAddRequest);
        ThrowUtils.throwIf(chatRoomAddRequest == null, ErrorCode.PARAMS_ERROR);
        
        ChatRoom chatRoom = ChatConvert.requestToChatRoom(chatRoomAddRequest);
        
        boolean result = this.save(chatRoom);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建聊天室失败");
        
        // 创建者自动加入房间
        Long userId = SecurityUtils.getLoginUserId();
        joinChatRoom(chatRoom.getId(), userId);
        
        return chatRoom.getId();
    }

    @Override
    public List<ChatRoomVO> listUserChatRooms(Long userId) {
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR);
        
        List<ChatRoomMember> members = chatRoomMemberService.list(
                new LambdaQueryWrapper<ChatRoomMember>()
                        .eq(ChatRoomMember::getUserId, userId)
        );
        
        if (members.isEmpty()) {
            return List.of();
        }
        
        List<Long> roomIds = members.stream().map(ChatRoomMember::getRoomId).collect(Collectors.toList());
        List<ChatRoom> rooms = this.listByIds(roomIds);
        
        return rooms.stream()
                .map(ChatConvert::objToVo)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void joinChatRoom(Long roomId, Long userId) {
        log.info("用户加入聊天室: roomId={}, userId={}", roomId, userId);
        ThrowUtils.throwIf(roomId == null || userId == null, ErrorCode.PARAMS_ERROR);
        
        // 检查房间是否存在
        ChatRoom chatRoom = this.getById(roomId);
        ThrowUtils.throwIf(chatRoom == null, ErrorCode.NOT_FOUND_ERROR, "聊天室不存在");
        
        // 检查是否已经在房间中
        long count = chatRoomMemberService.count(
                new LambdaQueryWrapper<ChatRoomMember>()
                        .eq(ChatRoomMember::getRoomId, roomId)
                        .eq(ChatRoomMember::getUserId, userId)
        );
        if (count > 0) {
            log.info("用户已在房间中: userId={}, roomId={}", userId, roomId);
            return;
        }
        
        ChatRoomMember member = new ChatRoomMember();
        member.setRoomId(roomId);
        member.setUserId(userId);
        member.setRole(1); // 普通成员
        
        boolean result = chatRoomMemberService.save(member);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "加入聊天室失败");
    }
}
