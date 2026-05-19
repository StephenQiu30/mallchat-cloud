---
layer: PRD
doc_no: "P-006"
audience:
  - PM
  - Dev
  - QA
  - Ops
feature_area: read-receipt-and-message-state
purpose: "定义 MallChat 已读回执、未读统计、送达状态和消息状态查询的后端增强边界。"
canonical_path: "docs/prd/P-006-read-receipt-and-message-state-prd.md"
status: draft
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "docs/prd/P-001-im-real-time-communication-prd.md"
  - "openspec/specs/chat-message/spec.md"
  - "openspec/specs/chat-session/spec.md"
outputs:
  - "已读回执与消息状态增强需求"
  - "后续 OpenSpec change: enhance-read-receipts-message-state"
triggers:
  - "新增已读人数、未读名单、送达状态或消息状态查询"
downstream:
  - "docs/design/"
  - "docs/acceptance/"
---

# 已读回执与消息状态 PRD

## 1. 背景

当前后端支持 `markMessageRead`，会更新房间成员和会话的最后已读消息 ID，并向房间广播 `MESSAGE_READ` 事件。会话表存储 `unread_count` 和 `last_read_message_id`。

但 QQ-like IM 中常见的已读回执还包括单聊“已读/未读”、群聊“已读人数/未读人数”、成员级已读名单、送达状态和消息发送状态。当前未发现对应查询接口和数据模型。

## 2. 产品目标

1. MVP 保留读边界和未读数，保证会话列表准确。
2. P1 支持单聊和群聊的成员级已读回执查询。
3. P2 支持送达状态、发送失败重试和多端状态一致性。

```gherkin
Given 群聊中有 10 名成员
When 其中 6 名成员读到消息 M
Then 发送者应能查询消息 M 的已读人数为 6
And 未读成员列表应只对有权限的用户展示
```

## 3. 非目标

- 不在首版实时推送每个成员的详细读列表。
- 不为超大群提供无限制已读名单查询。
- 不把客户端本地发送态当作服务端送达态。

## 4. 核心用户故事

### 4.1 普通聊天用户

作为聊天用户，我希望读过消息后未读数被清空或减少。

验收标准：
- 已读上报必须校验用户是房间成员。
- 旧读边界不能覆盖新读边界。
- 部分已读时，较新的未读消息仍保留未读数。

### 4.2 消息发送者

作为消息发送者，我希望知道消息是否被对方读到。

验收标准：
- 单聊可以展示对方是否已读。
- 群聊可以展示已读人数和未读人数。
- 查询已读详情需校验房间权限。

### 4.3 QA 和运维

作为 QA，我希望消息状态有可测试的状态机。

验收标准：
- 服务端状态至少区分正常、撤回、删除。
- 送达、已读、失败重试等状态进入 P1/P2 时需单独定义。

## 5. 数据与权限边界

- `chat_room_member.last_read_message_id` 可作为读边界基础。
- `chat_session.unread_count` 是会话展示缓存，不应作为唯一回执事实来源。
- 群聊已读详情可能需要限制分页、权限和数据保留周期。

## 6. 首版验收门禁

- 已读边界、未读数重算、旧边界忽略均有测试覆盖。
- P1 新增已读详情时，需要覆盖单聊、群聊、非成员访问、撤回消息场景。
- 已读事件与会话更新事件应在客户端可区分。

## 7. 风险与边界

- 群聊成员很多时，实时计算已读/未读列表可能有性能风险。
- 如果未来支持消息删除，删除与已读回执的展示关系需要单独确认。

## 8. 待确认问题

- 群聊已读详情是否所有成员可见，还是仅发送者可见？
- 已读回执是否允许关闭？
- 消息送达状态是否进入 P1？

## 9. 变更记录

| 日期 | 作者 | 版本 | 变更说明 |
| --- | --- | --- | --- |
| 2026-05-19 | StephenQiu30 | 0.1.0 | 初始化已读回执与消息状态 PRD |
