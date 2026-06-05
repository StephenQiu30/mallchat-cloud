---
layer: Acceptance
doc_no: "001"
audience:
  - PM
  - Dev
  - QA
  - Ops
feature_area: im-system
purpose: "IM 系统生产化验收 RAG 看板，追踪 P0/P1 feature 的验收状态、红绿测试证据和残余风险。"
canonical_path: "docs/acceptance/001-im-e2e-rag-acceptance.md"
status: active
version: "1.0.0"
owner: "StephenQiu30"
inputs:
  - "docs/prd/001-im-system-srd.md"
  - "docs/plans/001-im-production-task-orchestration-plan.md"
outputs:
  - "IM 系统生产化验收结论"
triggers:
  - "子 Feature 进入验收阶段"
  - "PR 需要引用验收证据"
downstream:
  - "docs/operations/im-runbook.md"
---

# IM 系统生产化 RAG 验收看板

## 1. 背景

IM 系统生产化增强涉及多个核心 Feature（消息可靠性、一致性、权限等）。为了避免仅使用 Linear Issue 状态替代真实的生产化验收，本看板通过 Red、Amber、Green（RAG）状态来管理每个 Feature 的真实验收进度。

## 2. 状态定义

| 状态 | 含义 | 准入/准出要求 |
| --- | --- | --- |
| 🔴 **Red** | 初始状态或验收未通过 | 不得进入生产环境验收，存在阻塞性缺陷或缺乏 E2E 证据。 |
| 🟡 **Amber** | 验收中或存在延期风险 | 必须记录延期风险和缓解措施，核心链路通畅但有残余风险。 |
| 🟢 **Green** | 验收通过 | TDD 红绿测试通过，E2E 场景覆盖，无阻塞性风险，可发布。 |

## 3. RAG 验收看板

| Feature | 状态 | TDD (Red/Green) | E2E 场景 | 残余风险 | 关联 PR/Issue |
| --- | --- | --- | --- | --- | --- |
| `im-rag-acceptance-dashboard` | 🟢 Green | N/A (Docs-only) | Manual Verification | 无 | [STE-208](https://linear.app/stephenqiu/issue/STE-208) |
| `im-e2e-test-harness` | 🔴 Red | TBD | TBD | 尚未建立 | [STE-207](https://linear.app/stephenqiu/issue/STE-207) |
| `message-send-idempotency` | 🔴 Red | TBD | TBD | 尚未实现 | [STE-209](https://linear.app/stephenqiu/issue/STE-209) |
| `message-delivery-reliability` | 🔴 Red | TBD | TBD | 尚未实现 | [STE-210](https://linear.app/stephenqiu/issue/STE-210) |
| `session-consistency` | 🔴 Red | TBD | TBD | 尚未实现 | [STE-211](https://linear.app/stephenqiu/issue/STE-211) |
| `message-recovery-observability` | 🔴 Red | TBD | TBD | 尚未实现 | [STE-212](https://linear.app/stephenqiu/issue/STE-212) |
| `friend-apply-lifecycle` | 🔴 Red | TBD | TBD | 尚未实现 | [STE-213](https://linear.app/stephenqiu/issue/STE-213) |
| `friend-message-permission` | 🔴 Red | TBD | TBD | 尚未实现 | [STE-214](https://linear.app/stephenqiu/issue/STE-214) |
| `friend-cache-degradation` | 🔴 Red | TBD | TBD | 尚未实现 | [STE-215](https://linear.app/stephenqiu/issue/STE-215) |
| `group-member-lifecycle` | 🔴 Red | TBD | TBD | 尚未实现 | [STE-216](https://linear.app/stephenqiu/issue/STE-216) |
| `group-message-permission` | 🔴 Red | TBD | TBD | 尚未实现 | [STE-217](https://linear.app/stephenqiu/issue/STE-217) |
| `group-session-consistency` | 🔴 Red | TBD | TBD | 尚未实现 | [STE-218](https://linear.app/stephenqiu/issue/STE-218) |
| `moment-publish-feed` | 🔴 Red | TBD | TBD | 尚未实现 | [STE-219](https://linear.app/stephenqiu/issue/STE-219) |
| `moment-like-comment-idempotency` | 🔴 Red | TBD | TBD | 尚未实现 | [STE-220](https://linear.app/stephenqiu/issue/STE-220) |
| `moment-permission-boundary` | 🔴 Red | TBD | TBD | 尚未实现 | [STE-221](https://linear.app/stephenqiu/issue/STE-221) |

## 4. 验收准则

每个 P0 Feature 必须满足以下条件方可标记为 🟢 **Green**：

1. **TDD 证据**：提供失败的 `test:` 提交和修复后的绿色测试结果。
2. **E2E 覆盖**：关联至少一个 E2E 或等价验收场景。
3. **无残余风险**：所有已知风险已关闭或有明确的长期 Issue 追踪且不影响核心生产。

## 5. 变更记录

| 日期 | 作者 | 版本 | 变更说明 |
| --- | --- | --- | --- |
| 2026-06-06 | StephenQiu30 | 1.0.0 | 建立 IM RAG 验收看板，初始化 P0 feature 列表。 |
