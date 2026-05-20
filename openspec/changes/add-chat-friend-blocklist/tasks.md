## 1. OpenSpec

- [x] 1.1 创建 `add-chat-friend-blocklist` change。
- [x] 1.2 明确拉黑对好友申请、私聊和动态可见性的最小影响。
- [x] 1.3 运行 `openspec validate add-chat-friend-blocklist --strict`。

## 2. TDD

- [x] 2.1 先补 `UserFriendServiceImplTest`，覆盖拉黑、解除拉黑和好友集合过滤。
- [x] 2.2 先补 `UserFriendApplyServiceImplTest`，覆盖拉黑后不能发起好友申请。
- [x] 2.3 先补 `ChatMessageServiceImplTest`，覆盖拉黑后私聊消息被拒绝。
- [x] 2.4 运行目标测试确认 RED。

## 3. Implementation

- [x] 3.1 新增 `UserFriendBlock`、Mapper 和 Service 方法。
- [x] 3.2 好友申请和审批复用 `isBlockedBetween`。
- [x] 3.3 私聊发送权限复用 `isBlockedBetween`。
- [x] 3.4 好友 ID 集合过滤黑名单，动态流沿用现有可见性。

## 4. Validation

- [x] 4.1 运行 m6 聚焦 Maven 测试。
- [x] 4.2 运行 OpenSpec strict 校验。
- [x] 4.3 同步 GitHub Issue #27。
