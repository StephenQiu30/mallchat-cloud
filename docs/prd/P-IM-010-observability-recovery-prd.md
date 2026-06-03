---
layer: PRD
doc_no: "P-IM-010"
audience:
  - PM
  - Dev
  - QA
feature_area: observability-recovery
purpose: "定义 IM 可观测性与数据恢复演练的产品边界、运维入口和验收标准。"
canonical_path: "docs/prd/P-IM-010-observability-recovery-prd.md"
status: draft
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "MallChat IM 核心表与现有 Micrometer 指标"
outputs:
  - "可观测性与恢复 PRD"
  - "P0 运维闭环验收依据"
downstream:
  - "docs/plans/PL-IM-010-observability-recovery-plan.md"
---

# P-IM-010 可观测性与恢复 PRD

## 1. 背景

MallChat IM 已具备业务指标（`mallchat.im.business.total`）、推送指标与 RabbitMQ 发布指标，但缺少面向运维的恢复 dry-run 入口和核心表一致性检查 API。数据恢复脚本已存在于仓库，需要产品化入口与可验证验收。

## 2. 目标

1. 提供管理员可用的核心表一致性只读检查，覆盖好友、房间、消息、会话、动态。
2. 提供恢复 dry-run 流程：默认不修改线上库，execute 模式仅在隔离库演练。
3. 记录 `mallchat.im.recovery.total` 与 `mallchat.im.consistency.total` 运维指标。

## 3. 非目标

- 不替代现有业务事实表与指标体系。
- 不在 P0 实现自动故障切换或在线热恢复。
- 不提供面向普通用户的 UI。

## 4. 验收门禁

- [ ] 管理员可触发核心表一致性检查，五类领域均有断言结果。
- [ ] dry-run 返回完整检查点计划且不写入线上库。
- [ ] 存在备份时可对隔离库执行 execute 演练并通过一致性断言。
- [ ] 运维 API 独立 Swagger 分组且需管理员角色。
