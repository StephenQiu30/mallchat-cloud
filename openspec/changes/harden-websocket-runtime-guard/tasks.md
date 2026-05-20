## 1. OpenSpec

- [x] 1.1 创建 `harden-websocket-runtime-guard` change。
- [x] 1.2 增加 `websocket-runtime-contract` delta，明确本地连接上限、短时间重复连接拒绝和异常断开审计。
- [x] 1.3 运行 `openspec validate harden-websocket-runtime-guard --strict`。

## 2. TDD

- [x] 2.1 先补 `ChannelManagerTest`，覆盖同用户连接超过上限时拒绝新连接。
- [x] 2.2 先补 `ChannelManagerTest`，覆盖同用户短时间重复连接时拒绝新连接。
- [x] 2.3 先补 `ChannelManagerTest`，覆盖未登记连接断开时记录异常断开计数。
- [x] 2.4 先补 `WebSocketPropertiesTest`，覆盖运行保护默认配置。
- [x] 2.5 运行目标测试确认红灯来自缺失运行保护 API 或行为。
- [x] 2.6 根据 review 补并发握手回归测试，证明单用户连接上限判断不可被并发绕过。
- [x] 2.7 根据 review 补 guard 拒绝连接断开回归测试，确认不污染异常断开审计。

## 3. Implementation

- [x] 3.1 最小修改 `WebSocketProperties`，增加单用户连接上限和最小连接间隔配置。
- [x] 3.2 最小修改 `ChannelManager`，支持运行保护配置和拒绝计数。
- [x] 3.3 最小修改 `ChannelManager.addChannel`，连接数超过上限时关闭新连接并返回 false。
- [x] 3.4 最小修改 `ChannelManager.addChannel`，短时间重复连接时关闭新连接并返回 false。
- [x] 3.5 最小修改 `ChannelManager.removeChannel`，未登记连接断开时记录异常断开计数。
- [x] 3.6 最小修改 `NettyWebSocketServer`，启动时注入运行保护配置。
- [x] 3.7 最小修改 `ChannelManager`，区分 guard 主动拒绝连接与真正未登记断开。

## 4. Validation

- [x] 4.1 运行目标测试。
- [x] 4.2 运行 common WebSocket 模块回归。
- [x] 4.3 运行 `openspec validate harden-websocket-runtime-guard --strict`。
- [x] 4.4 运行 `openspec validate --all --strict`。
- [x] 4.5 更新 GitHub Issue #7 和验收记录。
