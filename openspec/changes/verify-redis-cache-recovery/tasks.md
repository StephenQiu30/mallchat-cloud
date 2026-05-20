## 1. OpenSpec

- [x] 1.1 创建 `verify-redis-cache-recovery` change。
- [x] 1.2 明确缓存失效恢复边界。
- [x] 1.3 运行 `openspec validate verify-redis-cache-recovery --strict`。

## 2. TDD

- [x] 2.1 先补好友和房间成员冷缓存 characterization test，锁定已有 DB 回源行为。
- [x] 2.2 先补 WebSocket 心跳重建 Redis 在线态测试。
- [x] 2.3 运行目标测试确认 WebSocket 缺失重建 RED；好友和房间成员冷缓存测试作为已有行为保护。

## 3. Implementation

- [x] 3.1 让 WebSocket 心跳在 Redis 集合缺失时从本地连接重建在线态。
- [x] 3.2 保持好友和房间成员 DB 回源逻辑，不扩大到脏缓存修复。
- [x] 3.3 更新验收文档。

## 4. Validation

- [x] 4.1 运行 websocket 与 chat-service 目标测试。
- [x] 4.2 运行 OpenSpec strict 校验。
- [x] 4.3 同步 GitHub Issue #18。
