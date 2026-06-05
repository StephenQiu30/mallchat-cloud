---
layer: Design
doc_no: "D-001"
audience:
  - Dev
  - Ops
feature_area: im-core
purpose: "定义 IM 系统生产化架构与技术决策边界"
canonical_path: "docs/design/D-001-im-production-architecture.md"
status: draft
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "docs/prd/001-im-system-srd.md"
outputs:
  - "无"
triggers:
  - "进行具体子系统设计时"
downstream:
  - "docs/acceptance/001-im-e2e-rag-acceptance.md"
---

# IM 系统生产化架构设计

## 1. 背景

基于 SRD (docs/prd/001-im-system-srd.md)，MallChat 需要进一步生产化以提高消息可靠性与权限一致性。本设计限定了系统的技术演进边界和架构基调。

## 2. 目标

- 明确消息可靠性闭环的实现链路（数据库 -> 会话 -> WebSocket / RabbitMQ）。
- 约束技术栈，不得引入新的 ORM 或消息总线，基于现有组件进行改造。
- 代码风格与现有模型一致：Controller + Service + Mapper + Entity + Convert + DTO/VO。

## 3. 非目标

- 重构 `chat-*` 领域模型。
- 修改已有的 WebSocket 或 RabbitMQ 核心库逻辑。

## 4. 核心内容

- **消息可靠性**：消息发出后必须落库，形成数据库事实，然后再通过 RabbitMQ 发送并在 WebSocket 端实时投递。同时保证发送幂等。
- **状态一致性**：群组与好友关系的变更同步反映在会话系统及权限系统中。

## 5. 关联文档

### 5.1 输入文档
1. `docs/prd/001-im-system-srd.md`

### 5.2 下游文档
1. `docs/acceptance/001-im-e2e-rag-acceptance.md`

## 6. 变更记录

| 日期 | 作者 | 版本 | 变更说明 |
| --- | --- | --- | --- |
| 2026-06-06 | Gemini Agent | 0.1.0 | 初始化文档 |
