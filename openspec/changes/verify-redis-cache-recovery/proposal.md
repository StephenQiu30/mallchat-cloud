## Why

MallChat 依赖 Redis 保存好友缓存、房间成员缓存、WebSocket 在线态和登录态。Redis 被清空后，业务事实不应永久丢失；能从 MySQL 恢复的缓存应回源，WebSocket 在线态应由仍存活的本地连接在心跳时重建。

## What Changes

- 验证好友和房间成员冷缓存从数据库回源。
- 修复 WebSocket 心跳在 Redis 连接集合丢失时不重建在线态的问题。
- 明确登录态 Redis 丢失后的恢复路径是用户重新登录，不恢复旧 token。
- 更新验收文档和 OpenSpec。

## Non-Goals

- 不修复 Redis 中已经存在但内容错误的脏缓存。
- 不要求所有缓存自动预热。
- 不让旧登录态在 Redis 清空后自动恢复。
