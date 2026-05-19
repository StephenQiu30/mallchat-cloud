## 1. OpenSpec 与自审

- [x] 1.1 创建 `enhance-message-read-status-summary` change。
- [x] 1.2 明确本次只做发送者可见的聚合统计，不实现成员名单、送达状态或新表。
- [x] 1.3 运行 `openspec validate enhance-message-read-status-summary --strict`。
- [x] 1.4 使用 Hermes 只读复核范围、测试和实现一致性。

## 2. TDD 红灯

- [x] 2.1 增加发送者查询消息已读统计成功测试。
- [x] 2.2 增加非发送者查询被拒绝测试。
- [x] 2.3 增加非房间成员查询被拒绝测试。
- [x] 2.4 增加消息不存在查询被拒绝测试。
- [x] 2.5 增加消息不属于房间查询被拒绝测试。
- [x] 2.6 增加跨房间消息存在性不泄露测试。
- [x] 2.7 增加 `lastReadMessageId > messageId` 计入已读测试。
- [x] 2.8 增加返回 VO 不暴露成员名单字段测试。

## 3. 最小实现

- [x] 3.1 新增 `ChatMessageReadStatusVO`。
- [x] 3.2 新增 Controller / Feign / Service 查询契约。
- [x] 3.3 基于 `ChatRoomMember.lastReadMessageId` 计算 `totalCount`、`readCount`、`unreadCount`。
- [x] 3.4 保持现有 `markMessageRead` 读边界上报和推送事件不变。
- [x] 3.5 使用 `roomId + messageId` 联合查询，避免跨房间消息存在性侧信道泄漏。

## 4. 验证与归档

- [x] 4.1 运行 `ChatMessageServiceImplTest` 相关测试。
- [x] 4.2 运行 chat-service 模块回归。
- [x] 4.3 运行 `openspec validate --all --strict`。
- [x] 4.4 归档本次 OpenSpec change 并再次运行 `openspec validate --all --strict`。
- [x] 4.5 更新 `task_plan.md`、`findings.md`、`progress.md` 并按 test/impl 拆分提交。
