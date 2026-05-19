## Why

MallChat 的房间消息、撤回和已读事件会通过 `CHAT_MESSAGE_PUSH` 进入 notification-service，再由 WebSocket 推送给本机在线成员。当前 `ChatMessagePushHandler` 只从 Redis 房间成员缓存读取接收人；当 Redis 冷启动、缓存过期或被清理时，处理器会记录“房间缓存中没有成员，跳过推送”并直接返回。消息事实已经落库，但实时链路会漏推，后续只能依赖客户端拉取历史消息兜底，实时 IM 体验不稳定。

## What Changes

- 新增 `chat-realtime-delivery` 能力规格，约束房间实时事件不能只依赖 Redis 成员缓存。
- chat-service 发送房间实时事件时，将数据库成员快照写入现有 `WebSocketMessage.userIds`。
- notification-service 处理房间广播时优先使用 Redis 成员缓存；缓存为空时使用消息中的成员快照兜底，不再直接跳过推送。
- 用 TDD 覆盖缓存命中、缓存缺失兜底、MQ 生产端携带成员快照和业务服务传递成员快照。
- 不新增 RabbitMQ 类型、不改 WebSocket 事件外层格式、不处理离线补偿游标或 MQ 去重。

## Capabilities

### New Capabilities

- `chat-realtime-delivery`: 房间实时事件的接收人事实来源、缓存降级和推送不丢失契约。

### Modified Capabilities

- 无。本 change 只新增实时投递可靠性能力，并复用现有 `ImWebSocketEvent`、`WebSocketMessage` 和 `CHAT_MESSAGE_PUSH`。

## Impact

- 代码：`mallchat-chat-service` 的 MQ 生产与消息服务调用；`mallchat-notification-service` 的房间广播处理器。
- 测试：新增/更新 chat producer、chat message service 和 notification handler 单测。
- OpenSpec：新增 `chat-realtime-delivery` spec，并归档该 change。
- 风险：成员快照是事件入队时的数据库事实；若事件积压期间成员关系变化，下一阶段需通过权限收敛和离线补偿进一步治理。
