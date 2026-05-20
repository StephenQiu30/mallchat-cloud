---
layer: Acceptance
doc_no: "A-010"
audience:
  - Dev
  - QA
feature_area: group-dismiss-push-degradation
purpose: "记录群解散会话删除推送失败降级的验收范围、测试证据与剩余风险。"
canonical_path: "docs/acceptance/A-010-group-dismiss-push-degradation-acceptance.md"
status: complete
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "../prd/P-004-group-chat-management-prd.md"
  - "../prd/P-002-im-realtime-delivery-reliability-prd.md"
  - "../../openspec/changes/archive/2026-05-20-harden-group-dismiss-push-degradation"
outputs:
  - "群解散推送失败降级验收结论"
triggers:
  - "harden-group-dismiss-push-degradation 完成后验收"
downstream:
  - "../../openspec/specs/chat-room-access/spec.md"
---

# 群解散推送失败降级验收

## 1. 变更验收范围

- 群主解散群聊时，单个成员 `sendSessionDelete` 推送失败不回滚解散事务。
- 解散流程仍删除群成员关系。
- 解散流程仍删除该群聊下的会话记录。
- 解散流程仍删除群扩展资料和群房间记录。
- 本阶段不新增解散通知中心、MQ 重试表、群操作审计或群主转让能力。

## 2. TDD 证据

| 阶段 | 命令 | 结果 | 结论 |
| --- | --- | --- | --- |
| OpenSpec change | `openspec validate harden-group-dismiss-push-degradation --strict` | change 合法 | 通过 |
| 红灯 | `mvn -pl :mallchat-chat-service -am test -Dtest=ChatRoomServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false` | `1 failure`，`sendSessionDelete` 异常中断群解散 | 通过 |
| 绿灯 | 同上 | `Tests run: 28, Failures: 0, Errors: 0` | 通过 |
| 测试验证人复核 | 子智能体“测试验证人/Code Reviewer”只读审核 | 无 P0/P1，建议补单个成员失败后继续尝试其他成员 | 已补充 |
| 补充测试绿灯 | 同上 | `Tests run: 28, Failures: 0, Errors: 0` | 通过 |
| chat-service 回归 | `mvn -pl :mallchat-chat-service -am test` | `Tests run: 137, Failures: 0, Errors: 0` | 通过 |
| OpenSpec 全量 | `openspec validate --all --strict` | `10 passed, 0 failed` | 通过 |
| 归档后 OpenSpec 全量 | `openspec validate --all --strict` | `9 passed, 0 failed` | 通过 |
| diff 检查 | `git diff --check` | 无空白错误 | 通过 |

## 3. 结论

本切片将群解散的会话删除实时推送调整为降级处理：推送失败只记录日志，不阻断核心业务事实。客户端后续仍可通过会话列表和房间事实补偿最终状态，符合实时 IM 中“事实优先、推送可降级”的后端可靠性边界。

## 4. 残余风险

1. 本阶段不做 MQ 失败重试，短时间在线端可能需要刷新或重连后才能看到解散后的最终状态。
2. 群解散没有单独通知中心记录，如需离线通知或操作审计，应另拆 P-008/P-004 后续切片。
