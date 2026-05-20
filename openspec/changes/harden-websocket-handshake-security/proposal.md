# Change: harden-websocket-handshake-security

## Summary

加固 MallChat WebSocket 握手入口，要求 IM 默认运行时在协议升级前拒绝缺失 token、非法 token 和不在允许列表内的 Origin，同时保持现有 `Authorization: Bearer <token>` 与 `?token=` 两种 token 传递方式。

## Motivation

WebSocket 是 MallChat 实时 IM 的生产入口。当前 `HttpHeadersHandler` 能解析 token 并绑定 userId，但 token 缺失、token 无效或解析异常时仍会继续放行给后续 WebSocket 协议升级处理器。正式生产环境中，这会让未认证连接进入长连接层，增加资源滥用和权限绕过风险。

本变更只补齐握手入口的最小安全边界，不改造登录协议、不引入独立认证中心、不改变消息事件模型。

## Scope

- WebSocket 握手前继续支持 `Authorization: Bearer <token>`。
- WebSocket 握手前继续支持 URL query 参数 `token`。
- token 缺失、无效或解析异常时返回 `401 Unauthorized` 并关闭连接。
- 配置了 `websocket.allowed-origins` 时，Origin 不在允许列表内返回 `403 Forbidden` 并关闭连接。
- 未配置 Origin 允许列表时保持兼容，默认不额外限制 Origin。
- 修正现有 `websocket-runtime-contract` spec 的 Purpose 占位描述。

## Non-Goals

- 不新增独立认证中心。
- 不改变 HTTP 登录接口。
- 不引入复杂风控系统。
- 不实现连接频率限制，连接频率限制由 `harden-websocket-runtime-guard` 处理。
- 不改变客户端实时消息协议。

## Validation

- `mvn -pl mallchat-common/mallchat-common-websocket -Dtest=HttpHeadersHandlerTest test`
- `mvn -pl mallchat-common/mallchat-common-websocket -Dtest=HttpHeadersHandlerTest,WebSocketPropertiesTest test`
- `openspec validate harden-websocket-handshake-security --strict`
- `openspec validate --all --strict`
