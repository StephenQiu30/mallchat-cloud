---
layer: Acceptance
doc_no: "A-012"
audience:
  - Dev
  - QA
feature_area: message-flow-push-degradation
purpose: "记录消息发送、已读上报、消息撤回推送失败降级的验收范围、测试证据与剩余风险。"
canonical_path: "docs/acceptance/A-012-message-flow-push-degradation-acceptance.md"
status: complete
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "../prd/P-002-im-realtime-delivery-reliability-prd.md"
  - "../prd/P-001-im-real-time-communication-prd.md"
  - "../../openspec/changes/archive/2026-05-20-harden-message-flow-push-degradation"
outputs:
  - "消息主链路推送失败降级验收结论"
triggers:
  - "harden-message-flow-push-degradation 完成后验收"
downstream:
  - "../../openspec/specs/chat-message/spec.md"
  - "../../openspec/specs/chat-session/spec.md"
---

# 消息主链路推送失败降级验收

## 1. 变更验收范围

- 消息落库成功后，聊天消息实时推送失败不回滚消息事实。
- 已读边界和未读数更新成功后，已读事件推送失败不回滚已读事实。
- 已读边界和未读数更新成功后，会话刷新推送失败不回滚已读事实。
- 消息撤回状态更新成功后，撤回事件推送失败不回滚撤回事实。
- 消息撤回状态更新成功后，单个成员会话刷新推送失败不阻断其他成员刷新尝试。
- 消息发送事件触发会话事实批量更新后，单个成员会话刷新推送失败不阻断发送流程或其他成员刷新尝试。
- 本阶段不新增 outbox、MQ 重试表、离线补偿、通知中心记录或新 HTTP API。

## 2. TDD 证据

| 阶段 | 命令 | 结果 | 结论 |
| --- | --- | --- | --- |
| OpenSpec change | `openspec validate harden-message-flow-push-degradation --strict` | change 合法 | 通过 |
| 红灯 | `mvn -pl :mallchat-chat-service -am test -Dtest=ChatMessageServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false` | `5 failures`，消息发送、已读、撤回相关推送异常均中断业务操作 | 通过 |
| 绿灯 | 同上 | `Tests run: 35, Failures: 0, Errors: 0` | 通过 |
| 测试验证人复核 | 子智能体“测试验证人/Code Reviewer”只读审核 | 无 P0/P1；P2 指出消息发送同步监听器会话刷新推送仍可能冒泡 | 已补红灯测试并修复 |
| 监听器红灯 | `mvn -pl :mallchat-chat-service -am test -Dtest=ChatSessionListenerTest -Dsurefire.failIfNoSpecifiedTests=false` | `1 failure`，会话刷新推送异常从监听器冒泡 | 通过 |
| 监听器+消息服务绿灯 | `mvn -pl :mallchat-chat-service -am test -Dtest=ChatSessionListenerTest,ChatMessageServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false` | `Tests run: 36, Failures: 0, Errors: 0` | 通过 |
| chat-service 回归 | `mvn -pl :mallchat-chat-service -am test` | `Tests run: 146, Failures: 0, Errors: 0` | 通过 |
| OpenSpec 全量 | `openspec validate --all --strict` | `10 passed, 0 failed` | 通过 |
| 归档后 OpenSpec 全量 | `openspec validate --all --strict` | `9 passed, 0 failed` | 通过 |
| diff 检查 | `git diff --check` | 无空白错误 | 通过 |

## 3. 结论

本切片将消息发送、已读上报、消息撤回中的实时推送调整为降级处理。MQ/WebSocket 抖动时，服务端仍保留 `chat_message`、成员已读边界、会话未读数和撤回状态等权威事实，客户端可通过历史消息、会话列表和重连补偿获得最终状态。

## 4. 残余风险

1. 本阶段不做 MQ 失败重试，在线端可能需要刷新或重连后才能看到最终状态。
2. 本阶段不改变 `ChatMessageSentEvent` 的同步发布方式；如果后续发现事件监听器异常会影响发送链路，应另拆事件发布降级或事务后事件切片。
