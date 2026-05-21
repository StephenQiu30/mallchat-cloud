## 1. OpenSpec

- [x] 1.1 创建 `add-room-admin-role` change。
- [x] 1.2 明确管理员任免只复用现有 `role` 字段。
- [x] 1.3 运行 OpenSpec strict 校验。

## 2. TDD

- [x] 2.1 先补 `ChatRoomServiceImplTest` 管理员任命 RED 测试。
- [x] 2.2 先补 `ChatRoomServiceImplTest` 管理员取消 RED 测试。
- [x] 2.3 先补非群主、群主目标等拒绝场景。

## 3. Implementation

- [x] 3.1 新增 `ChatRoomAdminRoleRequest`。
- [x] 3.2 新增 `/chat/room/member/admin/grant` 与 `/chat/room/member/admin/revoke`。
- [x] 3.3 `ChatRoomService` 增加 `grantAdmin` / `revokeAdmin`，复用 `ChatRoomMemberService` 更新角色。

## 4. Validation

- [x] 4.1 运行 m7 聚焦 Maven 测试。
- [x] 4.2 运行 chat/notification service 全量测试。
- [x] 4.3 同步 GitHub Issue #30。
