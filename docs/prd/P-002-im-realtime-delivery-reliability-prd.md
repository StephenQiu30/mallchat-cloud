---
layer: PRD
doc_no: "P-002"
audience:
  - PM
  - Dev
  - QA
  - Ops
feature_area: im-realtime-delivery-reliability
purpose: "定义 MallChat IM 实时消息投递、离线补偿、WebSocket 连接和推送可靠性的后端增强边界。"
canonical_path: "docs/prd/P-002-im-realtime-delivery-reliability-prd.md"
status: draft
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "docs/prd/P-001-im-real-time-communication-prd.md"
  - "openspec/specs/chat-message/spec.md"
  - "openspec/specs/chat-session/spec.md"
  - "openspec/specs/chat-online-status/spec.md"
outputs:
  - "实时投递可靠性增强需求"
  - "后续 OpenSpec change: harden-im-realtime-delivery"
triggers:
  - "消息实时推送丢失、重复或延迟"
  - "WebSocket 多实例部署或网关路由调整"
  - "需要补齐离线消息与重连补偿"
downstream:
  - "docs/design/"
  - "docs/acceptance/"
---

# IM 实时投递可靠性 PRD

## 1. 背景

当前后端已经具备 `chat-service -> RabbitMQ -> notification-service -> WebSocket` 的实时链路。消息发送后，`ChatMqProducer` 将聊天事件包装为 `ImWebSocketEvent`，`ChatMessagePushHandler` 再按房间成员或指定用户推送到本地 `ChannelManager`。

调研发现，房间广播依赖 Redis 房间成员缓存。如果缓存为空，推送处理器会跳过推送；同时网关配置存在 `/api/websocket/** -> lb://mallchat-websocket-service`，但仓库未发现独立同名服务模块，WebSocket 真实承载服务与网关契约需要澄清。

## 2. 产品目标

1. 用户在线时，新消息、撤回、已读、会话更新、好友事件和在线状态应稳定实时到达。
2. 用户离线或断线重连后，应能通过会话列表与历史消息补偿未收到的事件。
3. 多实例部署下，WebSocket 连接、RabbitMQ 消费、Redis 连接态和网关路由的职责边界清晰。

```gherkin
Given 用户 A 和用户 B 已在同一房间
When 用户 A 发送一条消息
Then 用户 B 在线时应收到 WebSocket 消息事件
And 用户 B 离线后重新进入会话时应能通过会话未读和历史消息补齐该消息
```

## 3. 非目标

- 不在首版实现端到端加密、全量消息漫游重构或独立长连接网关。
- 不替换现有 RabbitMQ、Redis、Netty WebSocket 技术栈。
- 不把所有实时事件改成同步 HTTP 调用。

## 4. 核心用户故事

### 4.1 在线用户

作为在线聊天用户，我希望消息和会话变化实时出现，不需要手动刷新。

验收标准：
- 新消息、撤回、已读、会话置顶/删除、好友申请/通过、在线状态均有统一事件类型。
- 房间成员缓存为空时，系统必须有可验证的回补策略，不能直接导致消息实时推送丢失。
- 同一 `bizId` 的实时事件不应在客户端造成重复业务效果。

### 4.2 离线或重连用户

作为网络不稳定的移动端用户，我希望重连后会话未读数和历史消息能恢复真实状态。

验收标准：
- 会话列表以数据库 `chat_session` 为准展示未读和最新消息。
- 历史消息支持按消息 ID 游标加载，且只允许房间成员访问。
- 客户端可用最后已读消息 ID 或最后收到消息 ID 做补偿入口。

### 4.3 运维用户

作为运维用户，我希望知道 WebSocket 应该连接哪个服务、哪个路径和哪个端口。

验收标准：
- 网关路由、Nacos 服务名、WebSocket 端口和连接路径在 docs 或 operations 文档中保持一致。
- 多实例部署时，Redis 连接态过期、心跳刷新和下线通知有明确策略。

## 5. 数据与权限边界

- 消息写入必须先落库，再发送 MQ 推送。
- 推送只能发送给房间成员、指定用户或系统广播目标。
- Redis 只作为在线连接、好友缓存和房间成员缓存的加速层，不能成为唯一事实来源。

## 6. 首版验收门禁

- 新增缓存冷启动回补或补偿策略的单元测试。
- 新增 WebSocket 路由/服务名契约说明。
- 核心命令至少覆盖 chat-service、notification-service、websocket common 的相关测试。

## 7. 风险与边界

- 如果 WebSocket 长连接绕过网关直接连 9090，需要在客户端配置和运维文档中明确。
- 如果通过网关代理 WebSocket，需要确认服务发现名与实际 Spring Boot 应用一致。

## 8. 待确认问题

- WebSocket 首版是独立服务、notification-service 内嵌，还是 chat-service 内嵌？
- 客户端重连补偿以 `lastMessageId`、`lastReadMessageId` 还是服务端会话版本号作为主游标？

## 9. 变更记录

| 日期 | 作者 | 版本 | 变更说明 |
| --- | --- | --- | --- |
| 2026-05-19 | StephenQiu30 | 0.1.0 | 初始化实时投递可靠性增强 PRD |
