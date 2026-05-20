---
layer: Acceptance
doc_no: "A-013"
audience:
  - Dev
  - QA
feature_area: friend-apply-push-degradation
purpose: "记录好友申请与好友通过推送失败降级的验收范围、测试证据与剩余风险。"
canonical_path: "docs/acceptance/A-013-friend-apply-push-degradation-acceptance.md"
status: complete
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "../prd/P-003-im-friendship-discovery-prd.md"
  - "../prd/P-001-im-real-time-communication-prd.md"
  - "../../openspec/changes/archive/2026-05-20-harden-friend-apply-push-degradation"
outputs:
  - "好友申请与通过推送失败降级验收结论"
triggers:
  - "harden-friend-apply-push-degradation 完成后验收"
downstream:
  - "../../openspec/specs/chat-friend/spec.md"
---

# 好友申请与通过推送失败降级验收

## 1. 变更验收范围

- 好友申请保存成功后，好友申请 WebSocket/MQ 推送失败不回滚申请事实。
- 好友申请推送失败后，仍继续尝试创建通知中心记录。
- 好友通过时，好友关系和私聊房间初始化完成后，好友通过 WebSocket/MQ 推送失败不回滚业务事实。
- 好友通过推送失败后，仍继续尝试创建通知中心记录，并保持原有申请状态更新流程。
- 本阶段不新增 outbox、MQ 重试表、离线补偿、审批模型、新通知通道或新 HTTP API。

## 2. TDD 证据

| 阶段 | 命令 | 结果 | 结论 |
| --- | --- | --- | --- |
| OpenSpec change | `openspec validate harden-friend-apply-push-degradation --strict` | change 合法 | 通过 |
| 红灯 | `mvn -pl :mallchat-chat-service -am test -Dtest=UserFriendApplyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false` | `2 failures`，好友申请/通过推送异常均中断业务操作 | 通过 |
| 绿灯 | 同上 | `Tests run: 9, Failures: 0, Errors: 0` | 通过 |
| chat-service 回归 | `mvn -pl :mallchat-chat-service -am test` | `Tests run: 148, Failures: 0, Errors: 0` | 通过 |
| OpenSpec 全量 | `openspec validate --all --strict` | `10 passed, 0 failed` | 通过 |
| 测试验证人复核 | 子智能体“测试验证人/Code Reviewer”只读审核 | 无 P0/P1；确认实现未引入 outbox、重试表、补偿任务或额外架构 | 通过 |
| 归档后 OpenSpec 全量 | `openspec validate --all --strict` | `9 passed, 0 failed` | 通过 |
| diff 检查 | `git diff --check` | 无空白错误 | 通过 |

## 3. 结论

本切片将好友申请和好友通过中的实时推送调整为降级处理。MQ/WebSocket 抖动时，服务端仍保留 `user_friend_apply`、双向好友关系和私聊房间等权威事实，并继续尝试写入通知中心记录。在线提醒失败只影响即时展示，不影响后续列表查询和业务结果。

## 4. 残余风险

1. 本阶段不做 MQ 失败重试，在线端可能需要刷新好友申请列表或通知中心后才能看到最终状态。
2. 好友通过流程目前仍保持原有“发送实时事件和通知后再 `updateById`”的顺序；本切片只处理推送失败降级，不改变既有事务顺序。
