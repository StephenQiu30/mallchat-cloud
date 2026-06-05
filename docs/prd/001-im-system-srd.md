---
layer: PRD
doc_no: "001"
audience:
  - PM
  - Dev
  - QA
feature_area: im-core
purpose: "定义 IM 系统生产化增强的需求边界与产品目标"
canonical_path: "docs/prd/001-im-system-srd.md"
status: draft
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "无"
outputs:
  - "docs/design/D-001-im-production-architecture.md"
  - "docs/plans/001-im-production-task-orchestration-plan.md"
triggers:
  - "IM 核心功能需要范围确认时"
downstream:
  - "docs/design/D-001-im-production-architecture.md"
  - "docs/plans/001-im-production-task-orchestration-plan.md"
  - "docs/acceptance/001-im-e2e-rag-acceptance.md"
---

# IM 系统生产化增强（SRD 总 Epic）

## 1. 背景

MallChat 当前已经具备 `chat-*` 领域模型、好友/群聊/消息/会话/朋友圈相关表、WebSocket 公共模块、RabbitMQ 消息模型和 focused tests。但是现阶段仍存在消息可靠性不足、权限不一致等风险，需要基于现有后端进行生产化增强。

## 2. 目标

1. P0 阶段形成 IM 消息可靠性闭环：数据库事实先成立，再完成会话更新和实时投递。
2. P0 阶段保留好友、群聊、朋友圈 MVP 闭环，优先修正影响消息可靠性和权限一致性的风险。
3. P1 阶段扩展富媒体、已读、撤回、群管理、通知聚合和朋友圈增强能力。
4. 每个 feature 必须具备 TDD、E2E 和 RAG 验收证据。

## 3. 非目标

- 不重写 `chat-*` 领域模型。
- 不引入新的 ORM、消息总线或 WebSocket 事件格式替代现有模型。
- 不把朋友圈 feed 复用 `chat_message` 作为事实表。
- 不用通知中心替代好友申请、群申请、动态评论等业务事实表。
- 不实现端侧 UI 或前端接口生成。

## 4. 核心内容

此 SRD 包含以下子功能边界：
1. 阶段 0：规格与测试底座
2. 阶段 1：消息可靠性 P0
3. 阶段 2：好友与权限 P0
4. 阶段 3：群聊 P0/P1
5. 阶段 4：朋友圈 P0/P1
6. 阶段 5：通知与运维

## 5. 关联文档

### 5.1 输出文档
1. `docs/design/D-001-im-production-architecture.md`
2. `docs/plans/001-im-production-task-orchestration-plan.md`

### 5.2 下游文档
1. `docs/acceptance/001-im-e2e-rag-acceptance.md`

## 6. 验收门禁

- 业务需求拆解不偏离非目标。
- 架构设计遵循此 PRD 约束。

## 7. 变更记录

| 日期 | 作者 | 版本 | 变更说明 |
| --- | --- | --- | --- |
| 2026-06-06 | Gemini Agent | 0.1.0 | 初始化文档 |
