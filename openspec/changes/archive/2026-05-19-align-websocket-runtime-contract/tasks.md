## 1. OpenSpec 契约

- [x] 1.1 创建 `align-websocket-runtime-contract` change。
- [x] 1.2 编写 proposal/design/spec delta，明确 WebSocket 运行承载、启动开关和非目标。
- [x] 1.3 使用 Hermes 审阅 PRD 拆解顺序，并将审阅结论同步到计划文件。

## 2. TDD 实现

- [x] 2.1 先新增失败测试，验证 common WebSocket 默认不隐式启动的配置契约。
- [x] 2.2 在 `WebSocketProperties` 新增 `websocket.enabled=false` 默认值。
- [x] 2.3 为 Netty server 启动 bean 增加 `websocket.enabled=true` 条件。
- [x] 2.4 在 notification-service 显式启用 WebSocket，在 chat-service 显式关闭 WebSocket。

## 3. 文档与运维

- [x] 3.1 新增 WebSocket 运行契约运维文档，说明本地/生产连接方式、鉴权、心跳和 gateway 现状。
- [x] 3.2 更新计划与发现文件，记录 Hermes 审阅、实现取舍和待后续 change 的事项。

## 4. 验证

- [x] 4.1 记录红灯测试结果。
- [x] 4.2 运行 `mvn -pl mallchat-common/mallchat-common-websocket -am test`。
- [x] 4.3 运行 `openspec validate align-websocket-runtime-contract --strict`。
- [x] 4.4 运行 `openspec validate --all --strict`。
- [x] 4.5 检查 Git 状态，确认只包含本轮 OpenSpec/实现/文档变更。
