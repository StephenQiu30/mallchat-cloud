# PL-IM-009: 安全权限 可靠性和体验增强 实施计划

## 关联 PRD

- [P-IM-009: 安全权限 可靠性和体验增强](../prd/P-IM-009-security-permission-prd.md)

## TDD 策略

RED 先覆盖频控、拉黑收敛、撤回权限、管理员权限和审计事件；GREEN 只实现本阶段增强。

## 实施步骤

### Phase 1: 基础设施 (chore/docs)

1. 创建 PRD 和 Plan 文档
2. 新增 `chat_audit_event` 表 SQL
3. 补齐 ErrorCode 新增错误码

### Phase 2: RED 测试 (test)

1. `ChatSecurityEnhancementTest` — 6 个核心测试：
   - `shouldRejectSendMessageWhenRateLimited` — 频控测试
   - `shouldRejectRecallWhenBlockedByTargetUser` — 拉黑收敛测试
   - `shouldAllowOwnerToRecallAnyMessageInRoom` — 群主撤回权限测试
   - `shouldAllowAdminToRecallRegularMemberMessageInRoom` — 管理员撤回测试
   - `shouldRejectAdminRecallOfOtherAdminMessage` — 管理员越权撤回测试
   - `shouldRecordAuditEventOnMessageRecall` — 审计事件测试

### Phase 3: GREEN 实现 (impl/feat)

1. ChatAuditEvent 实体/Mapper/Service
2. ChatAuditEventRecorder — 异步审计记录器
3. 频控集成 — sendMessage 调用 RateLimitUtils
4. 拉黑收敛 — recallMessage 检查 isBlockedBetween
5. 撤回权限增强 — 群主/管理员可撤回群内消息
6. 审计事件集成 — recallMessage 记录审计

## 文件变更清单

### 新增文件

- docs/prd/P-IM-009-security-permission-prd.md
- docs/plans/PL-IM-009-security-permission-plan.md
- sql/V20260603__add_chat_audit_event.sql
- ChatAuditEvent.java, ChatAuditEventMapper.java
- ChatAuditEventService.java, ChatAuditEventServiceImpl.java
- ChatAuditEventRecorder.java
- ChatSecurityEnhancementTest.java

### 修改文件

- ErrorCode.java — 新增错误码
- ChatMessageServiceImpl.java — 频控/拉黑收敛/撤回权限
- ChatRoomServiceImpl.java — 管理员权限
