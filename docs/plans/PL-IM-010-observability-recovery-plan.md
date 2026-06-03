---
layer: Plan
doc_no: "PL-IM-010"
audience:
  - PM
  - Dev
  - QA
feature_area: observability-recovery
purpose: "定义可观测性与恢复 P0 实现计划与 TDD 交付顺序。"
canonical_path: "docs/plans/PL-IM-010-observability-recovery-plan.md"
status: draft
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "docs/prd/P-IM-010-observability-recovery-prd.md"
downstream:
  - "STE-93"
---

# P-IM-010 可观测性与恢复实现计划

## P0 任务（STE-93）

| 任务 | 描述 | 验收 |
|------|------|------|
| P0-01 | `OpsMetricsRecorder` | recovery/consistency 指标可记录 |
| P0-02 | `ImCoreConsistencyCheckService` | 20 条核心断言，覆盖 5 领域 |
| P0-03 | `RecoveryDryRunService` | dry-run 检查点 + 隔离库 execute |
| P0-04 | `OpsController` | 管理员 REST + Swagger 独立 Tag |

## TDD 顺序

1. `test:` 失败测试（指标、一致性、dry-run）
2. `impl:` 最小实现
3. `docs:` PRD/Plan
4. `chore:` CI focused tests

## 验证命令

```bash
mvn -B -pl mallchat-service/mallchat-chat-service -am \
  -Dtest=OpsMetricsRecorderTest,ImCoreConsistencyCheckServiceImplTest,RecoveryDryRunServiceImplTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
bash scripts/validate-repository.sh
```
