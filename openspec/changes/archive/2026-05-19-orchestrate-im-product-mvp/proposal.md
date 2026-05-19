## Why

MallChat 已具备后端 chat 能力和多个客户端雏形，但产品范围、跨端职责和 `demo.html` 视觉还原主线尚未被同一份规范约束。该 change 用于把 QQ-like IM MVP 从调研结论落到可执行的 PRD、计划和 OpenSpec 契约。

## What Changes

- 新增 MallChat IM MVP PRD，定义会话、好友、群聊、聊天详情、动态/我的和管理端后续治理边界。
- 新增执行计划，明确后端/Taro/UniApp/Flutter/Admin 的职责和本轮自动化执行范围。
- 新增 `im-product-mvp` 能力规格，用于约束跨端产品行为和阶段验收。
- 不改动后端运行时代码，不引入新依赖，不改变现有 `chat-*` API 契约。

## Capabilities

### New Capabilities

- `im-product-mvp`: MallChat QQ-like IM MVP 的产品范围、跨端职责和验收门禁。

### Modified Capabilities

- 无。

## Impact

- 文档：`docs/prd/P-001-im-real-time-communication-prd.md`、`docs/plans/PL-001-im-mvp-openspec-plan.md`。
- OpenSpec：`openspec/changes/orchestrate-im-product-mvp/specs/im-product-mvp/spec.md`。
- 代码：无后端代码改动。
