---
layer: PRD
doc_no: "P-009"
audience:
  - PM
  - Dev
  - QA
  - Ops
feature_area: websocket-runtime-contract
purpose: "定义 MallChat WebSocket 服务名、连接路径、鉴权、心跳和部署契约的后端优化边界。"
canonical_path: "docs/prd/P-009-websocket-runtime-contract-prd.md"
status: draft
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "docs/prd/P-002-im-realtime-delivery-reliability-prd.md"
outputs:
  - "WebSocket 运行契约优化需求"
  - "后续 OpenSpec change: align-websocket-runtime-contract"
triggers:
  - "调整 WebSocket 网关路由、服务拆分、连接鉴权或运维部署"
downstream:
  - "docs/design/"
  - "docs/operations/"
  - "docs/acceptance/"
---

# WebSocket 运行契约 PRD

## 1. 背景

WebSocket 是 IM 实时通讯的核心运行入口。当前代码中 `mallchat-common-websocket` 提供 Netty WebSocket server、连接管理和在线状态；Nacos secret 配置中有 `websocket.port=9090`；网关则配置 `/api/websocket/** -> lb://mallchat-websocket-service`。

调研未发现独立 `mallchat-websocket-service` 模块或同名 Spring Boot 应用。因此需要明确 WebSocket 首版运行契约，避免端侧和部署侧不知道该连接网关、notification-service、chat-service 还是独立服务。

## 2. 产品目标

1. 明确 WebSocket 服务名、端口、路径、鉴权方式和部署方式。
2. 明确心跳、连接刷新、掉线和在线状态通知规则。
3. 明确网关是否代理 WebSocket，以及代理后的路由和白名单策略。

```gherkin
Given 客户端持有有效登录 token
When 客户端连接 WebSocket 地址
Then 服务端应建立用户连接并记录分布式在线状态
And 客户端心跳应刷新连接过期时间
```

## 3. 非目标

- 不在本文档决定是否重写 WebSocket 框架。
- 不把业务消息发送接口改为 WebSocket 上行协议。
- 不在首版实现独立长连接网关集群，除非后续 OpenSpec 明确要求。

## 4. 核心用户故事

### 4.1 客户端开发者

作为客户端开发者，我需要一个稳定的 WebSocket 地址和鉴权规则。

验收标准：
- 文档明确连接 URL、token 放置方式、心跳格式和重连策略。
- 连接失败时能区分未登录、服务不可达和协议错误。
- 服务端事件格式与 `ImWebSocketEvent` 对齐。

### 4.2 后端开发者

作为后端开发者，我需要知道 WebSocket 承载在哪个服务中。

验收标准：
- 如果内嵌到 notification-service 或 chat-service，网关路由与服务名必须同步。
- 如果拆为独立 websocket-service，需要新增模块、启动类、Nacos 配置和部署说明。
- Common 模块只提供能力，不承担服务发现身份。

### 4.3 运维用户

作为运维用户，我需要能部署和观测 WebSocket 连接。

验收标准：
- 端口、线程数、Redis 连接态 TTL、服务实例 ID 有配置说明。
- 在线人数、连接数、异常关闭和推送失败有日志或指标。
- 发布后可以用验收命令验证连接建立和消息推送。

## 5. 数据与权限边界

- WebSocket 鉴权必须绑定登录用户 ID。
- 连接态存 Redis，业务消息和会话状态存数据库。
- 客户端不能通过 WebSocket 伪造他人 userId。

## 6. 首版验收门禁

- 网关路由和实际服务名一致。
- WebSocket 连接、心跳、下线、在线状态推送有测试或手工验收记录。
- operations 文档说明本地和生产连接方式。

## 7. 风险与边界

- 如果保持内嵌 Netty 服务直连 9090，网关 `/api/websocket/**` 路由可能误导客户端。
- 如果通过 Gateway 代理，需要确认 Spring Cloud Gateway 对当前 Netty WebSocket server 的代理兼容性。

## 8. 待确认问题

- WebSocket 是否需要独立服务模块？
- 客户端最终连接地址是 `/api/websocket/**` 还是 `:9090/websocket`？
- 连接鉴权 token 使用 query、header 还是首帧消息？

## 9. 变更记录

| 日期 | 作者 | 版本 | 变更说明 |
| --- | --- | --- | --- |
| 2026-05-19 | StephenQiu30 | 0.1.0 | 初始化 WebSocket 运行契约 PRD |
