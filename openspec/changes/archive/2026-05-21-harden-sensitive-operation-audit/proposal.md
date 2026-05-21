## Why

MallChat 已有 `@OperationLog` 审计体系，并且聊天、好友、群、动态等敏感操作已经接入。生产可用阶段更需要保证这些审计日志既能追踪关键操作，又不会把验证码、token、密码等敏感字段原样落库。

## What Changes

- 保持现有 `@OperationLog` AOP 与各服务 `OperationLogRecorder` 机制。
- 在记录请求参数时对常见敏感字段做统一脱敏。
- 增加 `bizId` 审计字段与查询过滤，便于按业务对象追溯敏感操作。
- 文件上传复用现有 `file_upload_record` 表记录成功和失败审计。
- 增加 AOP、日志服务、聊天服务和文件服务单元测试，覆盖成功审计、失败审计、敏感参数脱敏、兜底不泄露、`bizId` 查询、文件上传记录和异步启用。

## Non-Goals

- 不新增独立审计日志表。
- 不引入复杂审计工作流；`OperationLog` 只最小增加 `bizId` 追溯字段。
- 不引入复杂规则引擎或后台配置。
