---
layer: Acceptance
doc_no: "A-021"
audience:
  - Dev
  - QA
  - Ops
feature_area: im-room-message-experience
purpose: "记录 m7 群聊治理与消息体验 Epic 的测试先行、实现范围和验收命令。"
canonical_path: "docs/acceptance/A-021-m7-room-message-experience-acceptance.md"
status: review
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "GitHub Issue #26"
  - "GitHub Issue #30"
  - "GitHub Issue #31"
  - "GitHub Issue #32"
  - "GitHub Issue #33"
  - "openspec/changes/add-room-admin-role"
  - "openspec/changes/add-room-join-approval"
  - "openspec/changes/add-message-search-mvp"
  - "openspec/changes/add-notification-preferences"
outputs:
  - "m7-backend-room-message-epic"
  - "群管理员任免"
  - "入群审批 MVP"
  - "消息搜索 MVP"
  - "会话免打扰"
  - "sql/migrations/20260521_m7_room_message_experience.sql"
triggers:
  - "创建或更新 m7 PR"
  - "回归群聊治理与消息体验 Epic #26"
downstream:
  - "GitHub Epic #26"
---

# m7 群聊治理与消息体验验收

## 1. 验收范围

本次 m7 聚合消费 Epic #26 下的 #30、#31、#32、#33。实现保持最小生产可用闭环：管理员任免复用 `chat_room_member.role`；入群审批新增申请事实表；消息搜索使用 DB LIKE MVP；免打扰复用 `chat_session`，只影响 `CHAT_MESSAGE` 推送目标，不影响消息事实、未读事实和重连补偿。

## 2. 结论

1. #30：群主可任命/取消管理员，非群主和群主目标会被拒绝。
2. #31：非成员可提交入群申请，群主/管理员可同意或拒绝，同意后创建成员和会话。
3. #32：房间成员可按关键词搜索正常文本消息，非成员和空关键词被拒绝。
4. #33：会话支持免打扰状态，群聊实时消息推送会排除免打扰接收者，Redis 房间成员缓存不会绕过 `userIds` allowlist。

## 3. RED 证据

1. `ChatMessagePushHandlerTest.shouldRespectMessageUserIdsAsAllowlistWhenRoomMemberCacheAlsoExists` 初次失败：缓存成员 `3` 被错误推送。
2. `ChatRoomServiceImplTest` 新增管理员任免测试初次编译失败：缺少 `grantAdmin` / `revokeAdmin`。
3. `ChatRoomJoinApplyServiceImplTest` 初次编译失败：缺少入群申请 DTO、VO、Entity、Service。
4. `ChatMessageServiceImplTest` 新增搜索测试初次编译失败：缺少 `searchMessages`。
5. `ChatSessionServiceImplTest` 新增免打扰测试初次编译失败：缺少 `muteSession`、`filterPushUserIds` 和 `muteStatus`。
6. `ChatRoomJoinApplyServiceImplTest.shouldRejectApprovalWhenPendingStatusUpdateLosesRace` 初次失败：审批更新未带待处理状态条件，无法抵御并发重复审批。

## 4. GREEN 命令

```bash
mvn -pl mallchat-service/mallchat-chat-service,mallchat-service/mallchat-notification-service -am -Dtest=ChatRoomServiceImplTest,ChatRoomJoinApplyServiceImplTest,ChatMessageServiceImplTest,ChatSessionServiceImplTest,ChatMessagePushHandlerTest -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：97 个测试通过，0 failure，0 error。

## 5. 验收门禁

```bash
openspec validate --all --strict
git diff --check
mvn -pl mallchat-service/mallchat-chat-service,mallchat-service/mallchat-notification-service -am test
```

结果：OpenSpec 30 项通过、0 failed；`git diff --check` 通过；chat-service 与 notification-service 相关模块全量回归 196 个测试通过。

## 6. 残余风险

1. 消息搜索是 DB LIKE MVP，不承担全文索引、高亮或跨房间搜索。
2. 已有生产库需要先执行 `sql/migrations/20260521_m7_room_message_experience.sql`，新环境可继续直接执行 `sql/mallchat.sql`。
3. 入群申请通知失败只降级记录日志，端侧可通过申请列表恢复处理。
4. 免打扰只过滤 `CHAT_MESSAGE` 强提醒推送，不屏蔽已读、撤回和会话状态同步。
