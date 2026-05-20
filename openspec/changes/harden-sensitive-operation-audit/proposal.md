## Why

MallChat 已有 `@OperationLog` 审计体系，并且聊天、好友、群、动态等敏感操作已经接入。生产可用阶段更需要保证这些审计日志既能追踪关键操作，又不会把验证码、token、密码等敏感字段原样落库。

## What Changes

- 保持现有 `@OperationLog` AOP 与各服务 `OperationLogRecorder` 机制。
- 在记录请求参数时对常见敏感字段做统一脱敏。
- 增加 AOP 单元测试，覆盖成功审计、失败审计和敏感参数脱敏。

## Non-Goals

- 不新增独立审计日志表。
- 不改变现有 OperationLog API DTO 和数据库结构。
- 不引入复杂规则引擎或后台配置。
