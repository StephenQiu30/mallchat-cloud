package com.stephen.cloud.chat.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.api.chat.model.dto.ChatFriendProfileUpdateRequest;
import com.stephen.cloud.api.chat.model.dto.ChatFriendQueryRequest;
import com.stephen.cloud.api.chat.model.vo.ChatFriendUserVO;
import com.stephen.cloud.api.user.model.dto.UserQueryRequest;
import com.stephen.cloud.api.user.client.UserFeignClient;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.chat.convert.ChatFriendConvert;
import com.stephen.cloud.chat.mapper.UserFriendApplyMapper;
import com.stephen.cloud.chat.mapper.UserFriendBlockMapper;
import com.stephen.cloud.chat.mapper.UserFriendMapper;
import com.stephen.cloud.chat.model.entity.UserFriendApply;
import com.stephen.cloud.chat.model.entity.UserFriendBlock;
import com.stephen.cloud.chat.model.entity.UserFriend;
import com.stephen.cloud.chat.service.ChatOnlineStatusService;
import com.stephen.cloud.chat.service.UserFriendApplyService;
import com.stephen.cloud.chat.service.UserFriendService;
import com.stephen.cloud.common.cache.constants.ChatCacheConstant;
import com.stephen.cloud.common.cache.utils.CacheUtils;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.constants.CommonConstant;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.mysql.utils.SqlUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.apache.commons.lang3.StringUtils;
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

    private static final String DEFAULT_FRIEND_GROUP_NAME = "默认分组";
    private static final int MAX_REMARK_NAME_LENGTH = 64;
    private static final int MAX_FRIEND_GROUP_NAME_LENGTH = 32;

    @Resource
    private UserFeignClient userFeignClient;

    @Resource
    private UserFriendBlockMapper userFriendBlockMapper;

    @Resource
    private CacheUtils cacheUtils;

    @Resource
    private ChatOnlineStatusService chatOnlineStatusService;

    @Resource
    private UserFriendApplyMapper userFriendApplyMapper;

    @Lazy
    @Resource
    private UserFriendApplyService userFriendApplyService;

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
            fillFriendProfile(chatFriendUserVO);
            chatFriendUserVO.setOnlineStatus(chatOnlineStatusService.getOnlineStatus(friendUserId));
            chatFriendUserVO.setFriendStatus(2);
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
        Map<Long, Integer> onlineStatusMap = chatOnlineStatusService.getOnlineStatusMap(friendUserIdList);
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
            fillFriendProfile(chatFriendUserVO);
            chatFriendUserVO.setOnlineStatus(onlineStatusMap.getOrDefault(friendUserId, 0));
            chatFriendUserVO.setFriendStatus(2);
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
        log.info("[UserFriendServiceImpl] 添加好友: userId={}, friendUserId={}", userId, friendUserId);
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
        a.setFriendGroupName(DEFAULT_FRIEND_GROUP_NAME);
        UserFriend b = new UserFriend();
        b.setUserId(friendUserId);
        b.setFriendUserId(userId);
        b.setFriendGroupName(DEFAULT_FRIEND_GROUP_NAME);
        boolean ok = this.save(a) && this.save(b);
        ThrowUtils.throwIf(!ok, ErrorCode.OPERATION_ERROR, "添加好友失败");

        // Sync to Redis
        syncFriendToCache(userId, friendUserId);
        syncFriendToCache(friendUserId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeFriend(Long userId, Long friendUserId) {
        log.info("[UserFriendServiceImpl] 移除好友: userId={}, friendUserId={}", userId, friendUserId);
        ThrowUtils.throwIf(userId == null || friendUserId == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(userId.equals(friendUserId), ErrorCode.PARAMS_ERROR, "不能移除自己");

        boolean ok = this.remove(new LambdaQueryWrapper<UserFriend>()
                .and(wrapper -> wrapper.eq(UserFriend::getUserId, userId).eq(UserFriend::getFriendUserId, friendUserId)
                        .or()
                        .eq(UserFriend::getUserId, friendUserId).eq(UserFriend::getFriendUserId, userId)));

        if (ok) {
            // Remove from Redis
            cacheUtils.sRemove(ChatCacheConstant.getUserFriendKey(userId), String.valueOf(friendUserId));
            cacheUtils.sRemove(ChatCacheConstant.getUserFriendKey(friendUserId), String.valueOf(userId));
        }
        // Always clean up pending applies (defensive, even if remove returned false)
        rejectPendingApplies(userId, friendUserId);
    }

    @Override
    public List<ChatFriendUserVO> listFriends(Long userId) {
        return listFriends(userId, null);
    }

    @Override
    public List<ChatFriendUserVO> listFriends(Long userId, String friendGroupName) {
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR);
        String normalizedGroupName = normalizeFriendGroupNameForQuery(friendGroupName);
        List<UserFriend> rows = this.list(new LambdaQueryWrapper<UserFriend>()
                .eq(UserFriend::getUserId, userId)
                .eq(StringUtils.isNotBlank(normalizedGroupName), UserFriend::getFriendGroupName, normalizedGroupName)
                .orderByDesc(UserFriend::getCreateTime));
        return getUserFriendVO(rows, null);
    }

    @Override
    public void updateFriendProfile(Long userId, ChatFriendProfileUpdateRequest request) {
        ThrowUtils.throwIf(userId == null || request == null || request.getFriendUserId() == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(Objects.equals(userId, request.getFriendUserId()), ErrorCode.PARAMS_ERROR, "不能设置自己为好友");
        String remarkName = normalizeRemarkName(request.getRemarkName());
        String friendGroupName = normalizeFriendGroupName(request.getFriendGroupName());

        UserFriend friend = getFriendByPair(userId, request.getFriendUserId());
        ThrowUtils.throwIf(friend == null, ErrorCode.NO_AUTH_ERROR, "非好友不可设置联系人资料");
        friend.setRemarkName(remarkName);
        friend.setFriendGroupName(friendGroupName);
        ThrowUtils.throwIf(!this.updateById(friend), ErrorCode.OPERATION_ERROR, "更新好友资料失败");
    }

    @Override
    public void blockUser(Long userId, Long targetUserId) {
        ThrowUtils.throwIf(userId == null || targetUserId == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(Objects.equals(userId, targetUserId), ErrorCode.PARAMS_ERROR, "不能拉黑自己");
        ThrowUtils.throwIf(getUserById(targetUserId) == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        if (getBlock(userId, targetUserId) != null) {
            clearFriendCache(userId, targetUserId);
            rejectPendingApplies(userId, targetUserId);
            return;
        }
        UserFriendBlock block = new UserFriendBlock();
        block.setUserId(userId);
        block.setBlockedUserId(targetUserId);
        ThrowUtils.throwIf(!saveBlock(block), ErrorCode.OPERATION_ERROR, "拉黑用户失败");
        clearFriendCache(userId, targetUserId);
        rejectPendingApplies(userId, targetUserId);
    }

    @Override
    public void unblockUser(Long userId, Long targetUserId) {
        ThrowUtils.throwIf(userId == null || targetUserId == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(Objects.equals(userId, targetUserId), ErrorCode.PARAMS_ERROR, "不能解除自己");
        removeBlock(userId, targetUserId);
        clearFriendCache(userId, targetUserId);
        restoreFriendCacheIfMutual(userId, targetUserId);
    }

    @Override
    public boolean isBlockedBetween(Long userId, Long targetUserId) {
        if (userId == null || targetUserId == null) {
            return false;
        }
        return getBlock(userId, targetUserId) != null || getBlock(targetUserId, userId) != null;
    }

    @Override
    public boolean isMutualFriend(Long userId, Long friendUserId) {
        if (userId == null || friendUserId == null) {
            return false;
        }
        if (isBlockedBetween(userId, friendUserId)) {
            return false;
        }
        String key = ChatCacheConstant.getUserFriendKey(userId);
        // Try Cache first
        if (cacheUtils.exists(key)) {
            return cacheUtils.sIsMember(key, String.valueOf(friendUserId));
        }

        // Fallback to DB and load cache
        loadFriendCache(userId);
        return cacheUtils.sIsMember(key, String.valueOf(friendUserId));
    }

    @Override
    public Set<Long> listFriendIdsForNotification(Long userId) {
        return listMutualFriendIds(userId);
    }

    @Override
    public Set<Long> listMutualFriendIds(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        String key = ChatCacheConstant.getUserFriendKey(userId);
        Set<String> friendIds = cacheUtils.sMembers(key);
        if (!cacheUtils.exists(key)) {
            loadFriendCache(userId);
            friendIds = cacheUtils.sMembers(key);
        }
        if (CollUtil.isEmpty(friendIds)) {
            return Collections.emptySet();
        }
        return friendIds.stream()
                .filter(friendId -> friendId != null && !ChatCacheConstant.EMPTY_SET_PLACEHOLDER.equals(friendId))
                .map(Long::valueOf)
                .filter(friendId -> !isBlockedBetween(userId, friendId))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public Integer getFriendshipStatus(Long userId, Long targetUserId) {
        ThrowUtils.throwIf(userId == null || targetUserId == null, ErrorCode.PARAMS_ERROR);
        if (userId.equals(targetUserId)) {
            return 1;
        }
        if (isMutualFriend(userId, targetUserId)) {
            return 2;
        }
        if (hasPendingFriendApply(userId, targetUserId)) {
            return 3;
        }
        if (hasPendingFriendApply(targetUserId, userId)) {
            return 4;
        }
        return 0;
    }

    @Override
    public Page<ChatFriendUserVO> searchFriends(Long userId, String searchText, int current, int pageSize) {
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(pageSize < 1 || pageSize > 20, ErrorCode.PARAMS_ERROR, "分页参数不合法");

        UserQueryRequest userQueryRequest = new UserQueryRequest();
        userQueryRequest.setCurrent(current <= 0 ? 1 : current);
        userQueryRequest.setPageSize(pageSize);
        userQueryRequest.setSearchText(searchText);
        userQueryRequest.setNotId(userId);

        var userVOPage = userFeignClient.listUserByPage(userQueryRequest).getData();
        Page<ChatFriendUserVO> voPage = new Page<>(userQueryRequest.getCurrent(),
                userQueryRequest.getPageSize(),
                userVOPage == null ? 0L : userVOPage.getTotal());
        if (userVOPage == null || CollUtil.isEmpty(userVOPage.getRecords())) {
            return voPage;
        }

        List<UserVO> users = userVOPage.getRecords();
        Map<Long, Integer> onlineStatusMap = chatOnlineStatusService.getOnlineStatusMap(
                users.stream().map(UserVO::getId).toList());
        voPage.setRecords(users.stream().map(userVO -> {
            ChatFriendUserVO chatFriendUserVO = new ChatFriendUserVO();
            chatFriendUserVO.setId(userVO.getId());
            chatFriendUserVO.setUserName(userVO.getUserName());
            chatFriendUserVO.setUserAvatar(userVO.getUserAvatar());
            chatFriendUserVO.setOnlineStatus(onlineStatusMap.getOrDefault(userVO.getId(), 0));
            chatFriendUserVO.setFriendStatus(getFriendshipStatus(userId, userVO.getId()));
            return chatFriendUserVO;
        }).collect(Collectors.toList()));
        return voPage;
    }

    private void syncFriendToCache(Long userId, Long friendUserId) {
        String key = ChatCacheConstant.getUserFriendKey(userId);
        cacheUtils.sAdd(key, String.valueOf(friendUserId));
        cacheUtils.expire(key, ChatCacheConstant.USER_FRIEND_CACHE_EXPIRE_SECONDS);
    }

    private void clearFriendCache(Long userId, Long targetUserId) {
        cacheUtils.sRemove(ChatCacheConstant.getUserFriendKey(userId), String.valueOf(targetUserId));
        cacheUtils.sRemove(ChatCacheConstant.getUserFriendKey(targetUserId), String.valueOf(userId));
    }

    private void restoreFriendCacheIfMutual(Long userId, Long targetUserId) {
        if (getFriendByPair(userId, targetUserId) == null || getFriendByPair(targetUserId, userId) == null) {
            return;
        }
        syncFriendToCache(userId, targetUserId);
        syncFriendToCache(targetUserId, userId);
    }

    private void fillFriendProfile(ChatFriendUserVO vo) {
        if (StringUtils.isBlank(vo.getFriendGroupName())) {
            vo.setFriendGroupName(DEFAULT_FRIEND_GROUP_NAME);
        }
    }

    private String normalizeRemarkName(String remarkName) {
        String normalized = StringUtils.trimToNull(remarkName);
        ThrowUtils.throwIf(normalized != null && normalized.length() > MAX_REMARK_NAME_LENGTH,
                ErrorCode.PARAMS_ERROR, "好友备注过长");
        return normalized;
    }

    private String normalizeFriendGroupName(String friendGroupName) {
        String normalized = StringUtils.trimToNull(friendGroupName);
        if (normalized == null) {
            return DEFAULT_FRIEND_GROUP_NAME;
        }
        ThrowUtils.throwIf(normalized.length() > MAX_FRIEND_GROUP_NAME_LENGTH,
                ErrorCode.PARAMS_ERROR, "好友分组名称过长");
        return normalized;
    }

    private String normalizeFriendGroupNameForQuery(String friendGroupName) {
        String normalized = StringUtils.trimToNull(friendGroupName);
        if (normalized == null) {
            return null;
        }
        ThrowUtils.throwIf(normalized.length() > MAX_FRIEND_GROUP_NAME_LENGTH,
                ErrorCode.PARAMS_ERROR, "好友分组名称过长");
        return normalized;
    }

    protected UserVO getUserById(Long userId) {
        return userFeignClient.getUserVOById(userId).getData();
    }

    protected UserFriend getFriendByPair(Long userId, Long friendUserId) {
        return this.getOne(new LambdaQueryWrapper<UserFriend>()
                .eq(UserFriend::getUserId, userId)
                .eq(UserFriend::getFriendUserId, friendUserId)
                .last("LIMIT 1"));
    }

    protected UserFriendBlock getBlock(Long userId, Long blockedUserId) {
        if (userFriendBlockMapper == null) {
            return null;
        }
        return userFriendBlockMapper.selectOne(new LambdaQueryWrapper<UserFriendBlock>()
                .eq(UserFriendBlock::getUserId, userId)
                .eq(UserFriendBlock::getBlockedUserId, blockedUserId)
                .last("LIMIT 1"));
    }

    protected boolean saveBlock(UserFriendBlock block) {
        return userFriendBlockMapper.insert(block) > 0;
    }

    protected boolean removeBlock(Long userId, Long blockedUserId) {
        return userFriendBlockMapper.delete(new LambdaQueryWrapper<UserFriendBlock>()
                .eq(UserFriendBlock::getUserId, userId)
                .eq(UserFriendBlock::getBlockedUserId, blockedUserId)) >= 0;
    }

    private void loadFriendCache(Long userId) {
        List<UserFriend> friends = this.list(new LambdaQueryWrapper<UserFriend>()
                .eq(UserFriend::getUserId, userId));
        String key = ChatCacheConstant.getUserFriendKey(userId);
        if (CollUtil.isNotEmpty(friends)) {
            Set<String> friendIds = friends.stream()
                    .map(f -> String.valueOf(f.getFriendUserId()))
                    .collect(Collectors.toSet());
            cacheUtils.sAddAll(key, friendIds);
        } else {
            // 空好友列表也需要标识，防止缓存穿透，注意 SET 类型不能用 putString
            cacheUtils.sAdd(key, ChatCacheConstant.EMPTY_SET_PLACEHOLDER);
            cacheUtils.expire(key, 60);
        }
        cacheUtils.expire(key, ChatCacheConstant.USER_FRIEND_CACHE_EXPIRE_SECONDS);
    }

    protected boolean hasPendingFriendApply(Long userId, Long targetUserId) {
        return userFriendApplyMapper.selectCount(new LambdaQueryWrapper<UserFriendApply>()
                .eq(UserFriendApply::getStatus, 1)
                .eq(UserFriendApply::getUserId, userId)
                .eq(UserFriendApply::getTargetId, targetUserId)) > 0;
    }

    /**
     * 拒绝双方待处理好友申请（供子类覆盖用于测试）
     *
     * @param userId       用户 ID
     * @param targetUserId 目标用户 ID
     */
    protected void rejectPendingApplies(Long userId, Long targetUserId) {
        if (userFriendApplyService == null) {
            return;
        }
        List<UserFriendApply> pendingApplies = userFriendApplyMapper.selectList(
                new LambdaQueryWrapper<UserFriendApply>()
                        .eq(UserFriendApply::getStatus, 1)
                        .and(wrapper -> wrapper
                                .and(w -> w.eq(UserFriendApply::getUserId, userId)
                                        .eq(UserFriendApply::getTargetId, targetUserId))
                                .or()
                                .and(w -> w.eq(UserFriendApply::getUserId, targetUserId)
                                        .eq(UserFriendApply::getTargetId, userId))));
        for (UserFriendApply apply : pendingApplies) {
            apply.setStatus(3); // 3-已忽略
            userFriendApplyService.updateById(apply);
        }
    }
}
