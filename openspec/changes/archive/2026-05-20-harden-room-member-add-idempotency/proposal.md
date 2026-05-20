# Change: harden-room-member-add-idempotency

## Summary

保护受控入群路径的幂等语义：当成员已经存在时，重复调用 `addMember(roomId, userId, role)` 不应覆盖既有角色，避免群主或管理员被重复邀请路径降级为普通成员。

## Motivation

`ChatRoomMemberServiceImpl#addMember` 当前同时承担“创建成员”和“已有成员按传入 role 更新角色”的行为。创建群、邀请群成员、私聊初始化等路径都会复用该方法，其中邀请路径通常传入 `MEMBER`。如果目标用户已经是 `OWNER` 或 `ADMIN`，重复邀请会把角色覆盖成 `MEMBER`，破坏群权限边界。

本变更只修正幂等加入的角色保护，不引入管理员任命、群主转让、入群审批或角色操作日志。

## Scope

- 新增测试锁定已有 `OWNER` / `ADMIN` / `MEMBER` 重复加入时不发生角色更新。
- 调整 `addMember(roomId, userId, role)`：已有成员直接返回；首次加入仍按传入 role 或默认 `MEMBER` 创建。
- 保持现有缓存同步、会话、通知和群邀请流程不变。

## Non-Goals

- 不新增群管理员设置接口。
- 不新增群主转让或角色审计表。
- 不调整群邀请审批模型。
- 不改动群成员移除、退群和解散流程。

## Validation

- `openspec validate harden-room-member-add-idempotency --strict`
- `mvn -pl :mallchat-chat-service -am test -Dtest=ChatRoomMemberServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
- `mvn -pl :mallchat-chat-service -am test`
- `openspec validate --all --strict`
