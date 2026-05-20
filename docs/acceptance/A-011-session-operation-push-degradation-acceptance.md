---
layer: Acceptance
doc_no: "A-011"
audience:
  - Dev
  - QA
feature_area: session-operation-push-degradation
purpose: "记录退群、会话置顶、会话删除推送失败降级的验收范围、测试证据与剩余风险。"
canonical_path: "docs/acceptance/A-011-session-operation-push-degradation-acceptance.md"
status: complete
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "../prd/P-002-im-realtime-delivery-reliability-prd.md"
  - "../prd/P-004-group-chat-management-prd.md"
  - "../../openspec/changes/archive/2026-05-20-harden-session-operation-push-degradation"
outputs:
  - "退群与会话操作推送失败降级验收结论"
triggers:
  - "harden-session-operation-push-degradation 完成后验收"
downstream:
  - "../../openspec/specs/chat-room-access/spec.md"
  - "../../openspec/specs/chat-session/spec.md"
---

# 退群与会话操作推送失败降级验收

## 1. 变更验收范围

- 普通成员退群后，`sendSessionDelete` 推送失败不回滚成员离开和会话删除事实。
- 会话置顶状态更新后，`sendSessionUpdate` 推送失败不回滚置顶状态。
- 会话删除后，`sendSessionDelete` 推送失败不回滚删除结果。
- 本阶段不新增 MQ 重试表、通知中心记录、会话操作审计、免打扰、多端同步策略或新 HTTP API。

## 2. TDD 证据

| 阶段 | 命令 | 结果 | 结论 |
| --- | --- | --- | --- |
| OpenSpec change | `openspec validate harden-session-operation-push-degradation --strict` | change 合法 | 通过 |
| 红灯 | `mvn -pl :mallchat-chat-service -am test -Dtest=ChatRoomServiceImplTest,ChatSessionServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false` | `3 failures`，退群、置顶、删除会话均被 MQ 推送异常中断 | 通过 |
| 绿灯 | 同上 | `Tests run: 36, Failures: 0, Errors: 0` | 通过 |
| chat-service 回归 | `mvn -pl :mallchat-chat-service -am test` | `Tests run: 140, Failures: 0, Errors: 0` | 通过 |
| OpenSpec 全量 | `openspec validate --all --strict` | `10 passed, 0 failed` | 通过 |
| 测试验证人复核 | 子智能体“测试验证人/Code Reviewer”只读审核 | 无 P0/P1；P2 提醒归档前验收文档不应引用不存在的 archive 路径 | 已通过归档闭环修复 |
| 归档后 OpenSpec 全量 | `openspec validate --all --strict` | `9 passed, 0 failed` | 通过 |
| diff 检查 | `git diff --check` | 无空白错误 | 通过 |

## 3. 结论

本切片将退群、置顶、删除会话三类“事实已写入、在线端需刷新”的实时推送调整为降级处理。MQ/WebSocket 抖动时，服务端仍保留权威业务事实，客户端可通过会话列表、成员关系和重连补偿获得最终状态，符合当前 MallChat 后端“业务事实优先，实时推送可降级”的实现风格。

## 4. 残余风险

1. 本阶段不做 MQ 失败重试，在线端可能需要主动刷新或重连后才能看到最终状态。
2. 本阶段不处理多端会话同步策略，后续如需更细粒度的设备级确认，应另拆会话同步增强切片。
