## Why

IM 消息事实已经先落库再推送，但 notification-service 当前只记录推送日志，无法按消息类型统计离线、失败等结果。生产可用 MVP 需要让推送失败可观测，同时不能改变业务写入成功语义。

## What Changes

- 为 WebSocket/聊天消息/通知推送处理器增加最小 Micrometer 计数。
- 统计维度保持简单：`bizType`、`eventType`、`result`。
- 对用户离线、写入异常、成功推送分别记录结果；异常继续按现有 MQ 语义抛出或降级。

## Non-Goals

- 不改变业务事实落库成功语义。
- 不新增独立监控服务。
- 不改变 RabbitMQ 或 WebSocket 事件外层模型。
