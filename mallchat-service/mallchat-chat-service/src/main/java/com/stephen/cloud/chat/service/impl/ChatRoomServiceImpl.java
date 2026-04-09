package com.stephen.cloud.chat.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.api.chat.model.vo.ChatRoomVO;
import com.stephen.cloud.chat.convert.ChatRoomConvert;
import com.stephen.cloud.chat.mapper.ChatPrivateRoomMapper;
import com.stephen.cloud.chat.mapper.ChatRoomMapper;
import com.stephen.cloud.chat.model.entity.ChatPrivateRoom;
import com.stephen.cloud.chat.model.entity.ChatRoom;
import com.stephen.cloud.chat.model.entity.ChatRoomMember;
import com.stephen.cloud.chat.service.ChatRoomMemberService;
import com.stephen.cloud.chat.service.ChatRoomService;
import com.stephen.cloud.chat.service.ChatSessionService;
import com.stephen.cloud.chat.service.UserFriendService;
import com.stephen.cloud.common.auth.utils.SecurityUtils;
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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 聊天室服务实现类
 * <p>
 * 与通知等业务服务一致：入参校验使用 {@link ThrowUtils}，写操作打日志并配合事务边界。
 * </p>
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
    private ChatPrivateRoomMapper chatPrivateRoomMapper;

    @Lazy
    @Resource
    private ChatSessionService chatSessionService;

    /**
     * 校验数据
     *
     * @param chatRoom chatRoom
     * @param add      对创建的数据进行校验
     */
    @Override
    public void validChatRoom(ChatRoom chatRoom, boolean add) {
        if (chatRoom == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String name = chatRoom.getName();
        Integer type = chatRoom.getType();

        if (add) {
            ThrowUtils.throwIf(StringUtils.isBlank(name), ErrorCode.PARAMS_ERROR, "房间名称不能为空");
            ThrowUtils.throwIf(type == null, ErrorCode.PARAMS_ERROR, "房间类型不能为空");
        }
        if (StringUtils.isNotBlank(name)) {
            ThrowUtils.throwIf(name.length() > 80, ErrorCode.PARAMS_ERROR, "房间名称过长");
        }
    }

    /**
     * 获取当前登录用户 ID
     *
     * @param request request
     * @return Long userId
     */
    @Override
    public Long getLoginUserId(HttpServletRequest request) {
        return SecurityUtils.getLoginUserId();
    }

    /**
     * 获取聊天室 VO 封装类
     *
     * @param chatRoom chatRoom
     * @param request  request
     * @return {@link ChatRoomVO}
     */
    @Override
    public ChatRoomVO getChatRoomVO(ChatRoom chatRoom, HttpServletRequest request) {
        return ChatRoomConvert.objToVo(chatRoom);
    }

    /**
     * 获取聊天室 VO 视图类列表
     *
     * @param chatRoomList 聊天室列表
     * @param request      HTTP 请求
     * @return 聊天室视图类列表
     */
    @Override
    public List<ChatRoomVO> getChatRoomVO(List<ChatRoom> chatRoomList, HttpServletRequest request) {
        if (CollUtil.isEmpty(chatRoomList)) {
            return new ArrayList<>();
        }
        return chatRoomList.stream().map(chatRoom -> getChatRoomVO(chatRoom, request)).collect(Collectors.toList());
    }

    /**
     * 分页获取聊天室视图类
     *
     * @param chatRoomPage 聊天室分页数据
     * @param request      HTTP 请求
     * @return 聊天室视图类分页对象
     */
    @Override
    public Page<ChatRoomVO> getChatRoomVOPage(Page<ChatRoom> chatRoomPage, HttpServletRequest request) {
        List<ChatRoom> chatRoomList = chatRoomPage.getRecords();
        Page<ChatRoomVO> chatRoomVOPage = new Page<>(chatRoomPage.getCurrent(), chatRoomPage.getSize(), chatRoomPage.getTotal());
        if (CollUtil.isEmpty(chatRoomList)) {
            return chatRoomVOPage;
        }
        List<ChatRoomVO> chatRoomVOList = getChatRoomVO(chatRoomList, request);
        chatRoomVOPage.setRecords(chatRoomVOList);
        return chatRoomVOPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addChatRoom(ChatRoom chatRoom) {
        log.info("创建聊天室: {}", chatRoom);
        Long userId = SecurityUtils.getLoginUserId();
        chatRoom.setCreateUser(userId);

        // 1. 持久化聊天室基本信息
        boolean result = this.save(chatRoom);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建聊天室失败");

        // 2. 创建者自动加入房间成为首位成员
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

        return ChatRoomConvert.getChatRoomVO(rooms);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void joinChatRoom(Long roomId, Long userId) {
        log.info("用户加入聊天室: roomId={}, userId={}", roomId, userId);
        ThrowUtils.throwIf(roomId == null || userId == null, ErrorCode.PARAMS_ERROR);

        // 1. 检查目标房间是否存在
        ChatRoom chatRoom = this.getById(roomId);
        ThrowUtils.throwIf(chatRoom == null, ErrorCode.NOT_FOUND_ERROR, "聊天室不存在");

        // 2. 幂等校验：检查用户是否已在该房间成员列表中
        long count = chatRoomMemberService.count(
                new LambdaQueryWrapper<ChatRoomMember>()
                        .eq(ChatRoomMember::getRoomId, roomId)
                        .eq(ChatRoomMember::getUserId, userId)
        );
        if (count > 0) {
            log.info("用户已在房间中: userId={}, roomId={}", userId, roomId);
            return;
        }

        // 3. 构造并保存成员关系记录
        ChatRoomMember member = new ChatRoomMember();
        member.setRoomId(roomId);
        member.setUserId(userId);
        member.setRole(1); // 1-普通成员
        boolean result = chatRoomMemberService.save(member);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "加入聊天室失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long getOrCreatePrivateRoom(Long peerUserId, Long userId) {
        log.info("获取或创建私聊房间: userId={}, peerUserId={}", userId, peerUserId);
        ThrowUtils.throwIf(peerUserId == null || userId == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(Objects.equals(peerUserId, userId), ErrorCode.PARAMS_ERROR, "不能与自己私聊");
        
        // 1. 业务校验：双向好友关系检查
        ThrowUtils.throwIf(!userFriendService.isMutualFriend(userId, peerUserId), ErrorCode.NO_AUTH_ERROR,
                "非好友无法发起私聊");

        // 2. 映射关系标准化：固定低 ID 为 userLow，高 ID 为 userHigh，确保房间唯一性
        long userLow = Math.min(userId, peerUserId);
        long userHigh = Math.max(userId, peerUserId);

        // 3. 检查是否已有私聊映射记录
        ChatPrivateRoom existing = chatPrivateRoomMapper.selectOne(
                new LambdaQueryWrapper<ChatPrivateRoom>()
                        .eq(ChatPrivateRoom::getUserLow, userLow)
                        .eq(ChatPrivateRoom::getUserHigh, userHigh)
                        .last("LIMIT 1"));
        if (existing != null) {
            return existing.getRoomId();
        }

        // 4. 初始化私聊房间逻辑
        // 4.1 创建公共聊天室实体
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setName("私聊");
        chatRoom.setType(2); // 2-私聊模式
        chatRoom.setCreateUser(userId);
        boolean saved = this.save(chatRoom);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "创建私聊房间失败");

        // 4.2 双方入群：维护成员列表
        joinChatRoom(chatRoom.getId(), userId);
        joinChatRoom(chatRoom.getId(), peerUserId);

        // 4.3 维护私聊一对一映射表
        ChatPrivateRoom mapping = new ChatPrivateRoom();
        mapping.setUserLow(userLow);
        mapping.setUserHigh(userHigh);
        mapping.setRoomId(chatRoom.getId());
        chatPrivateRoomMapper.insert(mapping);

        // 5. 会话列表初始化：确保双方好友的会话列表中均出现该房间 (非活跃会话加载)
        chatSessionService.updateSession(userId, chatRoom.getId(), null, false);
        chatSessionService.updateSession(peerUserId, chatRoom.getId(), null, false);

        return chatRoom.getId();
    }
}
