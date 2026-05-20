## 1. OpenSpec

- [x] 1.1 创建 `add-im-push-failure-metrics` change。
- [x] 1.2 明确推送指标最小范围：success、offline、failure、skipped。
- [x] 1.3 运行 `openspec validate add-im-push-failure-metrics --strict`。

## 2. TDD

- [x] 2.1 先补聊天消息推送离线和异常测试。
- [x] 2.2 先补通知推送离线和异常测试。
- [x] 2.3 运行目标测试确认 RED。

## 3. Implementation

- [x] 3.1 增加 `ImPushMetricsRecorder`，复用 Micrometer `MeterRegistry`。
- [x] 3.2 在 `ChatMessagePushHandler` 记录推送 success/offline/failure。
- [x] 3.3 在 `WebSocketPushHandler` 和 `NotificationPushHandler` 记录同类结果。

## 4. Validation

- [x] 4.1 运行 notification-service 目标测试。
- [x] 4.2 运行相关 Maven 模块测试。
- [x] 4.3 运行 OpenSpec strict 校验。
- [x] 4.4 同步 GitHub Issue #11。
