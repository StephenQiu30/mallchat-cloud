package com.stephen.cloud.common.cache.constants;

/**
 * 聊天模块缓存 Key 常量
 *
 * @author StephenQiu30
 */
public interface ChatCacheConstant {

    /**
     * 房间成员缓存 Key 前缀
     */
    String ROOM_MEMBER_CACHE_KEY = "chat:room:members:";

    /**
     * 用户好友缓存 Key 前缀
     */
    String USER_FRIEND_CACHE_KEY = "chat:user:friends:";

    /**
     * 空集合占位符
     */
    String EMPTY_SET_PLACEHOLDER = "EMPTY";

    /**
     * 房间成员缓存过期时间（秒）
     */
    long ROOM_MEMBER_CACHE_EXPIRE_SECONDS = 86400L;

    /**
     * 好友缓存过期时间（秒）
     */
    long USER_FRIEND_CACHE_EXPIRE_SECONDS = 86400L * 7;

    /**
     * 获取房间成员缓存 Key
     *
     * @param roomId 房间 ID
     * @return 缓存 Key
     */
    static String getRoomMemberKey(Long roomId) {
        return ROOM_MEMBER_CACHE_KEY + roomId;
    }

    /**
     * 获取好友缓存 Key
     *
     * @param userId 用户 ID
     * @return 缓存 Key
     */
    static String getUserFriendKey(Long userId) {
        return USER_FRIEND_CACHE_KEY + userId;
    }

    /**
     * 获取好友缓存 Key
     *
     * @param userId 用户 ID
     * @return 缓存 Key
     */
    static String getUserFriendKey(String userId) {
        return USER_FRIEND_CACHE_KEY + userId;
    }
}
