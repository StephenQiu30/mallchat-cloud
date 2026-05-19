## 1. OpenSpec 与自审

- [x] 1.1 创建 `enhance-message-reply-preview` change。
- [x] 1.2 明确本次只做引用回复契约补强，不实现转发/合并转发/语音/表情。
- [x] 1.3 运行 `openspec validate enhance-message-reply-preview --strict`。
- [x] 1.4 使用 Hermes 只读复核范围、测试和实现一致性。

## 2. TDD 红灯

- [x] 2.1 增加同房间引用发送成功测试，断言保存 `replyMsgId` 并返回 `replyMsg`。
- [x] 2.2 增加跨房间引用拒绝测试。
- [x] 2.3 增加被引用消息撤回时预览脱敏测试。
- [x] 2.4 增加引用预览返回被引用消息发送者名称测试。
- [x] 2.5 增加被引用消息不存在时拒绝发送测试。
- [x] 2.6 增加历史脏数据跨房间引用展示不泄露测试。
- [x] 2.7 增加无发送权限时引用消息拒绝测试。

## 3. 最小实现

- [x] 3.1 保持发送权限校验和同房间引用校验。
- [x] 3.2 `ReplyMsgVO` 填充被引用消息发送者名称。
- [x] 3.3 撤回消息引用预览继续脱敏，不泄漏原内容。
- [x] 3.4 展示层二次校验被引用消息房间，防止历史脏数据跨房间泄露。

## 4. 验证与归档

- [x] 4.1 运行 `ChatMessageServiceImplTest` 相关测试。
- [x] 4.2 运行 chat-service 模块回归。
- [x] 4.3 运行 `openspec validate --all --strict`。
- [x] 4.4 归档本次 OpenSpec change 并再次运行 `openspec validate --all --strict`。
- [x] 4.5 更新 `task_plan.md`、`findings.md`、`progress.md` 并按 test/impl 拆分提交。
