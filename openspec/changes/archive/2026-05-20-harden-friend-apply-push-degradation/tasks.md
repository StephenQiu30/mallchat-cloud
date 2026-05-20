## 1. OpenSpec

- [x] 1.1 增加 `chat-friend` delta，明确好友申请和好友通过 WebSocket/MQ 推送失败不回滚业务事实。
- [x] 1.2 运行 `openspec validate harden-friend-apply-push-degradation --strict`。

## 2. TDD

- [x] 2.1 先补 `UserFriendApplyServiceImplTest`，覆盖好友申请保存后 WebSocket/MQ 推送失败仍返回申请 ID。
- [x] 2.2 先补 `UserFriendApplyServiceImplTest`，覆盖好友通过后 WebSocket/MQ 推送失败仍保留好友关系、私聊房间和申请状态更新。
- [x] 2.3 运行目标测试确认红灯来自现有推送异常未降级。

## 3. Implementation

- [x] 3.1 最小修改 `applyFriend`，捕获好友申请推送异常并记录日志，继续尝试通知中心写入。
- [x] 3.2 最小修改 `approveFriend`，捕获好友通过推送异常并记录日志，继续尝试通知中心写入。

## 4. Validation

- [x] 4.1 运行目标测试。
- [x] 4.2 运行 `mallchat-chat-service` 模块回归。
- [x] 4.3 使用测试验证人/Code Reviewer 子智能体只读复核变更。
- [x] 4.4 新增验收文档并归档 OpenSpec change。
- [x] 4.5 运行归档后 `openspec validate --all --strict`、`git diff --check`，提交并推送。
