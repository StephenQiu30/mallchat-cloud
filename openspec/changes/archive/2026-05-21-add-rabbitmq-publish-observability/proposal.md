## Why

RabbitMQ 是 IM 实时通知、聊天消息推送和异步同步的核心通道。当前发送端已有确认回调日志，但发送门面没有把业务类型、业务 ID、发布失败和 return/nack 统一成可测试、可统计的观测事实。生产可用 MVP 需要先补齐最小观测闭环，便于定位消息类型和业务 ID。

## What Changes

- 为 `RabbitMqSender` 发布成功、发布异常、空 payload 拒绝增加可测试的 Micrometer 计数。
- 为 RabbitMQ confirm ack/nack 和 return 增加带 `bizType` / `bizId` 的观测记录。
- 发送时保留现有 `RabbitMessage` 模型，同时向 RabbitMQ message headers 和 correlation id 写入业务标识。

## Non-Goals

- 不引入 outbox 表。
- 不改变现有 exchange、routing key、`RabbitMessage` 外层模型。
- 不新增消息中间件或重试调度服务。
