## 1. OpenSpec

- [x] 1.1 创建 `harden-sensitive-operation-audit` change。
- [x] 1.2 明确复用现有 `@OperationLog` AOP，不新建审计体系。
- [x] 1.3 运行 `openspec validate harden-sensitive-operation-audit --strict`。

## 2. TDD

- [x] 2.1 先补 `OperationLogAspectTest`，覆盖敏感参数脱敏。
- [x] 2.2 先补 `OperationLogAspectTest`，覆盖失败操作仍记录审计并抛出原异常。
- [x] 2.3 运行目标测试确认红灯来自敏感参数未脱敏。
- [x] 2.4 先补 `ChatOperationLogRecorderImplTest`，覆盖 chat-service 敏感操作审计会转发到日志服务。
- [x] 2.5 补充 `OperationLogAspectTest`，覆盖 `bizId` 提取和序列化兜底不泄露原始敏感参数。
- [x] 2.6 补充 `OperationLogServiceImplTest`，覆盖操作日志按 `bizId` 精确过滤。
- [x] 2.7 补充 `FileUploadRecordRecorderTest`，覆盖文件上传成功与失败审计记录。
- [x] 2.8 补充 `ChatServiceApplicationTest`，覆盖聊天服务开启异步审计执行。
- [x] 2.9 补充 `FileServiceApplicationTest`，覆盖文件服务开启异步上传记录。
- [x] 2.10 补充 `OperationLogAspectTest`，覆盖具体业务 ID 优先于泛化 `id`。

## 3. Implementation

- [x] 3.1 最小修改 `OperationLogAspect`，序列化请求参数时屏蔽敏感字段。
- [x] 3.2 保持现有 module/action/success/errorMessage 记录行为不变。
- [x] 3.3 最小增加 `ChatOperationLogRecorderImpl`，让 chat-service 已有 `@OperationLog` 能持久化到日志服务。
- [x] 3.4 最小增加 `bizId` 字段、SQL 索引和查询条件，支持按业务对象追溯敏感操作。
- [x] 3.5 最小增加 file-service 文件上传记录器，复用现有 `file_upload_record` 表和 `LogFeignClient`。
- [x] 3.6 为 chat-service 与 file-service 开启 `@Async`，保证审计记录不阻塞核心业务。
- [x] 3.7 文件上传审计在同步线程提取文件元数据快照，异步记录不再读取 `MultipartFile`。

## 4. Validation

- [x] 4.1 运行 common-log 目标测试。
- [x] 4.2 运行 common-log 模块测试。
- [x] 4.3 运行 chat-service recorder 目标测试。
- [x] 4.4 运行 OpenSpec strict 校验。
- [x] 4.5 更新 GitHub Issue #9 和 Epic 验收记录。
