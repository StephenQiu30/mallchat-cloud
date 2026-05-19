## Context

当前链路为：

1. `ChatMessageServiceImpl` 完成权限校验和消息落库。
2. `ChatMqProducer` 构造 `WebSocketMessage`，以 `CHAT_MESSAGE_PUSH` 发送到 RabbitMQ。
3. `ChatMessagePushHandler` 消费后，如果 `pushType=broadcast` 且存在 `roomId`，从 `ChatCacheConstant.getRoomMemberKey(roomId)` 读取 Redis set。
4. Redis set 为空时直接跳过推送。

`ChatRoomMemberServiceImpl` 在 chat-service 内具备数据库回源能力，但 notification-service 不是 chat 领域事实服务，不应为了本次小修复直接读 chat 表或复制 chat 成员模型。

## Decision

采用“生产端携带成员快照，消费端缓存缺失兜底”的最小修复：

1. `ChatMqProducer` 的房间事件保留 `pushType=broadcast` 和 `roomId`，继续使用 `CHAT_MESSAGE_PUSH`。
2. `WebSocketMessage.userIds` 用作房间事件的成员快照字段，不新增包装模型。
3. `ChatMessageServiceImpl` 在发送消息、已读、撤回这类房间事件前，通过 `chatRoomMemberService.listByRoomId(roomId)` 取得数据库成员列表并传给 producer。
4. `ChatMessagePushHandler` 房间推送时先读 Redis 成员缓存；缓存存在时优先使用缓存，保证成员变更后的在线推送尽量使用最新缓存。
5. Redis 缓存为空时，如果 `wsMessage.userIds` 非空，则使用成员快照推送。
6. Redis 缓存为空且成员快照也为空时，保留 warn 并跳过；这是数据异常或旧消息兼容场景，不在本 change 内做跨服务回源。

## Alternatives Considered

### notification-service 通过 Feign 调 chat-service 查询成员

该方案能在消费时拿到当前成员事实，但当前 `ChatRoomController#listRoomMembers` 是用户态接口，依赖登录用户权限；新增内部接口需要额外鉴权和接口契约，不适合本次小切片。

### notification-service 直接依赖 chat-service mapper 或实体读库

这会破坏服务边界，让 notification-service 复制 chat 领域事实模型，后续维护成本高。

### 只在 notification-service 继续依赖 Redis

这无法解决冷缓存或缓存失效漏推，是当前缺陷本身。

## Testing Strategy

- 先新增失败测试：
  - `ChatMessagePushHandlerTest`：房间成员缓存为空但 `WebSocketMessage.userIds` 存在时，应推送给快照成员。
  - `ChatMqProducerTest`：房间广播事件携带生产端提供的成员快照。
  - `ChatMessageServiceImplTest`：发送群消息时从 `ChatRoomMemberService` 读取成员并传入 producer。
- 实现后运行：
  - `mvn -pl mallchat-service/mallchat-notification-service -am -Dtest=ChatMessagePushHandlerTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - `mvn -pl mallchat-service/mallchat-chat-service -am -Dtest=ChatMqProducerTest,ChatMessageServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - `openspec validate harden-room-realtime-push-on-cache-miss --strict`
  - `openspec validate --all --strict`

## Non-goals

- 不新增消息离线补偿游标。
- 不处理 RabbitMQ 重复消费幂等。
- 不新增独立实时投递服务。
- 不改变 WebSocket 客户端协议。
- 不调整好友、群治理、富消息、动态 feed 或通知中心聚合。
