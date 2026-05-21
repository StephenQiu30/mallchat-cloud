# Change: harden-websocket-runtime-guard

## Summary

为 MallChat WebSocket 运行时增加轻量连接保护：同用户本地连接数上限、短时间重复连接拒绝、未登记连接断开审计计数。

## Motivation

WebSocket 是 IM 实时链路入口。m1 已加固握手鉴权和 Origin 校验，但握手通过后的连接注册仍缺少运行时保护：同一用户可以在单实例内建立过多连接，短时间重复连接没有本地降级，异常断开无法通过轻量事实定位。正式生产环境需要先补最小可用运行保护，避免滥用连接消耗 Netty、Redis 和推送资源。

本变更只做 common WebSocket 层的本地运行保护，不建设完整风控系统，不改变客户端消息协议。

## Scope

- `ChannelManager` 支持配置单用户本地连接上限。
- `ChannelManager` 支持配置同用户最小连接间隔，短时间重复连接被拒绝。
- `ChannelManager` 对未登记连接断开做轻量审计计数和日志记录。
- `WebSocketProperties` 暴露运行保护配置。
- `NettyWebSocketServer` 在启动时将运行保护配置注入 `ChannelManager`。

## Non-Goals

- 不建设独立风控系统。
- 不新增数据库审计表。
- 不改变客户端消息协议。
- 不改变 m1 的 token 和 Origin 握手规则。
- 不实现跨实例全局连接数限制。

## Validation

- `mvn -pl mallchat-common/mallchat-common-websocket -am -Dtest=ChannelManagerTest,WebSocketPropertiesTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -pl mallchat-common/mallchat-common-websocket -am test`
- `openspec validate harden-websocket-runtime-guard --strict`
- `openspec validate --all --strict`
