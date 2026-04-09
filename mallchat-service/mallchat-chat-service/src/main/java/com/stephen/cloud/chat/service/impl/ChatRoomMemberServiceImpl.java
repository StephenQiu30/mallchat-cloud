package com.stephen.cloud.chat.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.api.chat.model.vo.ChatRoomMemberVO;
import com.stephen.cloud.api.user.client.UserFeignClient;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.chat.convert.ChatRoomMemberConvert;
import com.stephen.cloud.chat.mapper.ChatRoomMemberMapper;
import com.stephen.cloud.chat.model.entity.ChatRoomMember;
import com.stephen.cloud.chat.service.ChatRoomMemberService;
import com.stephen.cloud.common.cache.utils.CacheUtils;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.exception.BusinessException;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatRoomMemberServiceImpl extends ServiceImpl<ChatRoomMemberMapper, ChatRoomMember>
        implements ChatRoomMemberService {

    @Resource
    private CacheUtils cacheUtils;

    @Resource
    private UserFeignClient userFeignClient;

    private static final String ROOM_MEMBER_CACHE_KEY = "chat:room:members:";

    /**
     * 校验房间成员
     *
     * @param chatRoomMember 房间成员
     */
    @Override
    public void validChatRoomMember(ChatRoomMember chatRoomMember) {
        if (chatRoomMember == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 核心权限或逻辑校验
        ThrowUtils.throwIf(chatRoomMember.getRoomId() == null, ErrorCode.PARAMS_ERROR, "房间 ID 不能为空");
        ThrowUtils.throwIf(chatRoomMember.getUserId() == null, ErrorCode.PARAMS_ERROR, "用户 ID 不能为空");
    }

    @Override
    public boolean isMember(Long roomId, Long userId) {
        if (roomId == null || userId == null) return false;
        String key = ROOM_MEMBER_CACHE_KEY + roomId;

        boolean isMember = cacheUtils.sIsMember(key, String.valueOf(userId));
        if (isMember) {
            return true;
        }

        // if not in cache, load to cache and check again
        if (!cacheUtils.exists(key)) {
            loadRoomMembersToCache(roomId);
            return cacheUtils.sIsMember(key, String.valueOf(userId));
        }

        return false;
    }

    @Override
    public List<ChatRoomMember> listByRoomId(Long roomId) {
        return this.list(new LambdaQueryWrapper<ChatRoomMember>()
                .eq(ChatRoomMember::getRoomId, roomId));
    }

    /**
     * 添加成员（带幂等检查与缓存同步）
     *
     * @param roomId 房间 ID
     * @param userId 用户 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMember(Long roomId, Long userId) {
        log.info("用户加入聊天室: roomId={}, userId={}", roomId, userId);
        ThrowUtils.throwIf(roomId == null || userId == null, ErrorCode.PARAMS_ERROR);

        // 幂等校验与保存逻辑
        ChatRoomMember existing = this.getOne(new LambdaQueryWrapper<ChatRoomMember>()
                .eq(ChatRoomMember::getRoomId, roomId)
                .eq(ChatRoomMember::getUserId, userId));
        if (existing != null) {
            log.info("用户已在房间中: userId={}, roomId={}", userId, roomId);
            return;
        }

        ChatRoomMember member = new ChatRoomMember();
        member.setRoomId(roomId);
        member.setUserId(userId);
        member.setRole(1); // 1-普通成员
        boolean result = this.save(member);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "加入聊天室失败");

        // 同步清除或更新缓存
        String key = ROOM_MEMBER_CACHE_KEY + roomId;
        cacheUtils.sAdd(key, String.valueOf(userId));
    }

    @Override
    public ChatRoomMemberVO getChatRoomMemberVO(ChatRoomMember chatRoomMember, HttpServletRequest request) {
        if (chatRoomMember == null) {
            return null;
        }
        List<ChatRoomMemberVO> vos = getChatRoomMemberVO(Collections.singletonList(chatRoomMember), request);
        return CollUtil.getFirst(vos);
    }

    @Override
    public List<ChatRoomMemberVO> getChatRoomMemberVO(List<ChatRoomMember> chatRoomMemberList, HttpServletRequest request) {
        if (CollUtil.isEmpty(chatRoomMemberList)) {
            return Collections.emptyList();
        }

        // 1. 批量转换基础属性
        List<ChatRoomMemberVO> voList = ChatRoomMemberConvert.getChatRoomMemberVO(chatRoomMemberList);

        // 2. 批量关联用户信息
        List<Long> userIds = chatRoomMemberList.stream().map(ChatRoomMember::getUserId).distinct().collect(Collectors.toList());
        try {
            List<UserVO> userVOs = userFeignClient.getUserVOByIds(userIds).getData();
            if (CollUtil.isNotEmpty(userVOs)) {
                Map<Long, UserVO> userMap = userVOs.stream().collect(Collectors.toMap(UserVO::getId, u -> u));
                voList.forEach(vo -> {
                    UserVO userVO = userMap.get(vo.getUserId());
                    if (userVO != null) {
                        vo.setUserName(userVO.getUserName());
                        vo.setUserAvatar(userVO.getUserAvatar());
                    }
                });
            }
        } catch (Exception e) {
            log.error("[ChatRoomMemberServiceImpl] 批量获取用户信息失败", e);
        }

        return voList;
    }

    @Override
    public Page<ChatRoomMemberVO> getChatRoomMemberVOPage(Page<ChatRoomMember> chatRoomMemberPage, HttpServletRequest request) {
        if (chatRoomMemberPage == null) {
            return new Page<>();
        }
        Page<ChatRoomMemberVO> resultPage = new Page<>(chatRoomMemberPage.getCurrent(), chatRoomMemberPage.getSize(), chatRoomMemberPage.getTotal());
        resultPage.setRecords(getChatRoomMemberVO(chatRoomMemberPage.getRecords(), request));
        return resultPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveRoom(Long roomId, Long userId) {
        if (roomId == null || userId == null) return;

        // 1. 从数据库删除
        this.remove(new LambdaQueryWrapper<ChatRoomMember>()
                .eq(ChatRoomMember::getRoomId, roomId)
                .eq(ChatRoomMember::getUserId, userId));

        // 2. 从 Redis 缓存删除
        String key = ROOM_MEMBER_CACHE_KEY + roomId;
        cacheUtils.sRemove(key, String.valueOf(userId));
    }

    private void loadRoomMembersToCache(Long roomId) {
        List<ChatRoomMember> members = this.list(new LambdaQueryWrapper<ChatRoomMember>()
                .eq(ChatRoomMember::getRoomId, roomId));
        if (members == null || members.isEmpty()) return;

        String key = ROOM_MEMBER_CACHE_KEY + roomId;
        Set<String> userIdStrs = members.stream()
                .map(m -> String.valueOf(m.getUserId()))
                .collect(Collectors.toSet());
        cacheUtils.sAddAll(key, userIdStrs);
        // Set an expiry for member list to avoid stale data issues
        cacheUtils.expire(key, 86400); // 1 day
    }
}
