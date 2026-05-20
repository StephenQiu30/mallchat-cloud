## 1. OpenSpec

- [x] 1.1 创建 `verify-im-core-data-recovery` change。
- [x] 1.2 明确核心 IM 表恢复 smoke 范围。
- [x] 1.3 运行 `openspec validate verify-im-core-data-recovery --strict`。

## 2. TDD

- [x] 2.1 先运行缺失恢复脚本命令确认 RED。
- [x] 2.2 固定核心表清单和恢复查询断言。

## 3. Implementation

- [x] 3.1 新增 `scripts/backup-im-core-tables.sh`。
- [x] 3.2 新增 `scripts/verify-im-core-data-recovery.sh`。
- [x] 3.3 更新 `docs/operations/O-003-im-production-runbook.md`。
- [x] 3.4 更新 `docs/acceptance` 验收记录。

## 4. Validation

- [x] 4.1 运行数据恢复脚本 dry-run。
- [x] 4.2 运行 OpenSpec strict 校验。
- [ ] 4.3 同步 GitHub Issue #17。
