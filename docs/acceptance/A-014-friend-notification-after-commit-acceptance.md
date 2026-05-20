---
layer: Acceptance
doc_no: "A-014"
audience:
  - Dev
  - QA
feature_area: friend-notification-after-commit
purpose: "记录好友申请与好友通过通知 afterCommit 事务边界的验收范围、测试证据与剩余风险。"
canonical_path: "docs/acceptance/A-014-friend-notification-after-commit-acceptance.md"
status: complete
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "../prd/P-003-im-friendship-discovery-prd.md"
  - "../prd/P-001-im-real-time-communication-prd.md"
  - "../../openspec/changes/archive/2026-05-20-harden-friend-notification-after-commit"
outputs:
  - "好友申请与通过通知 afterCommit 验收结论"
triggers:
  - "harden-friend-notification-after-commit 完成后验收"
downstream:
  - "../../openspec/specs/chat-friend/spec.md"
---

# 好友申请与通过通知 afterCommit 验收

## 1. 变更验收范围

- 好友申请通知在事务同步存在时，提交前不创建 notification-service 展示事实。
- 好友申请通知在事务同步存在时，事务提交后通过 `afterCommit` 创建通知中心记录。
- 好友通过通知在事务同步存在时，提交前不创建 notification-service 展示事实。
- 好友通过通知在事务同步存在时，事务提交后通过 `afterCommit` 创建通知中心记录。
- 无事务同步时保持原有立即发送行为，通知失败仍只记录 warn 并降级。
- 本阶段不新增 outbox、MQ 重试表、事件表、新通知通道或新 HTTP API。

## 2. TDD 证据

| 阶段 | 命令 | 结果 | 结论 |
| --- | --- | --- | --- |
| OpenSpec change | `openspec validate harden-friend-notification-after-commit --strict` | change 合法 | 通过 |
| 红灯 | `mvn -pl :mallchat-chat-service -am test -Dtest=UserFriendApplyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false` | `2 failures`，好友申请/通过通知在提交前已发送 | 通过 |
| 绿灯 | 同上 | `Tests run: 11, Failures: 0, Errors: 0` | 通过 |
| chat-service 回归 | `mvn -pl :mallchat-chat-service -am test` | `Tests run: 150, Failures: 0, Errors: 0` | 通过 |
| OpenSpec 全量 | `openspec validate --all --strict` | `10 passed, 0 failed` | 通过 |
| 测试验证人复核 | 子智能体“测试验证人/Code Reviewer”只读审核 | 无 P0/P1；确认复用群邀请 afterCommit 风格且未引入 outbox、重试表、事件表 | 通过 |
| 归档后 OpenSpec 全量 | `openspec validate --all --strict` | `9 passed, 0 failed` | 通过 |
| diff 检查 | `git diff --check` | 无空白错误 | 通过 |

## 3. 结论

本切片将好友申请和好友通过通知中心写入调整为事务提交后执行。事务同步存在时，notification-service 的展示事实不会早于本地好友申请、好友关系或私聊房间事实提交；无事务同步时仍保持即时发送，符合当前服务的最小实现风格。

## 4. 残余风险

1. 本阶段不做通知发送失败重试，提交后 notification-service 仍可能不可用，只按现有 warn 降级。
2. `applyFriend` 当前方法本身未标注事务，但测试覆盖了事务同步存在时的行为；若未来外层事务调用该方法，也会自动延迟通知发送。
