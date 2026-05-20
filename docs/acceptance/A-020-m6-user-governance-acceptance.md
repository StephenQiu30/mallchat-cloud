---
layer: Acceptance
doc_no: "A-020"
audience:
  - Dev
  - QA
  - Ops
feature_area: im-user-governance
purpose: "记录 m6 用户安全与关系治理 Epic 的测试先行、实现范围和验收命令。"
canonical_path: "docs/acceptance/A-020-m6-user-governance-acceptance.md"
status: review
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "GitHub Issue #25"
  - "GitHub Issue #27"
  - "GitHub Issue #28"
  - "GitHub Issue #29"
  - "openspec/changes/add-chat-friend-blocklist"
  - "openspec/changes/add-chat-report-mvp"
  - "openspec/changes/add-friend-remark-group"
outputs:
  - "m6-backend-user-governance-epic"
  - "用户拉黑与解除拉黑"
  - "举报用户/消息/动态 MVP"
  - "好友备注与轻量分组"
triggers:
  - "创建或更新 m6 PR"
  - "回归用户安全与关系治理 Epic #25"
downstream:
  - "GitHub Epic #25"
  - "GitHub PR 待创建"
---

# m6 用户安全与关系治理验收

## 1. 验收范围

本次 m6 聚合消费 Epic #25 下的 #27、#28、#29。实现保持最小生产可用闭环：拉黑只阻断好友申请、私聊和动态可见好友集合；举报只记录待处理事实；好友分组只做 `user_friend` 单表轻量字段。

## 2. 结论

1. #27：新增 `user_friend_block`，拉黑/解除拉黑保持幂等，并阻断好友申请、私聊发送和已拉黑好友的动态可见集合。
2. #28：新增 `chat_report`，支持举报用户、消息和动态，同一用户对同一对象重复举报返回既有 ID。
3. #29：`user_friend` 支持好友备注和轻量分组，好友列表返回联系人资料并可按分组过滤。

## 3. RED 证据

1. `UserFriendServiceImplTest` 初次编译失败：缺少 `ChatFriendProfileUpdateRequest`、`UserFriendBlock` 和黑名单相关 Service 方法。
2. `UserFriendApplyServiceImplTest` 初次编译失败：缺少 `isBlockedBetween` 契约。
3. `ChatMessageServiceImplTest` 初次编译失败：缺少私聊拉黑权限契约。
4. `ChatReportServiceImplTest` 初次编译失败：缺少 `ChatReportSubmitRequest`、`ChatReportTargetTypeEnum`、`ChatReport` 和 `ChatReportServiceImpl`。
5. 代码审查后追加 `shouldRestoreFriendCacheWhenUnblockExistingFriend`，初次失败：解除拉黑不会恢复仍互为好友的 Redis 好友缓存。

## 4. GREEN 命令

```bash
mvn -pl mallchat-service/mallchat-chat-service -am -Dtest=UserFriendServiceImplTest,UserFriendApplyServiceImplTest,ChatMessageServiceImplTest,ChatReportServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：77 个测试通过，0 failure，0 error。

## 5. 验收门禁

```bash
openspec validate --all --strict
git diff --check
mvn -pl mallchat-service/mallchat-chat-service -am test
```

结果：OpenSpec 26 项通过、0 failed；`git diff --check` 通过；chat-service 相关模块全量回归通过。

## 6. 残余风险

1. 本次不建设举报审核后台和封禁处置流，`chat_report` 只提供后续治理的数据入口。
2. 拉黑不改变群聊内既有发言权限，群内治理放到后续群治理任务。
3. 好友分组是轻量文本字段，不支持独立分组排序、批量移动和分组管理。
