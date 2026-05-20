## Why

IM 核心写接口在生产环境会承受更高频的调用压力，当前网关已有通用 `RequestRateLimiter`，但 `/api/chat/message/send` 仍与聊天查询接口共用较宽松的通用聊天路由。为了先满足生产可用 MVP，需要在不引入新限流体系的前提下，为消息发送入口增加更明确的用户维度限流验收。

## What Changes

- 在 Gateway 路由中为 `/api/chat/message/send` 增加优先于 `/api/chat/**` 的专用路由。
- 复用现有 `RequestRateLimiter` 与 `userKeyResolver`。
- 通过配置测试验证专用路由、限流参数和路由顺序。

## Non-Goals

- 不引入新的限流中间件或自定义过滤器。
- 不在本次实现复杂动态限流后台。
- 不改变现有 Redis RateLimiter 的实现模型。
