## Why

P-004 群聊管理 PRD 在群资料更新之后，下一步需要补齐基础群治理能力。当前后端已经支持成员主动退群和群主解散群聊，但缺少群主将违规或误拉成员移出群聊的受控入口，导致 QQ-like 群聊管理无法形成最小治理闭环。

本次变更只实现“群主移除普通成员”的最小能力，不引入管理员、禁言、入群审核或群系统消息落库，避免扩大 P-004 的待确认范围。

## What Changes

- 新增群成员移除请求 DTO，承载 `roomId` 与 `memberId`。
- 新增 `POST /chat/room/member/remove`，仅群主可调用。
- `ChatRoomService` 新增 `removeMember` 服务方法。
- 服务层校验：
  - 房间必须存在且为群聊。
  - 操作者必须是群主。
  - 目标成员必须在当前群聊中。
  - 群主不能通过该接口移除自己或移除群主账号。
- 成功移除后复用 `ChatRoomMemberService#leaveRoom`，同步数据库和成员缓存。
- 成功移除后删除被移除成员的会话，并发送会话删除事件，避免客户端继续保留可进入的群会话入口。

## Capabilities

### New Capabilities

- `chat-room-access`: 群主可移除群内普通成员。
- `chat-session`: 成员被移除后对应会话应删除并通知客户端刷新。

### Modified Capabilities

- `chat-room-access`: 在受控群聊模型中增加群主治理路径。

## Impact

- API:
  - `mallchat-api-chat` 新增群成员移除 DTO。
- 代码:
  - `ChatRoomController`
  - `ChatRoomService`
  - `ChatRoomServiceImpl`
- 测试:
  - `ChatRoomServiceImplTest`
- 非目标:
  - 群管理员、转让群主、禁言、群昵称、入群审核、群系统消息。
