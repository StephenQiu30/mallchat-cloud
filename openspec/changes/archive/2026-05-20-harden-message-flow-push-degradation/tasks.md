## 1. OpenSpec

- [x] 1.1 增加 `chat-message` delta，明确消息发送、已读上报、消息撤回的实时推送失败不回滚业务事实。
- [x] 1.2 增加 `chat-session` delta，明确消息主链路触发的会话刷新推送失败不回滚消息或已读/撤回事实。
- [x] 1.3 运行 `openspec validate harden-message-flow-push-degradation --strict`。

## 2. TDD

- [x] 2.1 先补 `ChatMessageServiceImplTest`，覆盖消息发送后聊天消息实时推送失败仍返回已落库消息。
- [x] 2.2 先补 `ChatMessageServiceImplTest`，覆盖已读上报后已读推送失败仍保留已读边界和未读数更新。
- [x] 2.3 先补 `ChatMessageServiceImplTest`，覆盖已读上报后会话刷新推送失败仍保留已读边界和未读数更新。
- [x] 2.4 先补 `ChatMessageServiceImplTest`，覆盖撤回后撤回推送失败仍保留撤回状态。
- [x] 2.5 先补 `ChatMessageServiceImplTest`，覆盖撤回后单个成员会话刷新推送失败不阻断其他成员刷新尝试。
- [x] 2.6 先补 `ChatSessionListenerTest`，覆盖消息发送事件触发会话刷新推送失败不阻断会话事实更新和其他成员刷新尝试。
- [x] 2.7 运行目标测试确认红灯来自现有推送异常未降级。

## 3. Implementation

- [x] 3.1 最小修改 `sendMessage`，捕获聊天消息推送异常并记录日志。
- [x] 3.2 最小修改 `markMessageRead`，捕获已读事件和会话刷新推送异常并记录日志。
- [x] 3.3 最小修改 `recallMessage`，捕获撤回事件和成员会话刷新推送异常并记录日志。
- [x] 3.4 最小修改 `ChatSessionListener`，捕获消息发送后成员会话刷新推送异常并记录日志。

## 4. Validation

- [x] 4.1 运行目标测试。
- [x] 4.2 运行 `mallchat-chat-service` 模块回归。
- [x] 4.3 使用测试验证人/Code Reviewer 子智能体只读复核变更。
- [x] 4.4 新增验收文档并归档 OpenSpec change。
- [x] 4.5 运行归档后 `openspec validate --all --strict`、`git diff --check`，提交并推送。
