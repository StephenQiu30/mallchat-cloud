## Why

MallChat 的实时 IM 链路已经包含 Netty WebSocket、RabbitMQ 推送、在线状态和通知分发，但当前运行契约存在前置风险：`mallchat-common-websocket` 被多个服务依赖后会自动启动长连接端口，而网关又配置了 `/api/websocket/** -> lb://mallchat-websocket-service`，仓库中却没有独立的 `mallchat-websocket-service` 模块。若不先收敛 WebSocket 承载服务、启动开关、连接地址和心跳鉴权契约，后续好友、群聊、富消息、已读、动态与通知聚合都会建立在不稳定的实时入口上。

## What Changes

- 新增 `websocket.enabled` 运行开关，默认不让 common WebSocket 能力在任意依赖方中隐式启动。
- 明确首版 WebSocket 运行承载由 `mallchat-notification-service` 显式启用，`mallchat-chat-service` 只复用在线状态/连接管理能力，不默认监听 Netty 端口。
- 补充 WebSocket 运行契约文档，说明本地首选连接方式、鉴权 token、心跳、断线重连和网关路由现状。
- 以 TDD 方式先补默认关闭行为测试，再实现配置与文档更新。
- 不修复房间成员缓存回源、不重写实时消息事件模型、不新增好友/群聊/动态/通知业务能力。

## Capabilities

### New Capabilities

- `websocket-runtime-contract`: MallChat WebSocket 服务承载、启动开关、连接入口、鉴权心跳和运维验收契约。

### Modified Capabilities

- 无。本 change 只新增运行契约能力，并通过配置约束现有 common WebSocket 启动行为。

## Impact

- 代码：`mallchat-common/mallchat-common-websocket` 的配置属性与 Netty server 启动条件。
- 配置：`mallchat-service/mallchat-chat-service`、`mallchat-service/mallchat-notification-service` 的 `websocket.enabled` 显式声明。
- 文档：新增 WebSocket 运行契约运维文档。
- 测试：新增或更新 common WebSocket 模块单测，验证默认关闭与显式启用契约。
