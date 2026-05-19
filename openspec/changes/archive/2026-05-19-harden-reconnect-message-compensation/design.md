# Design: Reconnect Message Compensation

## Current State
- `ChatMessageServiceImpl#listHistoryMessages` 使用 `id < lastMessageId` 查询更早历史消息，适合上滑翻页，不适合断线后补偿新增消息。
- `ChatSession` 实体包含 `lastMessageId` 与 `lastReadMessageId`，但 `ChatSessionVO` 未暴露这两个字段。
- `markMessageRead` 持久化的是阅读边界，不是客户端已收到消息边界。

## Proposed Approach
新增独立补偿查询：

```text
GET /api/chat/message/list/after/vo?roomId={roomId}&afterMessageId={messageId}&limit={limit}
```

行为：
- `roomId` 和当前登录用户必填。
- `afterMessageId` 可空或小于等于 0，此时返回房间内最新窗口内的消息，按 ID 升序输出。
- `limit` 默认 100，最大 200，所有传入值先归一化为安全整数再拼接 MyBatis Plus `last("limit ...")`。
- 先调用 `chatRoomMemberService.isMember(roomId, userId)` 校验访问权限，再查询 `room_id = roomId` 且 `id > afterMessageId` 的消息。
- 返回 `ChatMessageVO`，复用现有发送者与回复消息组装逻辑。

会话列表：
- 在 `ChatSessionVO` 增加 `lastMessageId` 和 `lastReadMessageId` 字段。
- 保持 `ChatSessionConvert` 使用 `BeanUtils.copyProperties` 的现有风格。

## Cursor Semantics
- `afterMessageId` 表示“客户端最后成功接收或持久化的消息 ID”。
- `lastReadMessageId` 表示“用户已读到的消息 ID”，只能用于未读/已读状态，不能作为补偿游标。
- `lastMessageId` 表示服务端会话最新消息 ID，可用于客户端判断是否需要补偿。

## Test Strategy
- 在 `ChatMessageServiceImplTest` 先写失败测试：
  - 成员按 `afterMessageId` 查询新增消息时按 ID 升序返回。
  - 非成员查询补偿消息被拒绝。
  - 无新增消息返回空列表。
  - limit 归一化为默认值或最大值。
- 新增 `ChatSessionConvertTest`，验证实体游标字段映射到 VO。

## Non-goals
- 不改变既有历史翻页接口语义。
- 不新增客户端状态表或服务端 per-device cursor。
- 不处理退群前历史可见性策略；本轮只保证当前成员才能补偿当前房间消息。
