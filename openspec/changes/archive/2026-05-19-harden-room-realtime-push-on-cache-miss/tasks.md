## 1. OpenSpec 与自审

- [x] 1.1 创建 `harden-room-realtime-push-on-cache-miss` change。
- [x] 1.2 编写 proposal/design/spec delta，限定边界为房间实时推送缓存缺失兜底。
- [x] 1.3 等待 Hermes 审阅并同步边界结论。

## 2. TDD 红灯

- [x] 2.1 为 notification-service 新增缓存缺失时使用 `WebSocketMessage.userIds` 兜底推送的失败测试。
- [x] 2.2 为 chat producer 新增房间广播携带成员快照的失败测试。
- [x] 2.3 为 chat message service 新增发送、已读、撤回房间事件时传递成员快照的失败测试。

## 3. 最小实现

- [x] 3.1 `ChatMessagePushHandler` 在 Redis 成员缓存为空时使用消息成员快照兜底。
- [x] 3.2 `ChatMqProducer` 支持房间广播事件携带成员快照，保持现有事件模型不变。
- [x] 3.3 `ChatMessageServiceImpl` 在消息、已读、撤回房间事件中传入数据库成员快照。

## 4. 验证与收口

- [x] 4.1 运行 notification-service 相关测试。
- [x] 4.2 运行 chat-service 相关测试。
- [x] 4.3 运行 OpenSpec change 与全量 strict 校验。
- [x] 4.4 归档 OpenSpec change 并执行归档后 strict 校验。
- [x] 4.5 检查 Git 状态并提交本轮变更。
