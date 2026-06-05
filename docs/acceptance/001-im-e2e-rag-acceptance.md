---
layer: Acceptance
doc_no: "001"
audience:
  - QA
  - Dev
feature_area: im-core
purpose: "统一定义 IM 生产化各功能的端到端（E2E）和 RAG 验收标准"
canonical_path: "docs/acceptance/001-im-e2e-rag-acceptance.md"
status: draft
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "docs/prd/001-im-system-srd.md"
  - "docs/design/D-001-im-production-architecture.md"
outputs:
  - "无"
triggers:
  - "进行具体子特性的验收时"
downstream:
  - "无"
---

# IM 端到端及 RAG 验收标准

## 1. 背景

规范各子特性的验收行为，确保满足 TDD, E2E 和 RAG 验证门槛。

## 2. 目标

- 明确每个 Feature 的必需要求：红绿测试记录、执行命令、RAG 状态报告。
- 确保代码风格符合 `Controller + Service + Mapper + Entity + Convert + DTO/VO`。
- 通过项目级仓库门禁。

## 3. 核心内容

1. **测试标准**：
   - 所有特性必须提交对应的测试证据（红测试 -> 绿测试）。
2. **命令验证**：
   - 必须记录验证该特性的具体命令。
3. **CI 门禁**：
   - 必须通过 `bash scripts/validate-repository.sh`。
4. **架构合规**：
   - 验收阶段验证层级调用关系，不得跨层调用或混入违规组件。

## 4. 关联文档
### 4.1 输入文档
1. `docs/prd/001-im-system-srd.md`
2. `docs/design/D-001-im-production-architecture.md`

## 5. 变更记录
| 日期 | 作者 | 版本 | 变更说明 |
| --- | --- | --- | --- |
| 2026-06-06 | Gemini Agent | 0.1.0 | 初始化文档 |
