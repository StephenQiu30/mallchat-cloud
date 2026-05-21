## 1. OpenSpec

- [x] 1.1 创建 `add-admin-audit-search-api` change。
- [x] 1.2 扩展 `sensitive-operation-audit` 管理员检索能力。
- [x] 1.3 运行 `openspec validate add-admin-audit-search-api --strict`。

## 2. TDD

- [x] 2.1 先补 `OperationLogServiceImplTest`，覆盖操作人名称检索。
- [x] 2.2 先补创建时间范围检索。
- [x] 2.3 运行 focused test 确认 RED。

## 3. Implementation

- [x] 3.1 扩展 `OperationLogQueryRequest` 的时间范围字段。
- [x] 3.2 在 `OperationLogServiceImpl#getQueryWrapper` 中补齐最小过滤条件。
- [x] 3.3 保持 Controller、VO 和数据库结构不变。

## 4. Validation

- [x] 4.1 运行 log-service focused tests。
- [x] 4.2 运行 OpenSpec strict 校验。
- [x] 4.3 同步 m10 验收文档和 GitHub Issue #47。
