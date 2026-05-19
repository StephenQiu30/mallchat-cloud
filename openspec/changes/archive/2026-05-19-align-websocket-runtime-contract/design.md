## Context

`mallchat-common-websocket` 当前同时提供 Netty WebSocket server、连接管理、心跳刷新和在线状态分发能力。该 common 模块被 `mallchat-chat-service` 与 `mallchat-notification-service` 依赖后，会随着 Spring 扫描创建 `NettyWebSocketServer` 的 `startServer` bean，并监听 `websocket.port`。在多服务同时启动时，这会导致 9090 端口归属不清或冲突。

另一方面，`mallchat-gateway` 当前把 `/api/websocket/**` 路由到 `mallchat-websocket-service`，但仓库没有这个服务模块。直接把 gateway 改到 `mallchat-notification-service` 也不能自动解决问题，因为当前 Netty WebSocket server 监听的是独立端口 `9090`，不是 notification-service 的 HTTP 端口。

## Decision

本 change 采用保守的最小运行契约：

1. `mallchat-common-websocket` 继续作为复用能力模块，但默认不启动 Netty server。
2. 新增 `websocket.enabled`，默认值为 `false`。
3. `NettyWebSocketServer#startServer` 仅在 `websocket.enabled=true` 时创建并启动。
4. 首版运行承载方为 `mallchat-notification-service`，该服务显式配置 `websocket.enabled: true`。
5. `mallchat-chat-service` 显式配置 `websocket.enabled: false`，避免 chat-service 和 notification-service 同时抢占 9090。
6. 首版客户端运行契约以直连 Netty 端口为准：`ws://<notification-host>:9090/websocket?token=<access-token>`。`Authorization` header 仍保留为服务端支持方式，但小程序端优先使用 query token。
7. 网关 `/api/websocket/**` 当前不作为已验收连接入口；后续若要走 gateway，需要单独 OpenSpec change 验证 Spring Cloud Gateway 与独立 Netty 端口的代理方案，或新增独立 websocket-service。

## Alternatives Considered

### 新增独立 `mallchat-websocket-service`

优点是服务发现名能与 gateway 路由完全一致；缺点是会新增部署单元、模块和跨服务依赖，超出本次“先收敛运行契约”的最小边界。当前 notification-service 已承担 MQ 到 WebSocket 的推送消费者，首版让它显式承载长连接更贴近现有代码。

### 直接把 gateway 路由改到 notification-service

该方案表面上消除了不存在的服务名，但当前 Netty server 监听独立 9090 端口，不是 notification-service 的 HTTP server 端口。未验证前修改路由会制造新的误导。

### 保持 common 模块自动启动

该方案最省改动，但无法保证只有一个服务监听 WebSocket 端口，也无法解释客户端到底应连接哪个服务。后续 IM 实时可靠性测试会因此不稳定。

## Testing Strategy

- 先新增 `WebSocketPropertiesTest`，验证 `websocket.enabled` 默认关闭。
- 实现属性与条件启动后，运行 `mallchat-common-websocket` 模块测试。
- 运行 `openspec validate align-websocket-runtime-contract --strict` 与 `openspec validate --all --strict`。
- 文档验收检查连接入口、鉴权 token、心跳与 gateway 现状是否写清楚。

## Non-goals

- 不修复 `ChatMessagePushHandler` 在房间成员缓存为空时跳过推送的问题。
- 不新增离线消息补偿游标。
- 不调整 RabbitMQ 事件模型。
- 不实现好友申请、群邀请、动态点赞评论或通知聚合。
- 不重构 WebSocket server 为独立服务，除非后续 change 明确认领。
