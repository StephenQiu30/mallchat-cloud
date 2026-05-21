## Why

断线重连补偿已经依赖持久化消息、会话游标和 `listMessagesAfter`，但生产可用 Epic 需要把真实链路验收与“不依赖实时缓存重放”的边界显式固化，避免后续实现误把离线补偿耦合到 WebSocket 或 Redis 在线态。

## What Changes

- 复用现有消息拉取、会话游标和房间成员权限校验。
- 补齐 OpenSpec 验收，说明补偿来自数据库消息事实，不要求 WebSocket 离线重放。
- 保持现有 `chat-*` 命名与消息事件模型。

## Non-Goals

- 不要求所有离线消息通过 WebSocket 重放。
- 不引入离线消息新表。
- 不改变历史消息分页语义。
