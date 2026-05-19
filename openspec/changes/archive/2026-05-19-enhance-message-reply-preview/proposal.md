## Why

P-005 富消息与媒体消息 PRD 已指出 `chat_message.reply_msg_id` 是引用回复的基础槽位。当前发送消息路径已经带有 `replyMsgId` 字段和同房间校验，但引用预览的契约还不够稳定：缺少测试约束，也没有在 `ReplyMsgVO.userName` 中返回被引用消息发送者名称。

本次变更只补强“引用回复消息”最小闭环，不实现转发、合并转发、语音、视频、表情或收藏。

## What Changes

- 固化引用回复发送规则：
  - 被回复消息必须存在。
  - 被回复消息必须属于同一房间。
  - 发送者仍必须满足房间发送权限。
- 固化引用回复展示规则：
  - 消息 VO 包含 `replyMsg` 简要信息。
  - 被回复消息正常时返回预览内容和发送者名称。
  - 被回复消息已撤回时，引用预览内容脱敏为“该消息已被撤回”。
- 补充 `ChatMessageServiceImplTest`，覆盖同房间引用、跨房间拒绝、撤回脱敏和引用发送者名称。

## Capabilities

### Modified Capabilities

- `chat-message`: 增强引用回复消息契约与测试覆盖。

## Impact

- 代码：
  - `ChatMessageServiceImpl`
- 测试：
  - `ChatMessageServiceImplTest`
- 非目标：
  - 转发、合并转发、语音、视频、表情、位置、名片、收藏。
