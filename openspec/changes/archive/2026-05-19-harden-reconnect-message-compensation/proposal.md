# Change: Harden reconnect message compensation

## Why
MallChat 已经通过 WebSocket 运行契约和房间成员缓存缺失兜底改善实时投递，但断线或重连用户仍缺少后端补偿入口。现有历史消息接口只支持 `id < lastMessageId` 的向前翻页，不能表达“客户端最后收到消息之后新增了哪些消息”。如果客户端误用 `lastReadMessageId` 作为补偿游标，还可能把已收到但未读的消息再次混入恢复链路，或漏掉已收未读状态。

## What Changes
- 为房间消息新增按 `afterMessageId` 查询新增消息的后端契约，按消息 ID 升序返回，供客户端重连后补偿遗漏实时事件。
- 查询必须使用房间成员事实源校验访问权限，不依赖 Redis 在线态或房间成员缓存。
- 会话列表项暴露 `lastMessageId` 和 `lastReadMessageId`，让客户端区分“服务端最新消息游标”和“用户已读游标”。
- 明确保留既有历史消息接口的 `id < lastMessageId` 向前翻页语义。
- 保持现有历史翻页、消息发送、MQ/WebSocket envelope 不变。

## Scope
### In
- `chat-realtime-delivery` 新增重连补偿查询要求。
- `chat-message` 新增按接收游标查询更新消息的查询语义要求。
- `chat-session` 新增会话游标暴露要求。
- chat-service Controller、Service、Feign、VO 与对应单元测试。

### Out
- 客户端重连恢复实现。
- MQ 去重、事务 outbox、离线消息中心。
- 修改现有 WebSocket 事件模型或消息发送协议。
- 成员退群后的历史可见性策略扩展。

## Risks
- 若把 `lastReadMessageId` 当作补偿游标，会混淆阅读状态与接收状态；实现和文档必须明确使用 `afterMessageId`。
- 若不限制 `limit`，补偿接口可能在大房间重连时造成数据库压力；实现应设置默认值和最大值。
- 若绕过成员校验，补偿接口会成为历史消息越权读取入口。

## Validation
- OpenSpec strict 校验。
- TDD 红绿测试覆盖补偿查询成功、非成员拒绝、升序返回、空结果、limit 默认/上限和会话游标映射。
- chat-service Maven 相关测试与 OpenSpec 归档后全量校验。
