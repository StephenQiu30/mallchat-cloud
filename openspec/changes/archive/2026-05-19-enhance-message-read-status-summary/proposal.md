## Why

P-006 已读回执与消息状态 PRD 指出，当前后端只有 `markMessageRead` 读边界上报和会话未读数更新，缺少发送者可查询的消息级已读/未读统计。QQ-like IM 的基础体验需要让消息发送者知道一条消息当前是否被对方或群成员读到，但 P-006 中“成员级名单谁可见、是否允许关闭回执、送达状态是否进入 P1”仍待确认。

本次变更只实现隐私更稳的最小闭环：消息发送者查询单条消息在当前房间成员中的聚合已读摘要，不返回成员名单，不新增送达状态，不新增数据表。

## What Changes

- 新增消息已读统计摘要查询：
  - 当前登录用户必须是房间成员。
  - 被查询消息必须存在且属于该房间。
  - 仅消息发送者可查询该消息的聚合已读摘要。
  - 返回 `roomId`、`messageId`、`totalCount`、`readCount`、`unreadCount`。
- 统计来源：
  - 使用当前房间成员的 `lastReadMessageId` 作为已读事实来源。
  - 消息发送者视为已读成员。
- 非目标：
  - 不返回已读/未读成员名单。
  - 不实现送达状态、发送失败重试或多端发送态。
  - 不新增 `message_read_receipt` 表。
  - 不改变现有 `markMessageRead` 上报和实时 `MESSAGE_READ` 事件。

## Capabilities

### Modified Capabilities

- `chat-message`: 增加消息级已读统计摘要查询契约。

## Impact

- 代码：
  - `ChatMessageController`
  - `ChatMessageService`
  - `ChatMessageServiceImpl`
  - `ChatFeignClient`
  - 新增 `ChatMessageReadStatusVO`
- 测试：
  - `ChatMessageServiceImplTest`
- 非目标：
  - 成员级明细列表、送达状态、消息失败重试、回执开关。
