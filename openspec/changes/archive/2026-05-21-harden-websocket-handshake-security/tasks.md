## 1. OpenSpec

- [x] 1.1 创建 `harden-websocket-handshake-security` change。
- [x] 1.2 增加 `websocket-runtime-contract` delta，明确握手 token 和 Origin 拒绝规则。
- [x] 1.3 运行 `openspec validate harden-websocket-handshake-security --strict`。

## 2. TDD

- [x] 2.1 先补 `HttpHeadersHandlerTest`，覆盖缺失 token 时拒绝握手。
- [x] 2.2 先补 `HttpHeadersHandlerTest`，覆盖非法 token 时拒绝握手。
- [x] 2.3 先补 `HttpHeadersHandlerTest`，覆盖合法 token 绑定 userId 并继续握手。
- [x] 2.4 先补 `HttpHeadersHandlerTest`，覆盖 Origin 不在允许列表时拒绝握手。
- [x] 2.5 运行目标测试确认红灯来自现有握手未拒绝行为。

## 3. Implementation

- [x] 3.1 最小修改 `HttpHeadersHandler`，缺失 token 返回 `401 Unauthorized` 并关闭连接。
- [x] 3.2 最小修改 `HttpHeadersHandler`，非法 token 或认证异常返回 `401 Unauthorized` 并关闭连接。
- [x] 3.3 最小修改 `WebSocketProperties` 和 `NettyWebSocketServer`，支持传入 Origin allowlist。
- [x] 3.4 最小修改 `HttpHeadersHandler`，配置 Origin allowlist 后拒绝不在列表内的 Origin。
- [x] 3.5 修正 `websocket-runtime-contract` spec 的 Purpose 占位描述。

## 4. Validation

- [x] 4.1 运行目标测试。
- [x] 4.2 运行 common WebSocket 模块回归。
- [x] 4.3 运行 `openspec validate harden-websocket-handshake-security --strict`。
- [x] 4.4 运行 `openspec validate --all --strict`。
- [x] 4.5 更新 GitHub Issue #6 和验收记录。
