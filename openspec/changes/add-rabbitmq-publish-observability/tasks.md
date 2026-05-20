## 1. OpenSpec

- [x] 1.1 创建 `add-rabbitmq-publish-observability` change。
- [x] 1.2 明确 RabbitMQ 发布观测最小范围：accepted、failed、rejected、confirm ack/nack、return。
- [x] 1.3 运行 `openspec validate add-rabbitmq-publish-observability --strict`。

## 2. TDD

- [x] 2.1 先补 `RabbitMqSenderTest`，覆盖发布成功、发布异常、payload 为空拒绝。
- [x] 2.2 先补 `RabbitMqPublishObservationTest`，覆盖 confirm ack/nack 与 return 观测。
- [x] 2.3 运行目标测试确认 RED。

## 3. Implementation

- [x] 3.1 增加最小 `RabbitMqPublishObservation`，复用 Micrometer `MeterRegistry`。
- [x] 3.2 `RabbitMqSender` 发送时写入 correlation id 和 headers。
- [x] 3.3 `RabbitMqConfiguration` confirm/return 回调复用观测记录。

## 4. Validation

- [x] 4.1 运行 common-rabbitmq 目标测试。
- [x] 4.2 运行相关 Maven 模块测试。
- [x] 4.3 运行 OpenSpec strict 校验。
- [x] 4.4 同步 GitHub Issue #10。
