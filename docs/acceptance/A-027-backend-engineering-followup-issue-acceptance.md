---
layer: Acceptance
doc_no: "A-027"
audience:
  - PM
  - Dev
  - QA
  - Ops
feature_area: backend-engineering-consistency
purpose: "记录 E2/E3/E4 后端工程化一致性治理的任务编排、Issue 生成和自审结论。"
canonical_path: "docs/acceptance/A-027-backend-engineering-followup-issue-acceptance.md"
status: review
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "docs/design/D-003-backend-engineering-consistency-design.md"
  - "docs/plans/PL-008-backend-engineering-consistency-plan.md"
  - "docs/plans/PL-009-backend-engineering-consistency-followup-plan.md"
  - "docs/acceptance/A-026-m11-chat-consistency-acceptance.md"
outputs:
  - "m12/m13/m14 GitHub Issue 编排记录"
  - "E2/E3/E4 任务自审结论"
triggers:
  - "完成 m11 Chat 工程化一致性治理后"
downstream:
  - "m12/m13/m14 GitHub Issue 与 PR"
---

# 后端工程化一致性后续批次编排验收

## 1. 编排范围

| 批次 | 范围 | 计划 PR | 状态 |
| --- | --- | --- | --- |
| E2 | `log/file/notification` 支撑领域 | `m12` | Issue 已创建，待消费 |
| E3 | `user/ai/gateway/common` 基础领域 | `m13` | Issue 已创建，待消费 |
| E4 | 工程化守护 | `m14` | Issue 已创建，待消费 |

## 2. 静态事实输入

### 2.1 E2 候选问题

1. `mallchat-api-log` / `mallchat-log-service` 中存在裸 `Boolean` 响应和通用 `DeleteRequest`。
2. `mallchat-api-file` / `mallchat-file-service` 文件上传接口使用 multipart + `bizType`，需要审查是否保持上传兼容边界。
3. `mallchat-api-notification` / `mallchat-notification-service` 中存在裸 `Long`、裸 `Boolean`、`List<Long>`、`@RequestParam("id")` 和通用 `DeleteRequest`。

### 2.2 E3 候选问题

1. `mallchat-user-service` 中存在裸 `Boolean` / `Long` 响应、`@RequestParam` 和通用 `DeleteRequest`。
2. `mallchat-api-user` Feign 契约存在 `@RequestParam` 和裸 `Boolean`。
3. `mallchat-api-ai` / `mallchat-ai-service` 中存在通用 `DeleteRequest` 和裸 `Boolean`。
4. `common` 中的公共契约属于历史基础能力，不能以治理名义破坏性删除。
5. `gateway` 使用 WebFlux，不应机械套用 MVC Controller DTO 规则。

## 3. Issue 生成计划

| 批次 | Epic | 子任务 |
| --- | --- | --- |
| E2 | [#58](https://github.com/StephenQiu30/mallchat-cloud/issues/58) `[m12][epic][backend] 支撑领域工程化一致性治理` | [#59](https://github.com/StephenQiu30/mallchat-cloud/issues/59) 审查清单；[#60](https://github.com/StephenQiu30/mallchat-cloud/issues/60) log 契约；[#61](https://github.com/StephenQiu30/mallchat-cloud/issues/61) file 契约；[#62](https://github.com/StephenQiu30/mallchat-cloud/issues/62) notification 契约；[#63](https://github.com/StephenQiu30/mallchat-cloud/issues/63) QA Review |
| E3 | [#64](https://github.com/StephenQiu30/mallchat-cloud/issues/64) `[m13][epic][backend] 基础领域工程化一致性治理` | [#65](https://github.com/StephenQiu30/mallchat-cloud/issues/65) user 契约；[#66](https://github.com/StephenQiu30/mallchat-cloud/issues/66) ai 契约；[#67](https://github.com/StephenQiu30/mallchat-cloud/issues/67) gateway 边界；[#68](https://github.com/StephenQiu30/mallchat-cloud/issues/68) common 边界；[#69](https://github.com/StephenQiu30/mallchat-cloud/issues/69) QA Review |
| E4 | [#70](https://github.com/StephenQiu30/mallchat-cloud/issues/70) `[m14][epic][backend] 工程化守护规则落地` | [#71](https://github.com/StephenQiu30/mallchat-cloud/issues/71) 契约守护测试；[#72](https://github.com/StephenQiu30/mallchat-cloud/issues/72) 仓库校验脚本；[#73](https://github.com/StephenQiu30/mallchat-cloud/issues/73) PR checklist；[#74](https://github.com/StephenQiu30/mallchat-cloud/issues/74) CI gate；[#75](https://github.com/StephenQiu30/mallchat-cloud/issues/75) QA Review |

## 4. 自审结论

1. 拆分顺序合理：先支撑领域，再基础领域，最后守护规则。
2. 边界没有混淆：E2 不碰 user/ai/gateway/common；E3 不碰 log/file/notification 的业务修正；E4 不新增业务功能。
3. 风格一致性可继承 m11：DTO Request / VO Response、Feign 同步、TDD、只读 Code Review 均保留。
4. 已避免过度设计：不引入新生成器、不做复杂静态平台、不强制 multipart 和 WebFlux 走不合适的 DTO 模式。
5. 测试优先原则明确：纯审查 issue 不伪造 RED；代码和契约变化必须先 RED。
6. GitHub Issue 正文已使用 Markdown 分段和 `Parent Epic: #xx` 关联格式，避免出现转义换行或正文粘连。
7. 原计划中的 `m144` 已按笔误修正为 `m14`，后续 Issue、文档和 PR 编号统一使用 `m14`。

## 5. 验收命令

```bash
openspec validate --all --strict
git diff --check
bash scripts/validate-repository.sh
```

本轮编排验证结果：

| 命令 | 结果 |
| --- | --- |
| `openspec validate --all --strict` | 通过，21 items passed |
| `git diff --check` | 通过 |
| `bash scripts/validate-repository.sh` | 通过 |

## 6. 后续待回填

1. 每个 Epic 的 PR 编号。
2. 后续 m12/m13/m14 的实际验收命令和 Code Review 结果。
3. Issue 关闭和 PR 合并结果。

## 7. 变更记录

| 日期 | 作者 | 版本 | 变更说明 |
| --- | --- | --- | --- |
| 2026-05-21 | StephenQiu30 | 0.1.0 | 初始化 E2/E3/E4 后续批次编排验收 |
