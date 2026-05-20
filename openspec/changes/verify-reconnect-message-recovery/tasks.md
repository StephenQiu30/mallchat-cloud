## 1. OpenSpec

- [x] 1.1 创建 `verify-reconnect-message-recovery` change。
- [x] 1.2 明确重连补偿依赖持久化消息事实和房间成员权限。
- [x] 1.3 运行 `openspec validate verify-reconnect-message-recovery --strict`。

## 2. TDD / Verification

- [x] 2.1 复核现有 `ChatMessageServiceImplTest` 中 after-cursor 拉取、非成员拒绝和 limit 归一化测试。
- [x] 2.2 如发现缺口，先补 RED 测试再实现。

## 3. Implementation

- [x] 3.1 优先复用现有 `listMessagesAfter`，不引入并行离线消息模型。

## 4. Validation

- [x] 4.1 运行 chat-service 相关测试。
- [x] 4.2 运行 OpenSpec strict 校验。
- [x] 4.3 同步 GitHub Issue #12。
