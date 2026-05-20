package com.stephen.cloud.chat.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.chat.model.dto.ChatMomentMediaRequest;
import com.stephen.cloud.api.chat.model.dto.ChatMomentCommentRequest;
import com.stephen.cloud.api.chat.model.dto.ChatMomentPublishRequest;
import com.stephen.cloud.api.chat.model.vo.ChatMomentCommentVO;
import com.stephen.cloud.api.chat.model.vo.ChatMomentVO;
import com.stephen.cloud.chat.model.entity.ChatMoment;
import com.stephen.cloud.chat.model.entity.ChatMomentComment;
import com.stephen.cloud.chat.model.entity.ChatMomentLike;
import com.stephen.cloud.chat.model.entity.ChatMomentMedia;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class ChatMomentServiceImplTest {

    private TestableChatMomentServiceImpl chatMomentService;

    @BeforeEach
    void setUp() {
        chatMomentService = new TestableChatMomentServiceImpl();
    }

    @Test
    void shouldRejectEmptyMoment() {
        ChatMomentPublishRequest request = new ChatMomentPublishRequest();

        Assertions.assertThrows(RuntimeException.class, () -> chatMomentService.publish(1L, request));
    }

    @Test
    void shouldRejectOverlongContent() {
        ChatMomentPublishRequest request = new ChatMomentPublishRequest();
        request.setContent("a".repeat(1001));

        Assertions.assertThrows(RuntimeException.class, () -> chatMomentService.publish(1L, request));
    }

    @Test
    void shouldRejectTooManyMediaItems() {
        ChatMomentPublishRequest request = new ChatMomentPublishRequest();
        request.setMediaList(new ArrayList<>());
        for (int i = 0; i < 10; i++) {
            request.getMediaList().add(media("https://example.com/" + i + ".png", i));
        }

        Assertions.assertThrows(RuntimeException.class, () -> chatMomentService.publish(1L, request));
    }

    @Test
    void shouldRejectBlankMediaUrl() {
        ChatMomentPublishRequest request = new ChatMomentPublishRequest();
        request.setMediaList(List.of(media(" ", 0)));

        Assertions.assertThrows(RuntimeException.class, () -> chatMomentService.publish(1L, request));
    }

    @Test
    void shouldPublishTextMoment() {
        ChatMomentPublishRequest request = new ChatMomentPublishRequest();
        request.setContent("hello moments");

        Long momentId = chatMomentService.publish(1L, request);

        Assertions.assertEquals(100L, momentId);
        Assertions.assertEquals(1L, chatMomentService.savedMoment.getUserId());
        Assertions.assertEquals("hello moments", chatMomentService.savedMoment.getContent());
        Assertions.assertEquals(0, chatMomentService.savedMoment.getMediaCount());
        Assertions.assertTrue(chatMomentService.savedMediaList.isEmpty());
    }

    @Test
    void shouldPublishImageMomentWithMediaOrder() {
        ChatMomentPublishRequest request = new ChatMomentPublishRequest();
        request.setContent("with images");
        request.setMediaList(List.of(
                media("https://example.com/1.png", 0),
                media("https://example.com/2.png", 1)));

        Long momentId = chatMomentService.publish(1L, request);

        Assertions.assertEquals(100L, momentId);
        Assertions.assertEquals(2, chatMomentService.savedMoment.getMediaCount());
        Assertions.assertEquals(2, chatMomentService.savedMediaList.size());
        Assertions.assertEquals(100L, chatMomentService.savedMediaList.get(0).getMomentId());
        Assertions.assertEquals("https://example.com/1.png", chatMomentService.savedMediaList.get(0).getUrl());
        Assertions.assertEquals(0, chatMomentService.savedMediaList.get(0).getSortOrder());
        Assertions.assertEquals(1, chatMomentService.savedMediaList.get(1).getSortOrder());
    }

    @Test
    void shouldListOwnAndFriendMoments() {
        chatMomentService.visibleFriendIds = new LinkedHashSet<>(Set.of(2L));
        chatMomentService.moments = List.of(
                moment(10L, 1L, "mine"),
                moment(11L, 2L, "friend"));

        Page<ChatMomentVO> page = chatMomentService.listVisibleMoments(1L, 1, 10);

        Assertions.assertEquals(new LinkedHashSet<>(Set.of(1L, 2L)), chatMomentService.capturedVisibleAuthorIds);
        Assertions.assertEquals(2, page.getRecords().size());
        Assertions.assertEquals(10L, page.getRecords().get(0).getId());
        Assertions.assertEquals(11L, page.getRecords().get(1).getId());
    }

    @Test
    void shouldFilterStrangerMomentsBeforePagination() {
        chatMomentService.visibleFriendIds = new LinkedHashSet<>();
        chatMomentService.moments = List.of(
                moment(10L, 2L, "stranger"),
                moment(11L, 1L, "mine"));

        Page<ChatMomentVO> page = chatMomentService.listVisibleMoments(1L, 1, 10);

        Assertions.assertEquals(new LinkedHashSet<>(Set.of(1L)), chatMomentService.capturedVisibleAuthorIds);
        Assertions.assertEquals(1, page.getRecords().size());
        Assertions.assertEquals(11L, page.getRecords().get(0).getId());
    }

    @Test
    void shouldDeleteOwnMoment() {
        ChatMoment moment = moment(10L, 1L, "mine");
        chatMomentService.momentById = Map.of(10L, moment);

        chatMomentService.deleteMoment(1L, 10L);

        Assertions.assertEquals(List.of(10L), chatMomentService.deletedMomentIds);
    }

    @Test
    void shouldKeepRepeatedDeleteByAuthorIdempotent() {
        ChatMoment deletedMoment = moment(10L, 1L, "mine");
        deletedMoment.setStatus(1);
        deletedMoment.setIsDelete(1);
        chatMomentService.momentById = Map.of(10L, deletedMoment);

        chatMomentService.deleteMoment(1L, 10L);

        Assertions.assertTrue(chatMomentService.deletedMomentIds.isEmpty());
    }

    @Test
    void shouldRejectDeleteOtherUserMoment() {
        chatMomentService.momentById = Map.of(10L, moment(10L, 2L, "friend"));

        Assertions.assertThrows(RuntimeException.class, () -> chatMomentService.deleteMoment(1L, 10L));
    }

    @Test
    void shouldRejectMissingMomentDelete() {
        chatMomentService.momentById = Map.of();

        Assertions.assertThrows(RuntimeException.class, () -> chatMomentService.deleteMoment(1L, 10L));
    }

    @Test
    void shouldLikeVisibleMomentAndCreateNotification() {
        chatMomentService.visibleFriendIds = new LinkedHashSet<>(Set.of(2L));
        chatMomentService.momentById = Map.of(10L, moment(10L, 2L, "friend"));

        chatMomentService.likeMoment(1L, 10L);

        Assertions.assertEquals(1, chatMomentService.savedLikes.size());
        Assertions.assertEquals(10L, chatMomentService.savedLikes.get(0).getMomentId());
        Assertions.assertEquals(1L, chatMomentService.savedLikes.get(0).getUserId());
        Assertions.assertEquals(List.of(10L), chatMomentService.likeIncrementMomentIds);
        Assertions.assertEquals(List.of("like:10:2:1"), chatMomentService.sentNotifications);
    }

    @Test
    void shouldKeepRepeatedLikeIdempotent() {
        chatMomentService.visibleFriendIds = new LinkedHashSet<>(Set.of(2L));
        chatMomentService.momentById = Map.of(10L, moment(10L, 2L, "friend"));
        chatMomentService.likeByMomentAndUser = Map.of("10:1", activeLike(31L, 10L, 1L));

        chatMomentService.likeMoment(1L, 10L);

        Assertions.assertTrue(chatMomentService.savedLikes.isEmpty());
        Assertions.assertTrue(chatMomentService.likeIncrementMomentIds.isEmpty());
        Assertions.assertTrue(chatMomentService.sentNotifications.isEmpty());
    }

    @Test
    void shouldUnlikeLikedMomentAndDecreaseCount() {
        chatMomentService.visibleFriendIds = new LinkedHashSet<>(Set.of(2L));
        chatMomentService.momentById = Map.of(10L, moment(10L, 2L, "friend"));
        chatMomentService.likeByMomentAndUser = Map.of("10:1", activeLike(31L, 10L, 1L));

        chatMomentService.unlikeMoment(1L, 10L);

        Assertions.assertEquals(List.of(31L), chatMomentService.deletedLikeIds);
        Assertions.assertEquals(List.of(10L), chatMomentService.likeDecreaseMomentIds);
    }

    @Test
    void shouldKeepRepeatedUnlikeIdempotent() {
        chatMomentService.visibleFriendIds = new LinkedHashSet<>(Set.of(2L));
        chatMomentService.momentById = Map.of(10L, moment(10L, 2L, "friend"));

        chatMomentService.unlikeMoment(1L, 10L);

        Assertions.assertTrue(chatMomentService.deletedLikeIds.isEmpty());
        Assertions.assertTrue(chatMomentService.likeDecreaseMomentIds.isEmpty());
    }

    @Test
    void shouldRejectInvisibleMomentInteraction() {
        chatMomentService.visibleFriendIds = new LinkedHashSet<>();
        chatMomentService.momentById = Map.of(10L, moment(10L, 2L, "stranger"));

        Assertions.assertThrows(RuntimeException.class, () -> chatMomentService.likeMoment(1L, 10L));
        Assertions.assertThrows(RuntimeException.class, () -> chatMomentService.commentMoment(1L, commentRequest(10L, "hi")));
        Assertions.assertTrue(chatMomentService.savedLikes.isEmpty());
        Assertions.assertTrue(chatMomentService.savedComments.isEmpty());
    }

    @Test
    void shouldRejectInvalidCommentContent() {
        chatMomentService.visibleFriendIds = new LinkedHashSet<>(Set.of(2L));
        chatMomentService.momentById = Map.of(10L, moment(10L, 2L, "friend"));

        Assertions.assertThrows(RuntimeException.class, () -> chatMomentService.commentMoment(1L, commentRequest(10L, " ")));
        Assertions.assertThrows(RuntimeException.class, () -> chatMomentService.commentMoment(1L, commentRequest(10L, "a".repeat(501))));
    }

    @Test
    void shouldCommentVisibleMomentAndCreateNotification() {
        chatMomentService.visibleFriendIds = new LinkedHashSet<>(Set.of(2L));
        chatMomentService.momentById = Map.of(10L, moment(10L, 2L, "friend"));

        Long commentId = chatMomentService.commentMoment(1L, commentRequest(10L, " hello "));

        Assertions.assertEquals(200L, commentId);
        Assertions.assertEquals(1, chatMomentService.savedComments.size());
        Assertions.assertEquals(10L, chatMomentService.savedComments.get(0).getMomentId());
        Assertions.assertEquals(1L, chatMomentService.savedComments.get(0).getUserId());
        Assertions.assertEquals("hello", chatMomentService.savedComments.get(0).getContent());
        Assertions.assertEquals(List.of(10L), chatMomentService.commentIncrementMomentIds);
        Assertions.assertEquals(List.of("comment:10:2:1"), chatMomentService.sentNotifications);
    }

    @Test
    void shouldListVisibleMomentComments() {
        chatMomentService.visibleFriendIds = new LinkedHashSet<>(Set.of(2L));
        chatMomentService.momentById = Map.of(10L, moment(10L, 2L, "friend"));
        chatMomentService.comments = List.of(comment(101L, 10L, 2L, "first"), comment(102L, 10L, 3L, "second"));

        Page<ChatMomentCommentVO> page = chatMomentService.listComments(1L, 10L, 1, 10);

        Assertions.assertEquals(2, page.getRecords().size());
        Assertions.assertEquals(101L, page.getRecords().get(0).getId());
        Assertions.assertEquals("first", page.getRecords().get(0).getContent());
    }

    @Test
    void shouldKeepInteractionWhenNotificationFails() {
        chatMomentService.visibleFriendIds = new LinkedHashSet<>(Set.of(2L));
        chatMomentService.momentById = Map.of(10L, moment(10L, 2L, "friend"));
        chatMomentService.failNotification = true;

        chatMomentService.likeMoment(1L, 10L);
        Long commentId = chatMomentService.commentMoment(1L, commentRequest(10L, "still saved"));

        Assertions.assertEquals(200L, commentId);
        Assertions.assertEquals(1, chatMomentService.savedLikes.size());
        Assertions.assertEquals(1, chatMomentService.savedComments.size());
        Assertions.assertEquals(List.of(10L), chatMomentService.likeIncrementMomentIds);
        Assertions.assertEquals(List.of(10L), chatMomentService.commentIncrementMomentIds);
    }

    private static ChatMomentMediaRequest media(String url, int sortOrder) {
        ChatMomentMediaRequest request = new ChatMomentMediaRequest();
        request.setUrl(url);
        request.setWidth(100);
        request.setHeight(80);
        request.setSize(1024L);
        request.setSortOrder(sortOrder);
        return request;
    }

    private static ChatMoment moment(Long id, Long userId, String content) {
        ChatMoment moment = new ChatMoment();
        moment.setId(id);
        moment.setUserId(userId);
        moment.setContent(content);
        moment.setMediaCount(0);
        moment.setLikeCount(0);
        moment.setCommentCount(0);
        moment.setStatus(0);
        moment.setIsDelete(0);
        moment.setCreateTime(new Date(id));
        return moment;
    }

    private static ChatMomentLike activeLike(Long id, Long momentId, Long userId) {
        ChatMomentLike like = new ChatMomentLike();
        like.setId(id);
        like.setMomentId(momentId);
        like.setUserId(userId);
        like.setIsDelete(0);
        return like;
    }

    private static ChatMomentComment comment(Long id, Long momentId, Long userId, String content) {
        ChatMomentComment comment = new ChatMomentComment();
        comment.setId(id);
        comment.setMomentId(momentId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setIsDelete(0);
        comment.setCreateTime(new Date(id));
        return comment;
    }

    private static ChatMomentCommentRequest commentRequest(Long momentId, String content) {
        ChatMomentCommentRequest request = new ChatMomentCommentRequest();
        request.setMomentId(momentId);
        request.setContent(content);
        return request;
    }

    private static class TestableChatMomentServiceImpl extends ChatMomentServiceImpl {
        private ChatMoment savedMoment;
        private final List<ChatMomentMedia> savedMediaList = new ArrayList<>();
        private List<ChatMoment> moments = new ArrayList<>();
        private Set<Long> visibleFriendIds = new LinkedHashSet<>();
        private Set<Long> capturedVisibleAuthorIds = new LinkedHashSet<>();
        private Map<Long, ChatMoment> momentById = Map.of();
        private final List<Long> deletedMomentIds = new ArrayList<>();
        private Map<String, ChatMomentLike> likeByMomentAndUser = Map.of();
        private final List<ChatMomentLike> savedLikes = new ArrayList<>();
        private final List<Long> restoredLikeIds = new ArrayList<>();
        private final List<Long> deletedLikeIds = new ArrayList<>();
        private final List<Long> likeIncrementMomentIds = new ArrayList<>();
        private final List<Long> likeDecreaseMomentIds = new ArrayList<>();
        private final List<ChatMomentComment> savedComments = new ArrayList<>();
        private final List<Long> commentIncrementMomentIds = new ArrayList<>();
        private List<ChatMomentComment> comments = new ArrayList<>();
        private final List<String> sentNotifications = new ArrayList<>();
        private boolean failNotification;

        @Override
        protected boolean saveMoment(ChatMoment moment) {
            moment.setId(100L);
            this.savedMoment = moment;
            return true;
        }

        @Override
        protected boolean saveMomentMedia(List<ChatMomentMedia> mediaList) {
            this.savedMediaList.clear();
            this.savedMediaList.addAll(mediaList);
            return true;
        }

        @Override
        protected Set<Long> listMutualFriendIds(Long userId) {
            return new LinkedHashSet<>(visibleFriendIds);
        }

        @Override
        protected Page<ChatMoment> pageVisibleMoments(Set<Long> visibleAuthorIds, int current, int pageSize) {
            capturedVisibleAuthorIds = new LinkedHashSet<>(visibleAuthorIds);
            List<ChatMoment> records = moments.stream()
                    .filter(item -> visibleAuthorIds.contains(item.getUserId()))
                    .filter(item -> Integer.valueOf(0).equals(item.getStatus()))
                    .filter(item -> Integer.valueOf(0).equals(item.getIsDelete()))
                    .toList();
            Page<ChatMoment> page = new Page<>(current, pageSize, records.size());
            page.setRecords(records);
            return page;
        }

        @Override
        protected Map<Long, List<com.stephen.cloud.api.chat.model.vo.ChatMomentMediaVO>> listMomentMediaMap(List<Long> momentIds) {
            return Map.of();
        }

        @Override
        protected ChatMoment getMomentIncludingDeleted(Long momentId) {
            return momentById.get(momentId);
        }

        @Override
        protected boolean softDeleteMoment(Long momentId) {
            deletedMomentIds.add(momentId);
            return true;
        }

        @Override
        protected ChatMomentLike getMomentLikeIncludingDeleted(Long momentId, Long userId) {
            return likeByMomentAndUser.get(momentId + ":" + userId);
        }

        @Override
        protected boolean saveMomentLike(ChatMomentLike like) {
            savedLikes.add(like);
            return true;
        }

        @Override
        protected boolean restoreMomentLike(Long likeId) {
            restoredLikeIds.add(likeId);
            return true;
        }

        @Override
        protected boolean softDeleteMomentLike(Long likeId) {
            deletedLikeIds.add(likeId);
            return true;
        }

        @Override
        protected boolean increaseMomentLikeCount(Long momentId) {
            likeIncrementMomentIds.add(momentId);
            return true;
        }

        @Override
        protected boolean decreaseMomentLikeCount(Long momentId) {
            likeDecreaseMomentIds.add(momentId);
            return true;
        }

        @Override
        protected boolean saveMomentComment(ChatMomentComment comment) {
            comment.setId(200L);
            savedComments.add(comment);
            return true;
        }

        @Override
        protected boolean increaseMomentCommentCount(Long momentId) {
            commentIncrementMomentIds.add(momentId);
            return true;
        }

        @Override
        protected Page<ChatMomentComment> pageMomentComments(Long momentId, int current, int pageSize) {
            List<ChatMomentComment> records = comments.stream()
                    .filter(item -> momentId.equals(item.getMomentId()))
                    .filter(item -> Integer.valueOf(0).equals(item.getIsDelete()))
                    .toList();
            Page<ChatMomentComment> page = new Page<>(current, pageSize, records.size());
            page.setRecords(records);
            return page;
        }

        @Override
        protected void sendMomentInteractionNotification(ChatMoment moment, Long actorUserId, String type, Long commentId) {
            if (failNotification) {
                throw new RuntimeException("notification down");
            }
            sentNotifications.add(type + ":" + moment.getId() + ":" + moment.getUserId() + ":" + actorUserId);
        }
    }
}
