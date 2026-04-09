package com.stephen.cloud.chat.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.api.chat.model.dto.ChatFriendQueryRequest;
import com.stephen.cloud.api.chat.model.vo.ChatFriendUserVO;
import com.stephen.cloud.api.user.client.UserFeignClient;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.chat.convert.ChatFriendConvert;
import com.stephen.cloud.chat.mapper.UserFriendMapper;
import com.stephen.cloud.chat.model.entity.UserFriend;
import com.stephen.cloud.chat.service.UserFriendService;
import com.stephen.cloud.common.cache.utils.CacheUtils;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.constants.CommonConstant;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.mysql.utils.SqlUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户好友服务实现
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class UserFriendServiceImpl extends ServiceImpl<UserFriendMapper, UserFriend>
        implements UserFriendService {

    @Resource
    private UserFeignClient userFeignClient;

    @Resource
    private CacheUtils cacheUtils;

    private static final String USER_FRIEND_CACHE_KEY = "chat:user:friends:";

    /**
     * 校验好友数据
     *
     * @param userFriend 好友实体
     */
    @Override
    public void validUserFriend(UserFriend userFriend) {
        if (userFriend == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 核心权限或逻辑校验
        Long userId = userFriend.getUserId();
        Long friendUserId = userFriend.getFriendUserId();
        ThrowUtils.throwIf(Objects.equals(userId, friendUserId), ErrorCode.PARAMS_ERROR, "不能添加自己为好友");
    }

    /**
     * 获取查询条件
     *
     * @param chatFriendQueryRequest 查询请求
     * @return {@link LambdaQueryWrapper<UserFriend>}
     */
    @Override
    public LambdaQueryWrapper<UserFriend> getQueryWrapper(ChatFriendQueryRequest chatFriendQueryRequest) {
        LambdaQueryWrapper<UserFriend> queryWrapper = new LambdaQueryWrapper<>();
        if (chatFriendQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long userId = chatFriendQueryRequest.getUserId();
        Long friendUserId = chatFriendQueryRequest.getFriendUserId();
        String sortField = chatFriendQueryRequest.getSortField();
        String sortOrder = chatFriendQueryRequest.getSortOrder();

        // 补充查询条件
        queryWrapper.eq(userId != null, UserFriend::getUserId, userId);
        queryWrapper.eq(friendUserId != null, UserFriend::getFriendUserId, friendUserId);
        // 排序规则
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                CommonConstant.SORT_ORDER_ASC.equals(sortOrder),
                UserFriend::getCreateTime);
        return queryWrapper;
    }

    /**
     * 获取记录视图类
     *
     * @param userFriend 好友实体
     * @param request    请求
     * @return {@link ChatFriendUserVO}
     */
    @Override
    public ChatFriendUserVO getUserFriendVO(UserFriend userFriend, HttpServletRequest request) {
        if (userFriend == null) {
            return null;
        }
        ChatFriendUserVO chatFriendUserVO = ChatFriendConvert.objToVo(userFriend);
        // 关联查询用户信息
        Long friendUserId = userFriend.getFriendUserId();
        if (friendUserId != null && friendUserId > 0) {
            UserVO userVO = userFeignClient.getUserVOById(friendUserId).getData();
            if (userVO != null) {
                chatFriendUserVO.setUserName(userVO.getUserName());
                chatFriendUserVO.setUserAvatar(userVO.getUserAvatar());
            }
        }
        return chatFriendUserVO;
    }

    /**
     * 批量获取记录视图类
     *
     * @param userFriendList 好友实体列表
     * @param request        请求
     * @return {@link List<ChatFriendUserVO>}
     */
    @Override
    public List<ChatFriendUserVO> getUserFriendVO(List<UserFriend> userFriendList, HttpServletRequest request) {
        if (CollUtil.isEmpty(userFriendList)) {
            return Collections.emptyList();
        }
        // 批量获取用户信息
        List<Long> friendUserIdList = userFriendList.stream().map(UserFriend::getFriendUserId).collect(Collectors.toList());
        Map<Long, List<UserVO>> userIdUserVOListMap = userFeignClient.getUserVOByIds(friendUserIdList).getData().stream()
                .collect(Collectors.groupingBy(UserVO::getId));
        // 填充信息
        return userFriendList.stream().map(userFriend -> {
            ChatFriendUserVO chatFriendUserVO = ChatFriendConvert.objToVo(userFriend);
            Long friendUserId = userFriend.getFriendUserId();
            UserVO userVO = null;
            if (userIdUserVOListMap.containsKey(friendUserId)) {
                userVO = userIdUserVOListMap.get(friendUserId).get(0);
            }
            if (userVO != null) {
                chatFriendUserVO.setUserName(userVO.getUserName());
                chatFriendUserVO.setUserAvatar(userVO.getUserAvatar());
            }
            return chatFriendUserVO;
        }).collect(Collectors.toList());
    }

    /**
     * 分页获取记录视图类
     *
     * @param userFriendPage 好友分页
     * @param request        请求
     * @return {@link Page<ChatFriendUserVO>}
     */
    @Override
    public Page<ChatFriendUserVO> getUserFriendVOPage(Page<UserFriend> userFriendPage, HttpServletRequest request) {
        List<UserFriend> records = userFriendPage.getRecords();
        Page<ChatFriendUserVO> voPage = new Page<>(userFriendPage.getCurrent(), userFriendPage.getSize(), userFriendPage.getTotal());
        if (CollUtil.isEmpty(records)) {
            return voPage;
        }
        voPage.setRecords(getUserFriendVO(records, request));
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addFriend(Long userId, Long friendUserId) {
        log.info("添加好友: userId={}, friendUserId={}", userId, friendUserId);
        ThrowUtils.throwIf(userId == null || friendUserId == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(userId.equals(friendUserId), ErrorCode.PARAMS_ERROR, "不能添加自己为好友");

        // 校验对方用户是否存在
        UserVO friendUserVO = userFeignClient.getUserVOById(friendUserId).getData();
        ThrowUtils.throwIf(friendUserVO == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");

        long exists = this.count(new LambdaQueryWrapper<UserFriend>()
                .eq(UserFriend::getUserId, userId)
                .eq(UserFriend::getFriendUserId, friendUserId));
        if (exists > 0) {
            // Ensure cache is in sync even if it's already in DB
            syncFriendToCache(userId, friendUserId);
            syncFriendToCache(friendUserId, userId);
            return;
        }

        UserFriend a = new UserFriend();
        a.setUserId(userId);
        a.setFriendUserId(friendUserId);
        UserFriend b = new UserFriend();
        b.setUserId(friendUserId);
        b.setFriendUserId(userId);
        boolean ok = this.save(a) && this.save(b);
        ThrowUtils.throwIf(!ok, ErrorCode.OPERATION_ERROR, "添加好友失败");

        // Sync to Redis
        syncFriendToCache(userId, friendUserId);
        syncFriendToCache(friendUserId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeFriend(Long userId, Long friendUserId) {
        log.info("移除好友: userId={}, friendUserId={}", userId, friendUserId);
        ThrowUtils.throwIf(userId == null || friendUserId == null, ErrorCode.PARAMS_ERROR);

        boolean ok = this.remove(new LambdaQueryWrapper<UserFriend>()
                .and(wrapper -> wrapper.eq(UserFriend::getUserId, userId).eq(UserFriend::getFriendUserId, friendUserId)
                        .or()
                        .eq(UserFriend::getUserId, friendUserId).eq(UserFriend::getFriendUserId, userId)));

        if (ok) {
            // Remove from Redis
            cacheUtils.sRemove(USER_FRIEND_CACHE_KEY + userId, String.valueOf(friendUserId));
            cacheUtils.sRemove(USER_FRIEND_CACHE_KEY + friendUserId, String.valueOf(userId));
        }
    }

    @Override
    public List<ChatFriendUserVO> listFriends(Long userId) {
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR);
        List<UserFriend> rows = this.list(new LambdaQueryWrapper<UserFriend>()
                .eq(UserFriend::getUserId, userId)
                .orderByDesc(UserFriend::getCreateTime));
        return getUserFriendVO(rows, null);
    }

    @Override
    public boolean isMutualFriend(Long userId, Long friendUserId) {
        if (userId == null || friendUserId == null) {
            return false;
        }
        String key = USER_FRIEND_CACHE_KEY + userId;
        // Try Cache first
        if (cacheUtils.exists(key)) {
            return cacheUtils.sIsMember(key, String.valueOf(friendUserId));
        }

        // Fallback to DB and load cache
        loadFriendCache(userId);
        return cacheUtils.sIsMember(key, String.valueOf(friendUserId));
    }

    private void syncFriendToCache(Long userId, Long friendUserId) {
        String key = USER_FRIEND_CACHE_KEY + userId;
        cacheUtils.sAdd(key, String.valueOf(friendUserId));
        cacheUtils.expire(key, 86400 * 7); // 7 days
    }

    private void loadFriendCache(Long userId) {
        List<UserFriend> friends = this.list(new LambdaQueryWrapper<UserFriend>()
                .eq(UserFriend::getUserId, userId));
        String key = USER_FRIEND_CACHE_KEY + userId;
        if (CollUtil.isNotEmpty(friends)) {
            Set<String> friendIds = friends.stream()
                    .map(f -> String.valueOf(f.getFriendUserId()))
                    .collect(Collectors.toSet());
            cacheUtils.sAddAll(key, friendIds);
        } else {
            // 空好友列表也需要标识，防止缓存穿透，注意 SET 类型不能用 putString
            cacheUtils.sAdd(key, "EMPTY");
            cacheUtils.expire(key, 60);
        }
        cacheUtils.expire(key, 86400 * 7);
    }
}
