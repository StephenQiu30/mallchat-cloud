---
layer: Plan
doc_no: "PL-001"
audience:
  - PM
  - Dev
  - QA
feature_area: im-real-time-communication
purpose: "拆解 MallChat IM MVP 的 OpenSpec 认领、自动化执行和验收顺序。"
canonical_path: "docs/plans/PL-001-im-mvp-openspec-plan.md"
status: draft
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "../prd/P-001-im-real-time-communication-prd.md"
outputs:
  - "OpenSpec change 执行路线"
triggers:
  - "启动或继续 IM MVP 实现"
downstream:
  - "../acceptance/A-002-im-mvp-research-acceptance.md"
---

# MallChat IM MVP OpenSpec 执行计划

## 1. 执行顺序

1. 后端仓 `orchestrate-im-product-mvp`：认领 PRD、跨端职责和后端能力边界。
2. Taro 仓 `restore-demo-im-mobile-shell`：认领 `demo.html` 风格还原和移动端主链路修正。
3. UniApp/Flutter/Admin：后续按 Taro 已验证体验拆分单独 change，不在本轮混入。

## 2. 本轮自动化执行范围

1. 创建 PRD、计划、OpenSpec proposal/design/tasks/spec delta。
2. 修正 Taro 联系人进入私聊的房间 ID 链路。
3. 补齐聊天输入栏语音、表情、图片、更多入口，使其更贴近设计稿。
4. 执行 OpenSpec 与 CI 等价命令。

## 3. 后续任务池

| 阶段 | 任务 | 验收方式 |
| --- | --- | --- |
| P1 | 管理端群聊/消息治理 | `pnpm run tsc` + 页面状态 |
| P1 | UniApp 同风格主页面 | `npm run type-check` |
| P1 | Flutter 同风格移动页 | `flutter analyze` + `flutter test` |
| P1 | 已读回执详情 | 后端单测 + OpenSpec |

## 4. 风险控制

- 单次变更只在一个端做代码实现，避免多端 CI 同时漂移。
- 后端本轮保持 PRD/OpenSpec 编排，不做 schema 重构。
- 所有正式结论写入 docs 或 OpenSpec，不保留一次性报告。

## 5. 验收门禁

- `openspec validate --strict` 通过。
- 相关仓库 CI 等价命令通过。
- `docs/acceptance` 记录测试结论。
