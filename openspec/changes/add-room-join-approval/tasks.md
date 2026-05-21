## 1. OpenSpec

- [x] 1.1 创建 `add-room-join-approval` change。
- [x] 1.2 明确申请事实、审批权限和通知降级边界。
- [x] 1.3 运行 OpenSpec strict 校验。

## 2. TDD

- [x] 2.1 先补 `ChatRoomJoinApplyServiceImplTest` 申请、重复申请、已成员拒绝。
- [x] 2.2 先补审核同意、拒绝、普通成员越权、通知失败降级。
- [x] 2.3 补充待处理状态条件更新的并发审批防重测试。

## 3. Implementation

- [x] 3.1 新增 DTO/VO、Entity、Mapper、Convert、Service、Controller。
- [x] 3.2 新增 `chat_room_join_apply` SQL。
- [x] 3.3 同意申请后复用 `ChatRoomMemberService.addMember` 和 `ChatSessionService.updateSession`。
- [x] 3.4 审批更新按 `id + status=PENDING` 条件落库，避免重复审批竞态。

## 4. Validation

- [x] 4.1 运行 m7 聚焦 Maven 测试。
- [x] 4.2 运行 chat/notification service 全量测试。
- [x] 4.3 同步 GitHub Issue #31。
