## 1. OpenSpec

- [x] 1.1 创建 `harden-sensitive-operation-audit` change。
- [x] 1.2 明确复用现有 `@OperationLog` AOP，不新建审计体系。
- [x] 1.3 运行 `openspec validate harden-sensitive-operation-audit --strict`。

## 2. TDD

- [x] 2.1 先补 `OperationLogAspectTest`，覆盖敏感参数脱敏。
- [x] 2.2 先补 `OperationLogAspectTest`，覆盖失败操作仍记录审计并抛出原异常。
- [x] 2.3 运行目标测试确认红灯来自敏感参数未脱敏。
- [x] 2.4 先补 `ChatOperationLogRecorderImplTest`，覆盖 chat-service 敏感操作审计会转发到日志服务。

## 3. Implementation

- [x] 3.1 最小修改 `OperationLogAspect`，序列化请求参数时屏蔽敏感字段。
- [x] 3.2 保持现有 module/action/success/errorMessage 记录行为不变。
- [x] 3.3 最小增加 `ChatOperationLogRecorderImpl`，让 chat-service 已有 `@OperationLog` 能持久化到日志服务。

## 4. Validation

- [x] 4.1 运行 common-log 目标测试。
- [x] 4.2 运行 common-log 模块测试。
- [x] 4.3 运行 chat-service recorder 目标测试。
- [x] 4.4 运行 OpenSpec strict 校验。
- [x] 4.5 更新 GitHub Issue #9 和 Epic 验收记录。
