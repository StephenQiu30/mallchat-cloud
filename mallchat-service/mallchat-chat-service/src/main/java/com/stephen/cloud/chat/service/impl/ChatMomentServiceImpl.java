package com.stephen.cloud.chat.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.api.chat.model.dto.ChatMomentMediaRequest;
import com.stephen.cloud.api.chat.model.dto.ChatMomentPublishRequest;
import com.stephen.cloud.api.chat.model.vo.ChatMomentMediaVO;
import com.stephen.cloud.api.chat.model.vo.ChatMomentVO;
import com.stephen.cloud.chat.mapper.ChatMomentMapper;
import com.stephen.cloud.chat.mapper.ChatMomentMediaMapper;
import com.stephen.cloud.chat.model.entity.ChatMoment;
import com.stephen.cloud.chat.model.entity.ChatMomentMedia;
import com.stephen.cloud.chat.service.ChatMomentService;
import com.stephen.cloud.chat.service.UserFriendService;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 动态服务实现
 *
 * @author StephenQiu30
 */
@Service
public class ChatMomentServiceImpl extends ServiceImpl<ChatMomentMapper, ChatMoment>
        implements ChatMomentService {

    private static final int MAX_CONTENT_LENGTH = 1000;
    private static final int MAX_MEDIA_COUNT = 9;
    private static final int MAX_MEDIA_URL_LENGTH = 1024;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 20;
    private static final int STATUS_NORMAL = 0;
    private static final int STATUS_DELETED = 1;

    @Resource
    private ChatMomentMediaMapper chatMomentMediaMapper;

    @Resource
    private UserFriendService userFriendService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publish(Long userId, ChatMomentPublishRequest request) {
        ThrowUtils.throwIf(userId == null || request == null, ErrorCode.PARAMS_ERROR);
        String content = normalizeContent(request.getContent());
        List<ChatMomentMediaRequest> mediaList = normalizeMediaList(request.getMediaList());
        ThrowUtils.throwIf(StrUtil.isBlank(content) && CollUtil.isEmpty(mediaList), ErrorCode.PARAMS_ERROR, "动态内容不能为空");

        ChatMoment moment = new ChatMoment();
        moment.setUserId(userId);
        moment.setContent(content);
        moment.setMediaCount(mediaList.size());
        moment.setLikeCount(0);
        moment.setCommentCount(0);
        moment.setStatus(STATUS_NORMAL);
        moment.setIsDelete(0);
        boolean saved = saveMoment(moment);
        ThrowUtils.throwIf(!saved || moment.getId() == null, ErrorCode.OPERATION_ERROR, "发布动态失败");

        if (CollUtil.isNotEmpty(mediaList)) {
            boolean mediaSaved = saveMomentMedia(buildMomentMedia(moment.getId(), mediaList));
            ThrowUtils.throwIf(!mediaSaved, ErrorCode.OPERATION_ERROR, "保存动态媒体失败");
        }
        return moment.getId();
    }

    @Override
    public Page<ChatMomentVO> listVisibleMoments(Long userId, int current, int pageSize) {
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR);
        int normalizedCurrent = current <= 0 ? 1 : current;
        int normalizedPageSize = normalizePageSize(pageSize);
        Set<Long> visibleAuthorIds = new LinkedHashSet<>();
        visibleAuthorIds.add(userId);
        visibleAuthorIds.addAll(listMutualFriendIds(userId));

        Page<ChatMoment> momentPage = pageVisibleMoments(visibleAuthorIds, normalizedCurrent, normalizedPageSize);
        Page<ChatMomentVO> voPage = new Page<>(momentPage.getCurrent(), momentPage.getSize(), momentPage.getTotal());
        if (CollUtil.isEmpty(momentPage.getRecords())) {
            return voPage;
        }
        List<Long> momentIds = momentPage.getRecords().stream().map(ChatMoment::getId).toList();
        Map<Long, List<ChatMomentMediaVO>> mediaMap = listMomentMediaMap(momentIds);
        voPage.setRecords(momentPage.getRecords().stream()
                .map(moment -> toVO(moment, mediaMap.getOrDefault(moment.getId(), Collections.emptyList())))
                .toList());
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMoment(Long userId, Long momentId) {
        ThrowUtils.throwIf(userId == null || momentId == null || momentId <= 0, ErrorCode.PARAMS_ERROR);
        ChatMoment moment = getMomentIncludingDeleted(momentId);
        ThrowUtils.throwIf(moment == null, ErrorCode.NOT_FOUND_ERROR, "动态不存在");
        ThrowUtils.throwIf(!Objects.equals(userId, moment.getUserId()), ErrorCode.NO_AUTH_ERROR, "无权删除该动态");
        if (Objects.equals(moment.getStatus(), STATUS_DELETED) || Objects.equals(moment.getIsDelete(), 1)) {
            return;
        }
        boolean deleted = softDeleteMoment(momentId);
        ThrowUtils.throwIf(!deleted, ErrorCode.OPERATION_ERROR, "删除动态失败");
    }

    protected boolean saveMoment(ChatMoment moment) {
        return this.save(moment);
    }

    protected boolean saveMomentMedia(List<ChatMomentMedia> mediaList) {
        if (CollUtil.isEmpty(mediaList)) {
            return true;
        }
        for (ChatMomentMedia media : mediaList) {
            if (chatMomentMediaMapper.insert(media) <= 0) {
                return false;
            }
        }
        return true;
    }

    protected Set<Long> listMutualFriendIds(Long userId) {
        return userFriendService.listMutualFriendIds(userId);
    }

    protected Page<ChatMoment> pageVisibleMoments(Set<Long> visibleAuthorIds, int current, int pageSize) {
        if (CollUtil.isEmpty(visibleAuthorIds)) {
            return new Page<>(current, pageSize, 0);
        }
        return this.page(new Page<>(current, pageSize),
                new LambdaQueryWrapper<ChatMoment>()
                        .in(ChatMoment::getUserId, visibleAuthorIds)
                        .eq(ChatMoment::getStatus, STATUS_NORMAL)
                        .orderByDesc(ChatMoment::getCreateTime)
                        .orderByDesc(ChatMoment::getId));
    }

    protected ChatMoment getMomentIncludingDeleted(Long momentId) {
        return baseMapper.selectByIdIncludingDeleted(momentId);
    }

    protected boolean softDeleteMoment(Long momentId) {
        return this.update(new LambdaUpdateWrapper<ChatMoment>()
                .eq(ChatMoment::getId, momentId)
                .set(ChatMoment::getStatus, STATUS_DELETED)
                .set(ChatMoment::getIsDelete, 1));
    }

    protected Map<Long, List<ChatMomentMediaVO>> listMomentMediaMap(List<Long> momentIds) {
        if (CollUtil.isEmpty(momentIds)) {
            return Collections.emptyMap();
        }
        List<ChatMomentMedia> mediaList = chatMomentMediaMapper.selectList(new LambdaQueryWrapper<ChatMomentMedia>()
                .in(ChatMomentMedia::getMomentId, momentIds)
                .orderByAsc(ChatMomentMedia::getMomentId)
                .orderByAsc(ChatMomentMedia::getSortOrder)
                .orderByAsc(ChatMomentMedia::getId));
        if (CollUtil.isEmpty(mediaList)) {
            return Collections.emptyMap();
        }
        return mediaList.stream()
                .map(this::toMediaVO)
                .collect(Collectors.groupingBy(ChatMomentMediaVO::getMomentId));
    }

    private String normalizeContent(String content) {
        if (content == null) {
            return null;
        }
        String normalized = content.trim();
        ThrowUtils.throwIf(normalized.length() > MAX_CONTENT_LENGTH, ErrorCode.PARAMS_ERROR, "动态正文过长");
        return normalized;
    }

    private List<ChatMomentMediaRequest> normalizeMediaList(List<ChatMomentMediaRequest> mediaList) {
        if (mediaList == null) {
            return Collections.emptyList();
        }
        ThrowUtils.throwIf(mediaList.size() > MAX_MEDIA_COUNT, ErrorCode.PARAMS_ERROR, "动态图片最多 9 张");
        for (ChatMomentMediaRequest media : mediaList) {
            ThrowUtils.throwIf(media == null || StrUtil.isBlank(media.getUrl()), ErrorCode.PARAMS_ERROR, "动态媒体 URL 不能为空");
            String url = media.getUrl().trim();
            ThrowUtils.throwIf(url.length() > MAX_MEDIA_URL_LENGTH, ErrorCode.PARAMS_ERROR, "动态媒体 URL 过长");
            media.setUrl(url);
        }
        return mediaList;
    }

    private List<ChatMomentMedia> buildMomentMedia(Long momentId, List<ChatMomentMediaRequest> mediaRequests) {
        List<ChatMomentMedia> result = new ArrayList<>();
        for (int i = 0; i < mediaRequests.size(); i++) {
            ChatMomentMediaRequest request = mediaRequests.get(i);
            ChatMomentMedia media = new ChatMomentMedia();
            media.setMomentId(momentId);
            media.setUrl(request.getUrl());
            media.setWidth(request.getWidth());
            media.setHeight(request.getHeight());
            media.setSize(request.getSize());
            media.setSortOrder(request.getSortOrder() == null ? i : request.getSortOrder());
            media.setIsDelete(0);
            result.add(media);
        }
        return result;
    }

    private int normalizePageSize(int pageSize) {
        if (pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private ChatMomentVO toVO(ChatMoment moment, List<ChatMomentMediaVO> mediaList) {
        ChatMomentVO vo = new ChatMomentVO();
        vo.setId(moment.getId());
        vo.setUserId(moment.getUserId());
        vo.setContent(moment.getContent());
        vo.setMediaCount(moment.getMediaCount());
        vo.setLikeCount(moment.getLikeCount());
        vo.setCommentCount(moment.getCommentCount());
        vo.setMediaList(mediaList);
        vo.setCreateTime(moment.getCreateTime());
        return vo;
    }

    private ChatMomentMediaVO toMediaVO(ChatMomentMedia media) {
        ChatMomentMediaVO vo = new ChatMomentMediaVO();
        vo.setId(media.getId());
        vo.setMomentId(media.getMomentId());
        vo.setUrl(media.getUrl());
        vo.setWidth(media.getWidth());
        vo.setHeight(media.getHeight());
        vo.setSize(media.getSize());
        vo.setSortOrder(media.getSortOrder());
        return vo;
    }
}
