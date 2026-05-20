---
layer: Acceptance
doc_no: "A-009"
audience:
  - Dev
  - QA
feature_area: room-member-add-idempotency
purpose: "记录受控入群幂等角色保护的验收范围、测试证据与剩余风险。"
canonical_path: "docs/acceptance/A-009-room-member-add-idempotency-acceptance.md"
status: complete
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "../prd/P-004-group-chat-governance-prd.md"
  - "../../openspec/changes/archive/2026-05-20-harden-room-member-add-idempotency"
outputs:
  - "群成员加入幂等角色保护验收结论"
triggers:
  - "harden-room-member-add-idempotency 完成后验收"
downstream:
  - "../../openspec/specs/chat-room-access/spec.md"
---

# 群成员加入幂等角色保护验收

## 1. 变更验收范围

- `ChatRoomMemberServiceImpl#addMember(roomId, userId, role)` 对已有成员保持幂等返回。
- 重复受控入群不得把已有 `OWNER` 降级为 `MEMBER`。
- 重复受控入群不得把已有 `ADMIN` 降级为 `MEMBER`。
- 已有 `MEMBER` 重复加入不产生角色更新。
- 首次加入、缓存写入和参数校验路径保持现有行为。

## 2. TDD 证据

| 阶段 | 命令 | 结果 | 结论 |
| --- | --- | --- | --- |
| OpenSpec change | `openspec validate harden-room-member-add-idempotency --strict` | change 合法 | 通过 |
| 红灯 | `mvn -pl :mallchat-chat-service -am test -Dtest=ChatRoomMemberServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false` | `2 failures`，`OWNER` / `ADMIN` 被覆盖为 `MEMBER` | 通过 |
| 绿灯 | 同上 | `Tests run: 4, Failures: 0, Errors: 0` | 通过 |
| 测试验证人复核 | 子智能体“测试验证人/Code Reviewer”只读审核 | 无 P0/P1，建议补 `userId == null` 和反向角色覆盖用例 | 已补充 |
| 补充测试绿灯 | 同上 | `Tests run: 6, Failures: 0, Errors: 0` | 通过 |
| chat-service 回归 | `mvn -pl :mallchat-chat-service -am test` | `Tests run: 136, Failures: 0, Errors: 0` | 通过 |
| OpenSpec 全量 | `openspec validate --all --strict` | `10 passed, 0 failed` | 通过 |
| 归档后 OpenSpec 全量 | `openspec validate --all --strict` | `9 passed, 0 failed` | 通过 |
| diff 检查 | `git diff --check` | 无空白错误 | 通过 |

## 3. 结论

本切片只修复受控入群的幂等角色保护：已有成员再次进入 `addMember` 时不再按传入 `role` 覆盖原角色，从而避免重复群邀请把群主或管理员降级。角色任命、群主转让、审批入群和角色审计仍不属于本次范围。

## 4. 残余风险

1. 未来如果需要角色变更，应新增显式群角色管理接口并补充权限、审计和并发测试。
2. 历史库中若已经存在被降级的数据，本次代码不会自动修复。
