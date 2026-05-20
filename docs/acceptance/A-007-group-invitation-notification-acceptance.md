---
layer: Acceptance
doc_no: "A-007"
audience:
  - PM
  - Dev
  - QA
feature_area: group-invitation-notification
purpose: "记录群邀请接入通知中心的验收结果、测试证据与剩余风险。"
canonical_path: "docs/acceptance/A-007-group-invitation-notification-acceptance.md"
status: complete
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "../prd/P-004-group-chat-management-prd.md"
  - "../prd/P-008-im-notification-center-prd.md"
  - "../../openspec/changes/archive/2026-05-20-integrate-group-invitation-notification"
outputs:
  - "群邀请通知中心接入验收结论"
triggers:
  - "integrate-group-invitation-notification 完成后验收"
downstream:
  - "../../openspec/specs/chat-room-access/spec.md"
  - "../../openspec/specs/im-product-mvp/spec.md"
---

# 群邀请通知中心接入验收

## 1. 变更验收范围

- 创建群聊时，为初始受邀成员创建 `user` 类型通知。
- 已有群聊邀请好友入群时，为被邀请人创建 `user` 类型通知。
- 通知 `relatedType` 固定为 `chat_room`，`relatedId` 为群房间 ID。
- 保留已有 `session_join:{roomId}:{userId}` 和 `session_invite:{roomId}:{userId}` 会话刷新事件。
- notification-service 调用失败时只降级记录日志，不回滚群成员、会话或 session update 事实。

## 2. TDD 证据

| 阶段 | 命令 | 结果 | 结论 |
| --- | --- | --- | --- |
| 红灯 | `mvn -pl :mallchat-chat-service -am test -Dtest=ChatRoomServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false` | 新增 3 个通知断言失败，通知数量为 0 | 通过 |
| 绿灯 | 同上 | `Tests run: 25, Failures: 0, Errors: 0` | 通过 |
| chat + notification 回归 | `mvn -pl mallchat-service/mallchat-chat-service,mallchat-service/mallchat-notification-service -am test` | `Tests run: 116, Failures: 0, Errors: 0` | 通过 |
| OpenSpec change | `openspec validate integrate-group-invitation-notification --strict` | change 合法 | 通过 |
| OpenSpec 全量 | `openspec validate --all --strict` | `10 passed, 0 failed` | 通过 |
| OpenSpec 归档后全量 | `openspec validate --all --strict` | `9 passed, 0 failed` | 通过 |

## 3. 结论

本切片补齐了直接群邀请的通知中心展示事实。实现复用 notification-service 已有 `addBusinessNotification` 接口，不新增群邀请审批状态、不新增通知类型、表结构、队列或前端协议；群成员关系和会话仍由 `chat_room_member` 与 `chat_session` 维护，通知只负责展示和未读计数。

## 4. 残余风险

1. 当前仍是“邀请好友即直接入群”，不支持群主审批或成员确认。
2. 群解散、踢人、群公告变更和系统广播尚未接入通知中心。
3. 当前通知文案为固定中文短文案，后续可按端侧展示需要补充邀请人昵称或国际化。
4. Controller 未补 Web 层未登录集成测试，仍依赖现有鉴权与服务层单测保护。
