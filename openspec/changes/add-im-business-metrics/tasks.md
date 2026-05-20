## 1. OpenSpec

- [x] 1.1 创建 `add-im-business-metrics` change。
- [x] 1.2 明确业务指标最小范围：消息发送、好友申请、动态互动。
- [x] 1.3 运行 `openspec validate add-im-business-metrics --strict`。

## 2. TDD

- [x] 2.1 先补业务指标记录器测试。
- [x] 2.2 先补消息发送、好友申请、动态互动指标测试。
- [x] 2.3 运行目标测试确认 RED。

## 3. Implementation

- [x] 3.1 增加 `ChatBusinessMetricsRecorder`。
- [x] 3.2 在消息发送成功和幂等重复路径记录指标。
- [x] 3.3 在好友申请新建和重复申请路径记录指标。
- [x] 3.4 在动态点赞和评论成功路径记录指标。

## 4. Validation

- [x] 4.1 运行 chat-service 目标测试。
- [x] 4.2 运行 OpenSpec strict 校验。
- [x] 4.3 同步 GitHub Issue #15。
