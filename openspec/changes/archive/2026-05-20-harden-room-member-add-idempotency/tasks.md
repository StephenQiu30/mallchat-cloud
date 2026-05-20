## 1. OpenSpec

- [x] 1.1 增加 `chat-room-access` delta，明确受控入群幂等不覆盖已有成员角色。
- [x] 1.2 运行 `openspec validate harden-room-member-add-idempotency --strict`。

## 2. TDD

- [x] 2.1 先写 `ChatRoomMemberServiceImplTest`，覆盖已有 `OWNER` / `ADMIN` / `MEMBER` 重复加入不更新角色。
- [x] 2.2 运行目标测试确认红灯来自现有角色覆盖行为。

## 3. Implementation

- [x] 3.1 最小修改 `ChatRoomMemberServiceImpl#addMember`，已有成员直接幂等返回。
- [x] 3.2 保持首次加入、缓存写入和参数校验行为不变。

## 4. Validation

- [x] 4.1 运行 `ChatRoomMemberServiceImplTest` 目标测试。
- [x] 4.2 运行 `mallchat-chat-service` 模块回归。
- [x] 4.3 使用测试验证人/Code Reviewer 子智能体只读复核变更。
- [x] 4.4 新增验收文档并归档 OpenSpec change。
- [x] 4.5 运行归档后 `openspec validate --all --strict`、`git diff --check`，提交并推送。
