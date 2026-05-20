## 1. OpenSpec

- [x] 1.1 创建 `harden-message-idempotency` change。
- [x] 1.2 明确并发重复 `clientMsgId` 的最小兜底行为。
- [x] 1.3 运行 `openspec validate harden-message-idempotency --strict`。

## 2. TDD

- [x] 2.1 先补 `ChatMessageServiceImplTest`，覆盖唯一键冲突后返回既有消息且不重复推送。
- [x] 2.2 运行目标测试确认 RED。

## 3. Implementation

- [x] 3.1 在 `sendMessage` 保存阶段捕获唯一键冲突。
- [x] 3.2 冲突后按 `fromUserId + clientMsgId` 读取既有消息并返回。
- [x] 3.3 保持正常发送、权限校验、回复校验和实时推送行为不变。

## 4. Validation

- [x] 4.1 运行 chat-service 目标测试。
- [x] 4.2 运行相关 Maven 模块测试。
- [x] 4.3 运行 OpenSpec strict 校验。
- [x] 4.4 同步 GitHub Issue #13。
