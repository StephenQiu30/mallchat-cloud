---
layer: PRD
doc_no: "001"
audience:
  - PM
  - Dev
  - QA
  - Ops
feature_area: im-system
purpose: "定义 MallChat IM 系统生产化增强的产品范围、P0/P1/P2 边界、TDD 与 RAG 验收要求。"
canonical_path: "docs/prd/001-im-system-srd.md"
status: draft
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "AGENTS.md"
  - "sql/mallchat.sql"
  - "mallchat-service/mallchat-chat-service"
  - "mallchat-common/mallchat-common-websocket"
  - "mallchat-common/mallchat-common-rabbitmq"
outputs:
  - "docs/design/D-001-im-production-architecture.md"
  - "docs/plans/001-im-production-task-orchestration-plan.md"
  - "docs/acceptance/001-im-e2e-rag-acceptance.md"
triggers:
  - "新增或调整 IM 好友、聊天、会话、朋友圈、通知、WebSocket 生产化能力"
  - "创建 IM 生产化 GitHub Issue 或 PR 前"
downstream:
  - "docs/design/D-001-im-production-architecture.md"
  - "docs/plans/001-im-production-task-orchestration-plan.md"
  - "docs/acceptance/001-im-e2e-rag-acceptance.md"
---

# IM 系统生产化 SRD

## 1. 背景

MallChat 当前已经具备 `chat-*` 领域模型、好友/群聊/消息/会话/朋友圈相关表、WebSocket 公共模块、RabbitMQ 消息模型和 focused tests。后续 IM 增强不应重新设计一套平行架构，而应基于现有后端继续生产化。

本 SRD 用于把 IM 系统需要实现的能力、边界、测试优先要求和 RAG 风险分级固化为长期需求来源。所有后续 feature Issue、Design、Acceptance 和 PR 都应从本文档派生。

## 2. 产品目标

1. P0 阶段形成可生产化的 IM 消息可靠性闭环：消息先形成数据库事实，再完成会话更新和实时投递。
2. P0 阶段保留好友、群聊、朋友圈的 MVP 产品闭环，但优先修正会影响消息可靠性和权限一致性的风险。
3. P1 阶段扩展富媒体、已读、撤回、群管理、通知聚合和朋友圈增强能力。
4. 每个 feature 必须遵循 TDD：先写失败测试，再写最小实现，再在测试保护下重构。
5. 每个可交付 feature 必须维护 RAG 状态：Red 阻塞、Amber 可延期但有风险、Green 可验收。
6. P0 核心链路必须有 E2E 或等价可执行验收脚本，不能只依赖编译通过。

### 2.1 BDD 场景

```gherkin
Given 用户 A 与用户 B 已建立好友关系
And 用户 A 与用户 B 存在私聊房间
When 用户 A 使用 clientMsgId 向用户 B 发送文本消息
Then 系统应只生成一条 chat_message 事实记录
And 重复发送同一 clientMsgId 不应重复落库或重复增加未读
And chat_session 应体现正确的 lastMessageId 与未读数
And WebSocket 或 MQ 投递失败不得回滚 chat_message 事实
```

## 3. 非目标

1. 不重写 `chat-*` 领域模型。
2. 不引入新的 ORM、消息总线或 WebSocket 事件格式替代现有模型。
3. 不把朋友圈 feed 复用 `chat_message` 作为事实表。
4. 不用通知中心替代好友申请、群申请、动态评论等业务事实表。
5. 不在本阶段实现端侧 UI、移动端适配或前端接口生成。
6. 不为一次性过程新增 `docs/superpowers/` 或临时计划文档。

## 4. 核心功能范围

### 4.1 P0 消息可靠性

1. `message-send-idempotency`：消息发送幂等、权限校验、参数校验、事实落库。
2. `message-delivery-reliability`：落库后 MQ/WebSocket 投递、重复投递识别、投递失败不破坏事实。
3. `session-consistency`：会话最后一条消息、未读数、重复消息和乱序消息保护。
4. `message-recovery-observability`：核心 IM 表备份恢复、日志指标、focused tests 和 CI 守护。

### 4.2 P0 好友与权限

1. `friend-apply-lifecycle`：好友申请、通过、拒绝、重复申请幂等。
2. `friend-message-permission`：非好友不可私聊，拉黑后权限收敛。
3. `friend-cache-degradation`：好友关系 Redis 缓存缺失时回源数据库。

### 4.3 P0/P1 群聊

1. `group-member-lifecycle`：建群、加入、退出、踢人、成员事实一致。
2. `group-message-permission`：非成员不可发消息，退出后权限即时收敛。
3. `group-session-consistency`：群消息会话、未读数和成员变化一致。

### 4.4 P0/P1 朋友圈

1. `moment-publish-feed`：动态发布、公开可见、好友可见。
2. `moment-like-comment-idempotency`：点赞幂等、评论事实独立。
3. `moment-permission-boundary`：非好友不可看好友可见动态，拉黑后权限收敛。

### 4.5 P1 通知与运维

1. `notification-fact-aggregation`：通知只做聚合，不替代业务事实表。
2. `im-production-ci-gates`：CI 接入稳定 focused tests 与 E2E smoke。
3. `im-runbook`：恢复、回滚、观测和故障排查说明。

## 5. 数据与权限边界

1. 数据库或明确持久化模型是事实源；Redis 只作为加速层。
2. 消息事实以 `chat_message` 为准，会话事实以 `chat_session` 为准。
3. 好友关系、黑名单、房间成员、朋友圈动态、点赞和评论不得只存在缓存中。
4. 私聊发送必须验证好友关系或既有业务允许关系。
5. 群聊发送必须验证房间成员身份。
6. 朋友圈好友可见必须验证好友关系和拉黑关系。
7. 权限失败不得产生业务事实记录。

## 6. 测试与 RAG 要求

1. Red：核心链路失败、无可执行测试、E2E 不存在或阻塞生产化。
2. Amber：核心链路可用，但失败补偿、观测、缓存退化或边界测试不足。
3. Green：单元、集成、E2E、仓库门禁和 focused tests 均有证据。
4. 每个 feature 必须记录红灯测试、绿灯验证、E2E 场景、RAG 状态和残余风险。

## 7. 代码风格一致性

1. Controller、Service、Mapper、Entity、Convert、DTO/VO 分层沿用当前项目风格。
2. API 契约放入 `mallchat-api-*`，业务实现放入 `mallchat-service/*`，公共能力放入 `mallchat-common/*`。
3. 返回结构继续使用 `BaseResponse` 和 `ResultUtils`。
4. 异常继续使用 `ThrowUtils`、`BusinessException` 和 `ErrorCode`。
5. SQL 事实源只维护 `sql/mallchat.sql`。
6. 实时事件优先复用 `ImWebSocketEvent`、`WebSocketMessage` 和现有 RabbitMQ 分发模型。
7. 新增代码继续沿用 `chat-*` / `Chat*` 命名，不新增平行 IM 命名体系。

## 8. 首版验收门禁

1. 本 SRD、对应 Design、Plan、Acceptance 文档均存在且互相引用。
2. P0 feature 均有 TDD 红灯测试规划和 E2E 验收场景。
3. RAG 状态表能区分 Red、Amber、Green。
4. 任务编排按 feature 组织，不按 PR 编号组织。
5. `bash scripts/validate-repository.sh` 通过。

## 9. 待确认问题

1. P0 E2E 是否使用纯后端集成测试、脚本驱动真实服务，还是两者组合。
2. WebSocket E2E 是否在首批使用真实 Netty 服务，还是先用可控替身验证投递契约。
3. 首批 Issue 是否创建到 GitHub，或先只保留 docs 编排。

## 10. 变更记录

| 日期 | 作者 | 版本 | 变更说明 |
| --- | --- | --- | --- |
| 2026-06-05 | StephenQiu30 | 0.1.0 | 初始化 IM 系统生产化 SRD |
