# Moments Feed MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first foundation slice of the MallChat QQ-like moments feed MVP: publish text/image moments, list friend-visible moments, and delete own moments.

**Architecture:** Add a `ChatMoment` domain inside `mallchat-chat-service`, separate from `chat_message` and `chat_session`. The service uses friend relationship checks for visibility and keeps notification as a downstream display concern.

**Tech Stack:** Java 21, Spring Boot 3, MyBatis Plus, JUnit 5, OpenSpec, existing MallChat `BaseResponse`/`ResultUtils`/`ThrowUtils` conventions.

---

## File Structure

- Create: `mallchat-api/mallchat-api-chat/src/main/java/com/stephen/cloud/api/chat/model/dto/ChatMomentPublishRequest.java`
- Create: `mallchat-api/mallchat-api-chat/src/main/java/com/stephen/cloud/api/chat/model/dto/ChatMomentQueryRequest.java`
- Create: `mallchat-api/mallchat-api-chat/src/main/java/com/stephen/cloud/api/chat/model/vo/ChatMomentMediaVO.java`
- Create: `mallchat-api/mallchat-api-chat/src/main/java/com/stephen/cloud/api/chat/model/vo/ChatMomentVO.java`
- Create: `mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/controller/ChatMomentController.java`
- Create: `mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/mapper/ChatMomentMapper.java`
- Create: `mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/mapper/ChatMomentMediaMapper.java`
- Create: `mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/model/entity/ChatMoment.java`
- Create: `mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/model/entity/ChatMomentMedia.java`
- Create: `mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/service/ChatMomentService.java`
- Create: `mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/service/impl/ChatMomentServiceImpl.java`
- Create: `mallchat-service/mallchat-chat-service/src/test/java/com/stephen/cloud/chat/service/impl/ChatMomentServiceImplTest.java`
- Modify: `sql/mallchat.sql`

## Task 1: OpenSpec and SQL Contract

- [ ] **Step 1: Validate active change**

Run:

```bash
openspec validate add-moments-feed-mvp --strict
```

Expected: the change is valid before implementation starts.

- [ ] **Step 2: Add SQL tables**

Add to `sql/mallchat.sql`:

```sql
CREATE TABLE IF NOT EXISTS `chat_moment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '动态ID',
  `user_id` bigint NOT NULL COMMENT '发布用户ID',
  `content` varchar(1000) DEFAULT NULL COMMENT '动态正文',
  `media_count` int NOT NULL DEFAULT 0 COMMENT '媒体数量',
  `like_count` int NOT NULL DEFAULT 0 COMMENT '点赞数',
  `comment_count` int NOT NULL DEFAULT 0 COMMENT '评论数',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0-正常，1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态主体表';

CREATE TABLE IF NOT EXISTS `chat_moment_media` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '动态媒体ID',
  `moment_id` bigint NOT NULL COMMENT '动态ID',
  `url` varchar(1024) NOT NULL COMMENT '媒体URL',
  `width` int DEFAULT NULL COMMENT '图片宽度',
  `height` int DEFAULT NULL COMMENT '图片高度',
  `size` bigint DEFAULT NULL COMMENT '文件大小',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_moment_sort` (`moment_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态媒体表';
```

- [ ] **Step 3: Commit docs/spec only**

Do not include production SQL in this commit. SQL is committed with the implementation that makes the red tests pass.

```bash
git add docs/prd/P-001-im-real-time-communication-prd.md \
  docs/prd/P-007-qzone-like-moments-feed-prd.md \
  docs/design/D-002-qq-like-im-mvp-architecture.md \
  docs/superpowers/plans/2026-05-20-moments-feed-mvp.md \
  openspec/changes/add-moments-feed-mvp
git commit -m "spec: 定义动态 feed 基础切片"
```

## Task 2: Publish Moment Red-Green

- [ ] **Step 1: Write failing tests**

Add tests to `ChatMomentServiceImplTest`:

```java
@Test
void shouldRejectEmptyMoment() {
    ChatMomentPublishRequest request = new ChatMomentPublishRequest();
    Assertions.assertThrows(RuntimeException.class, () -> chatMomentService.publish(1L, request));
}

@Test
void shouldPublishTextMoment() {
    ChatMomentPublishRequest request = new ChatMomentPublishRequest();
    request.setContent("hello moments");

    Long momentId = chatMomentService.publish(1L, request);

    Assertions.assertEquals(100L, momentId);
    ChatMoment savedMoment = chatMomentService.fakeMomentMapper.getSavedMoment();
    Assertions.assertEquals(1L, savedMoment.getUserId());
    Assertions.assertEquals("hello moments", savedMoment.getContent());
    Assertions.assertEquals(0, savedMoment.getMediaCount());
}
```

- [ ] **Step 2: Verify RED**

Run:

```bash
mvn -pl :mallchat-chat-service -am test -Dtest=ChatMomentServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: first run may fail because types do not exist. After adding minimal empty types and methods, run it again and confirm failure is caused by business assertions, not only compilation.

- [ ] **Step 3: Commit failing tests**

```bash
git add mallchat-service/mallchat-chat-service/src/test/java/com/stephen/cloud/chat/service/impl/ChatMomentServiceImplTest.java
git commit -m "test: 增加动态发布边界测试"
```

- [ ] **Step 4: Implement minimal publish path**

Create DTO/entity/mapper/service/controller files from the file structure. `publish(Long loginUserId, ChatMomentPublishRequest request)` validates current user, rejects blank content with no media, rejects `content.length() > 1000`, rejects more than 9 media items, rejects blank or overlong media URLs, saves `ChatMoment`, then saves ordered `ChatMomentMedia` records in the same transaction when media exists.

- [ ] **Step 5: Verify GREEN**

Run the same targeted Maven command.

Expected: `ChatMomentServiceImplTest` passes.

- [ ] **Step 6: Commit implementation**

```bash
git add mallchat-api/mallchat-api-chat/src/main/java/com/stephen/cloud/api/chat/model/dto/ChatMomentPublishRequest.java \
  mallchat-api/mallchat-api-chat/src/main/java/com/stephen/cloud/api/chat/model/vo/ChatMomentMediaVO.java \
  mallchat-api/mallchat-api-chat/src/main/java/com/stephen/cloud/api/chat/model/vo/ChatMomentVO.java \
  mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat \
  sql/mallchat.sql
git commit -m "feat: 支持发布动态"
```

## Task 3: Friend-visible Timeline Red-Green

- [ ] **Step 1: Write failing tests**

Add tests:

```java
@Test
void shouldListOwnAndFriendMoments() {
    chatMomentService.setVisibleAuthorIds(1L, Set.of(1L, 2L));
    chatMomentService.moments = List.of(moment(10L, 1L, "mine"), moment(11L, 2L, "friend"));

    Page<ChatMomentVO> page = chatMomentService.listVisibleMoments(1L, 1, 10);

    Assertions.assertEquals(2, page.getRecords().size());
}

@Test
void shouldFilterStrangerMoments() {
    chatMomentService.setVisibleAuthorIds(1L, Set.of(1L));
    chatMomentService.moments = List.of(moment(11L, 2L, "stranger"));

    Page<ChatMomentVO> page = chatMomentService.listVisibleMoments(1L, 1, 10);

    Assertions.assertTrue(page.getRecords().isEmpty());
}
```

- [ ] **Step 2: Verify RED**

Run the targeted Maven command.

Expected: failure because timeline method is missing.

- [ ] **Step 3: Commit failing tests**

```bash
git add mallchat-service/mallchat-chat-service/src/test/java/com/stephen/cloud/chat/service/impl/ChatMomentServiceImplTest.java
git commit -m "test: 增加好友可见动态流测试"
```

- [ ] **Step 4: Implement list path**

Add `ChatMomentQueryRequest`, `GET /chat/moment/list`, service method `listVisibleMoments(Long loginUserId, int current, int pageSize)`. Determine visible author IDs first (`loginUserId` plus mutual friends), then query `chat_moment` by `user_id IN (...)` with `status=0`, `is_delete=0`, `create_time desc` pagination. Do not page all moments first and then filter in memory.

- [ ] **Step 5: Verify GREEN**

Run targeted test command.

Expected: pass.

- [ ] **Step 6: Commit implementation**

```bash
git add mallchat-api/mallchat-api-chat/src/main/java/com/stephen/cloud/api/chat/model/dto/ChatMomentQueryRequest.java \
  mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/controller/ChatMomentController.java \
  mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/service/ChatMomentService.java \
  mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/service/impl/ChatMomentServiceImpl.java
git commit -m "feat: 支持好友可见动态流"
```

## Task 4: Delete Own Moment Red-Green

- [ ] **Step 1: Write failing tests**

Add tests:

```java
@Test
void shouldDeleteOwnMoment() {
    chatMomentService.moments = List.of(moment(10L, 1L, "mine"));

    chatMomentService.deleteMoment(1L, 10L);

    Assertions.assertTrue(chatMomentService.fakeMomentMapper.getRemovedMomentIds().contains(10L));
}

@Test
void shouldRejectDeleteOtherUserMoment() {
    chatMomentService.moments = List.of(moment(10L, 2L, "friend"));

    Assertions.assertThrows(RuntimeException.class, () -> chatMomentService.deleteMoment(1L, 10L));
}
```

- [ ] **Step 2: Verify RED**

Run targeted test command.

Expected: failure because delete method is missing.

- [ ] **Step 3: Commit failing tests**

```bash
git add mallchat-service/mallchat-chat-service/src/test/java/com/stephen/cloud/chat/service/impl/ChatMomentServiceImplTest.java
git commit -m "test: 增加动态删除边界测试"
```

- [ ] **Step 4: Implement delete path**

Add `DELETE /chat/moment/delete?id={momentId}` and `deleteMoment(Long loginUserId, Long momentId)`. The service loads the moment by id including deleted rows, rejects missing moments, rejects non-authors, soft deletes active rows, and returns success for an author repeating deletion of an already deleted moment.

- [ ] **Step 5: Verify GREEN and regression**

Run:

```bash
mvn -pl :mallchat-chat-service -am test -Dtest=ChatMomentServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false
mvn -pl :mallchat-chat-service -am test
openspec validate --all --strict
```

Expected: targeted tests, chat-service regression, and OpenSpec all pass.

- [ ] **Step 6: Commit implementation**

```bash
git add mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/controller/ChatMomentController.java \
  mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/service/ChatMomentService.java \
  mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/service/impl/ChatMomentServiceImpl.java
git commit -m "feat: 支持删除自己的动态"
```

## Self-Review

- Spec coverage: publish/list/delete are covered by this plan; likes/comments/notifications are intentionally split into the next change to keep blast radius small, so this plan completes only the foundation slice of moments MVP.
- Placeholder scan: no TBD/TODO placeholders are present.
- Type consistency: all public names use `ChatMoment*` to match existing `Chat*` backend style.
- TDD consistency: red test commits contain tests only; implementation commits contain the minimal code and SQL needed to make those tests pass.
