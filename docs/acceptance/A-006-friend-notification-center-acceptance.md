---
layer: Acceptance
doc_no: "A-006"
audience:
  - PM
  - Dev
  - QA
feature_area: friend-notification-center
purpose: "记录好友申请和通过接入通知中心的验收结果、测试证据与剩余风险。"
canonical_path: "docs/acceptance/A-006-friend-notification-center-acceptance.md"
status: complete
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "../prd/P-008-im-notification-center-prd.md"
  - "../../openspec/changes/archive/2026-05-20-integrate-friend-notification-center"
outputs:
  - "好友申请通知中心接入验收结论"
triggers:
  - "integrate-friend-notification-center 完成后验收"
downstream:
  - "../../openspec/specs/chat-friend/spec.md"
  - "../../openspec/specs/im-product-mvp/spec.md"
---

# 好友申请通知中心接入验收

## 1. 变更验收范围

- 好友申请成功后，为目标用户创建 `user` 类型通知。
- 好友通过成功后，为申请人创建 `user` 类型通知。
- 通知 `relatedType` 固定为 `user_friend_apply`，`relatedId` 为好友申请 ID。
- 保留已有 `friend_apply:{id}` 和 `friend_approve:{id}` WebSocket 事件。
- notification-service 调用失败时只降级记录日志，不回滚好友申请、好友关系或私聊房间创建。

## 2. TDD 证据

| 阶段 | 命令 | 结果 | 结论 |
| --- | --- | --- | --- |
| 红灯 | `mvn -pl :mallchat-chat-service -am test -Dtest=UserFriendApplyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false` | 缺少 `notificationFeignClient` 字段，测试替身无法注入，7 个测试 setup 失败 | 通过 |
| 绿灯 | 同上 | `Tests run: 7, Failures: 0, Errors: 0` | 通过 |
| chat + notification 回归 | `mvn -pl mallchat-service/mallchat-chat-service,mallchat-service/mallchat-notification-service -am test` | `Tests run: 113, Failures: 0, Errors: 0` | 通过 |
| OpenSpec change | `openspec validate integrate-friend-notification-center --strict` | change 合法 | 通过 |
| OpenSpec 全量 | `openspec validate --all --strict` | `10 passed, 0 failed` | 通过 |
| OpenSpec 归档后全量 | `openspec validate --all --strict` | `9 passed, 0 failed` | 通过 |

## 3. 结论

本切片补齐了好友申请和好友通过的通知中心展示事实。实现复用 notification-service 已有 `addBusinessNotification` 接口，不新增通知类型、表结构、队列或前端协议；好友申请和通过仍以 `user_friend_apply`、好友关系和私聊房间为业务事实，通知只负责展示和未读计数。

## 4. 残余风险

1. 群审核和系统广播尚未接入通知中心，需要后续单独切片处理。
2. 当前通知文案为固定中文短文案，后续可按端侧展示需要补充申请人昵称或国际化。
3. Controller 未补 Web 层未登录集成测试，仍依赖现有鉴权与服务层单测保护。
