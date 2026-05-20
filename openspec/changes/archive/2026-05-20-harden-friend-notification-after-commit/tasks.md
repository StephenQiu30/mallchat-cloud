## 1. OpenSpec

- [x] 1.1 增加 `chat-friend` delta，明确好友申请/通过通知在事务同步存在时 afterCommit 创建。
- [x] 1.2 运行 `openspec validate harden-friend-notification-after-commit --strict`。

## 2. TDD

- [x] 2.1 先补 `UserFriendApplyServiceImplTest`，覆盖好友申请通知在事务同步存在时提交前不发送、afterCommit 后发送。
- [x] 2.2 先补 `UserFriendApplyServiceImplTest`，覆盖好友通过通知在事务同步存在时提交前不发送、afterCommit 后发送。
- [x] 2.3 运行目标测试确认红灯来自现有通知提前发送。

## 3. Implementation

- [x] 3.1 最小修改 `trySendFriendNotification`，事务同步存在时注册 `afterCommit`。
- [x] 3.2 保持无事务同步时立即发送、通知失败只记录 warn 的原有行为。

## 4. Validation

- [x] 4.1 运行目标测试。
- [x] 4.2 运行 `mallchat-chat-service` 模块回归。
- [x] 4.3 使用测试验证人/Code Reviewer 子智能体只读复核变更。
- [x] 4.4 新增验收文档并归档 OpenSpec change。
- [x] 4.5 运行归档后 `openspec validate --all --strict`、`git diff --check`，提交并推送。
