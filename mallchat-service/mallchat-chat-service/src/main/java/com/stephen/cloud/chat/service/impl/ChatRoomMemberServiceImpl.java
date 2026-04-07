package com.stephen.cloud.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.chat.mapper.ChatRoomMemberMapper;
import com.stephen.cloud.chat.model.entity.ChatRoomMember;
import com.stephen.cloud.chat.service.ChatRoomMemberService;
import com.stephen.cloud.common.cache.utils.CacheUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 聊天室成员服务实现
 *
 * @author StephenQiu30
 */
@Service
public class ChatRoomMemberServiceImpl extends ServiceImpl<ChatRoomMemberMapper, ChatRoomMember>
    implements ChatRoomMemberService {

    @Resource
    private CacheUtils cacheUtils;

    private static final String ROOM_MEMBER_CACHE_KEY = "chat:room:members:";

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
