## Why

P-004 已明确群聊管理需要支持群主维护群资料和群公告。当前后端已经具备群创建、邀请、详情、成员列表、退群和解散能力，但缺少一个受控的群资料更新入口，导致移动端聊天详情页无法让群主编辑群名称、头像或公告，也无法通过会话更新事件让成员侧刷新群资料。

本次变更先落地群资料更新的最小闭环，作为 P-004 群治理能力的第一步。它不扩展管理员、踢人、禁言或入群审核，避免把多个群治理能力混进同一个不可验收的大改动。

## What Changes

- 新增 `ChatRoomUpdateRequest`，用于承载群资料更新参数。
- 新增 `POST /chat/room/update`，仅允许群主更新群名称、群头像和群公告。
- `ChatRoomService` 新增 `updateGroupProfile` 服务方法。
- 更新 `chat_room` 与 `chat_group_info` 的群资料事实数据。
- 当 `chat_group_info` 缺失时，以 `chat_room` 当前群名和头像作为默认值创建扩展记录，避免公告单独更新导致群名或头像变空。
- 更新后向当前房间成员发送 `SESSION_UPDATE`，让客户端刷新会话中的群名称和头像。
- 补充 `ChatRoomServiceImplTest`，覆盖私聊拒绝、非群主拒绝、空更新拒绝、群主更新成功、扩展记录缺失时默认值保护和会话刷新推送。

## Capabilities

### New Capabilities

- `chat-room-access`: 群主可以更新群资料。
- `chat-session`: 群资料更新后成员会话资料刷新。

### Modified Capabilities

- `chat-room-access`: 在既有受控群聊模型内增加群资料编辑边界。

## Impact

- API:
  - `mallchat-api-chat` 新增 `ChatRoomUpdateRequest`。
- 代码:
  - `ChatRoomController`
  - `ChatRoomService`
  - `ChatRoomServiceImpl`
- 测试:
  - `ChatRoomServiceImplTest`
- 非目标:
  - 群管理员、入群审核、踢人、禁言、群昵称、群系统消息落库。
