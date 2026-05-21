---
layer: Plan
doc_no: "PL-002"
audience:
  - PM
  - Dev
  - QA
feature_area: friend-discovery-and-relationship
purpose: "记录 Phase9 好友发现与关系状态增强的执行拆解、交付边界与验收门禁。"
canonical_path: "docs/plans/PL-002-friend-discovery-phase9-plan.md"
status: complete
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "../prd/P-003-friend-discovery-and-relationship-prd.md"
  - "../../openspec/changes/enhance-friend-discovery-relationship"
outputs:
  - "好友发现关系状态能力实现"
  - "OpenSpec 归档与验收材料"
triggers:
  - "执行 chat-friend 关系增强与高可用复核"
downstream:
  - "../prd/P-003-friend-discovery-and-relationship-prd.md"
  - "openspec/changes/archive/2026-05-19-enhance-friend-discovery-relationship"
---

# Phase9 好友发现与关系状态增强计划

## 1. 范围边界

本次仅覆盖：
- 用户发现分页查询返回关系状态快照；
- 好友关系删除接口；
- 关系状态统一计算（本人/陌生人/已是好友/待处理方向）；
- 对应 OpenSpec 与 TDD 验收闭环。

## 2. 不做事项

- 不新增好友备注、拉黑、分组、推荐算法；
- 不开启 `/chat/friend/add` 为直接建边行为；
- 不重构 `user_friend_apply` 状态机。

## 3. 任务拆解

1. OpenSpec 与自审
   - 完成 `proposal/design/tasks` 与 `spec/chat-friend` delta；
   - 通过 `openspec validate`。
2. 服务能力实现
   - `ChatFriendUserVO` 增加 `friendStatus`；
   - `UserFriendService` 增加 `getFriendshipStatus/searchFriends/removeFriend` 场景对齐；
   - `ChatFriendController` 增加 `GET /friend/search` 与 `DELETE /friend/delete`。
3. 关系状态与分页边界验证
   - 验证本人、好友、待处理方向、陌生、非法页长；
   - 验证 search 请求排除当前用户、在线状态回填。
4. 归档与验收
   - 通过 service 测试与 openspec 全量校验；
   - 提交验收文档。

## 4. 验收门禁

- `mvn -pl :mallchat-chat-service -am test -Dtest=UserFriendServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
- `openspec validate --all --strict`
- `openspec archive enhance-friend-discovery-relationship -y`
- `openspec validate --all --strict`（归档后复验）

## 5. 交付说明

- 变更文件统一在 `mallchat-api-chat`、`mallchat-chat-service` 与 `openspec` 相关目录；
- 按 OpenSpec close-the-loop 规则完成归档后再提交；
- 保留后续迭代议题：控制器级契约测试与缓存脏数据退化场景。
