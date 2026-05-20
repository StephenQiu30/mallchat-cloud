## 1. OpenSpec

- [x] 1.1 增加 `chat-room-access` delta，明确群解散业务事实不因会话删除推送失败回滚。
- [x] 1.2 运行 `openspec validate harden-group-dismiss-push-degradation --strict`。

## 2. TDD

- [x] 2.1 先补 `ChatRoomServiceImplTest`，覆盖群解散时会话删除推送失败仍完成成员、会话、群资料和房间删除。
- [x] 2.2 运行目标测试确认红灯来自 `sendSessionDelete` 异常中断解散流程。

## 3. Implementation

- [x] 3.1 最小修改 `ChatRoomServiceImpl#dismissRoom`，捕获单个成员会话删除推送异常并记录日志。
- [x] 3.2 保持解散权限校验和数据库删除流程不变。

## 4. Validation

- [x] 4.1 运行 `ChatRoomServiceImplTest` 目标测试。
- [x] 4.2 运行 `mallchat-chat-service` 模块回归。
- [x] 4.3 使用测试验证人/Code Reviewer 子智能体只读复核变更。
- [x] 4.4 新增验收文档并归档 OpenSpec change。
- [x] 4.5 运行归档后 `openspec validate --all --strict`、`git diff --check`，提交并推送。
