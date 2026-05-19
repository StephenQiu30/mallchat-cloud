---
layer: operations
doc_no: O-002
audience: 后端开发、客户端开发、运维
purpose: 明确 MallChat WebSocket 默认运行承载、连接入口、鉴权心跳和网关限制
owner: MallChat Backend
inputs:
  - docs/prd/P-009-websocket-runtime-contract-prd.md
  - openspec/changes/align-websocket-runtime-contract
outputs:
  - WebSocket 本地与生产运行契约
  - 后续 gateway 代理或独立 websocket-service 的待办边界
triggers:
  - WebSocket 服务启动、部署、联调或客户端接入
downstream:
  - harden-im-realtime-delivery
  - mallchat-taro WebSocket 接入
---

# WebSocket 运行契约

## 背景

MallChat 当前 WebSocket 能力位于 `mallchat-common-websocket`，该模块提供 Netty server、连接管理、心跳刷新和在线状态通知。该 common 模块会被业务服务复用，但它不是独立服务发现身份。

为避免多个服务因为引入 common 模块而同时监听 `9090`，WebSocket server 必须由具体运行服务显式启用。

## 默认承载服务

首版默认由 `mallchat-notification-service` 承载 WebSocket 长连接：

```yaml
websocket:
  enabled: true
```

`mallchat-chat-service` 不默认启动 WebSocket 监听：

```yaml
websocket:
  enabled: false
```

后续如果新增独立 `mallchat-websocket-service`，必须通过新的 OpenSpec change 迁移服务发现名、gateway route、部署脚本和客户端配置。

## 默认连接入口

本地联调首选连接入口：

```text
ws://localhost:9090/websocket?token=<access-token>
```

生产环境应将 `<notification-host>` 替换为实际部署的 notification-service WebSocket 暴露地址：

```text
ws://<notification-host>:9090/websocket?token=<access-token>
```

服务端仍支持通过 `Authorization` header 传递 token；小程序端或无法稳定设置 WebSocket header 的客户端可使用 query token。

## 鉴权与心跳

1. 握手阶段由 `HttpHeadersHandler` 解析 token。
2. token 必须来自服务端登录态，客户端不得通过消息体声明或伪造 `userId`。
3. 业务消息在未认证时会返回错误并关闭连接。
4. 心跳消息用于刷新 Redis 分布式连接态 TTL，并返回 `pong`。
5. 同一用户多连接时，应以首次上线和最后下线作为在线状态广播边界。

## Gateway 现状

`mallchat-gateway` 当前存在 `/api/websocket/** -> lb://mallchat-websocket-service` 路由，但仓库没有独立 `mallchat-websocket-service` 模块。

在完成独立验证前，`/api/websocket/**` 不作为已验收 WebSocket 运行入口。后续若要通过 gateway 代理，需要单独确认以下事项：

1. gateway 路由目标的服务发现名真实存在。
2. gateway 能将 WebSocket upgrade 流量代理到实际 Netty 监听端口。
3. gateway 白名单、鉴权过滤器和服务端握手鉴权不会互相冲突。
4. 客户端连接地址、Nacos 配置和部署文档同步更新。

## 验收检查

1. `mallchat-common-websocket` 默认 `websocket.enabled=false`。
2. 未显式启用时，依赖 common WebSocket 的服务不启动 Netty listener。
3. `mallchat-notification-service` 显式启用 WebSocket。
4. `mallchat-chat-service` 显式关闭 WebSocket。
5. OpenSpec change `align-websocket-runtime-contract` 校验通过。
6. `mallchat-common-websocket` 单元测试通过。
