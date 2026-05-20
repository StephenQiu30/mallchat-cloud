## 1. OpenSpec

- [x] 1.1 增加 `chat-room-access` delta，明确退群会话删除推送失败不回滚退群事实。
- [x] 1.2 增加 `chat-session` delta，明确会话置顶/删除推送失败不回滚会话事实。
- [x] 1.3 运行 `openspec validate harden-session-operation-push-degradation --strict`。

## 2. TDD

- [x] 2.1 先补 `ChatRoomServiceImplTest`，覆盖退群时会话删除推送失败仍完成成员离开和会话删除。
- [x] 2.2 先补 `ChatSessionServiceImplTest`，覆盖置顶推送失败仍返回成功并保留置顶状态。
- [x] 2.3 先补 `ChatSessionServiceImplTest`，覆盖删除会话推送失败仍返回删除成功。
- [x] 2.4 运行目标测试确认红灯来自现有推送异常未降级。

## 3. Implementation

- [x] 3.1 最小修改 `ChatRoomServiceImpl#quitRoom`，捕获会话删除推送异常并记录日志。
- [x] 3.2 最小修改 `ChatSessionServiceImpl#topSession`，捕获会话刷新推送异常并记录日志。
- [x] 3.3 最小修改 `ChatSessionServiceImpl#deleteSession`，捕获会话删除推送异常并记录日志。

## 4. Validation

- [x] 4.1 运行目标测试。
- [x] 4.2 运行 `mallchat-chat-service` 模块回归。
- [x] 4.3 使用测试验证人/Code Reviewer 子智能体只读复核变更。
- [x] 4.4 新增验收文档并归档 OpenSpec change。
- [x] 4.5 运行归档后 `openspec validate --all --strict`、`git diff --check`，提交并推送。
