---
layer: Acceptance
doc_no: "A-017"
audience:
  - Dev
  - QA
  - Ops
feature_area: im-message-reliability
purpose: "记录 m3 消息可靠性与可恢复 Epic 的测试先行、实现范围和验收命令。"
canonical_path: "docs/acceptance/A-017-m3-message-reliability-acceptance.md"
status: review
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "GitHub Issue #10"
  - "GitHub Issue #11"
  - "GitHub Issue #12"
  - "GitHub Issue #13"
  - "openspec/changes/add-rabbitmq-publish-observability"
  - "openspec/changes/add-im-push-failure-metrics"
  - "openspec/changes/verify-reconnect-message-recovery"
  - "openspec/changes/harden-message-idempotency"
outputs:
  - "m3-backend-message-reliability-epic"
  - "RabbitMQ 发布观测"
  - "IM 推送结果指标"
  - "重连补偿与消息幂等验收"
triggers:
  - "创建或更新 m3 PR"
  - "回归消息可靠性 Epic #3"
downstream:
  - "PR m3"
---

# m3 消息可靠性与可恢复验收

## 1. 验收范围

本次 m3 聚合消费 Epic #3 下的 #10、#11、#12、#13。实现保持最小闭环：不引入 outbox，不改变现有 RabbitMQ/WebSocket envelope，不新增离线消息表。

## 2. 结论

1. #10：RabbitMQ 发布 accepted、failed、rejected、confirm ack/nack、return 已有可测试指标。
2. #11：聊天消息推送、通用 WebSocket 推送、通知推送已记录 success、offline、failure、skipped。
3. #12：断线补偿继续复用 `listMessagesAfter` 和持久化消息事实，不依赖 Redis 在线态或 WebSocket 重放。
4. #13：重复 `clientMsgId` 并发唯一键冲突会回读既有消息；重复消息事件不会再次增加会话未读数。

## 3. RED 证据

1. `RabbitMqSenderTest` / `RabbitMqPublishObservationTest` 初次运行失败于缺少 `RabbitMqPublishObservation`。
2. `ChatMessagePushHandlerTest` / `NotificationPushHandlerTest` 初次运行失败于缺少 `ImPushMetricsRecorder`。
3. `ChatMessageServiceImplTest` 的重复 `clientMsgId` 唯一键冲突用例初次运行失败于未捕获 `DuplicateKeyException`。
4. `ChatSessionServiceImplTest#shouldNotIncrementUnreadWhenSameMessageBatchIsAppliedTwice` 初次运行失败：接收方未读数从 2 错增到 3。
5. `ChatMessagePushHandlerTest` / `WebSocketPushHandlerTest` 的部分在线、部分离线用例初次运行失败：只记录 success，未记录 offline。

## 4. GREEN 命令

```bash
mvn -pl mallchat-common/mallchat-common-rabbitmq -am -Dtest=RabbitMqSenderTest,RabbitMqPublishObservationTest,RabbitMqConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl mallchat-service/mallchat-notification-service -am -Dtest=ChatMessagePushHandlerTest,NotificationPushHandlerTest,WebSocketPushHandlerTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl mallchat-service/mallchat-chat-service -am -Dtest=ChatSessionServiceImplTest,ChatMessageServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test
openspec validate --all --strict
```

## 5. 残余风险

1. RabbitMQ confirm 真实触发仍依赖生产配置启用 publisher confirm / return，本次代码已保留 correlation id 和 headers。
2. `offline` 指标表示当前 notification-service 实例没有本地连接，不等同于全局用户不在线。
3. OpenSpec changes 暂不归档，待 m3 PR review 通过后统一归档。
