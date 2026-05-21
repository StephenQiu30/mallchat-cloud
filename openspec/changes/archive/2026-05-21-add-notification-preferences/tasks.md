## 1. OpenSpec

- [x] 1.1 创建 `add-notification-preferences` change。
- [x] 1.2 明确免打扰只影响 `CHAT_MESSAGE` 推送，不影响消息事实。

## 2. TDD

- [x] 2.1 先补 `ChatSessionServiceImplTest` 免打扰开启、推送失败降级和 push 目标过滤。
- [x] 2.2 先补 `ChatMessageServiceImplTest` 群消息排除免打扰接收者。
- [x] 2.3 先补 `ChatMessagePushHandlerTest` allowlist 优先级。

## 3. Implementation

- [x] 3.1 `chat_session` 增加 `mute_status`。
- [x] 3.2 新增 `/chat/session/mute`。
- [x] 3.3 群消息发送前过滤免打扰接收者。
- [x] 3.4 notification-service 房间广播尊重 `userIds` allowlist。
- [x] 3.5 新增 m7 增量 SQL，支持已有库补齐 `mute_status`。

## 4. Validation

- [x] 4.1 运行 m7 聚焦 Maven 测试。
- [x] 4.2 运行 chat/notification service 全量测试。
- [x] 4.3 同步 GitHub Issue #33。
