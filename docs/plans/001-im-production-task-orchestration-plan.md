---
layer: Plan
doc_no: "001"
audience:
  - PM
  - Dev
  - QA
  - Ops
feature_area: im-system
purpose: "按 Epic、Feature、Task、TDD/E2E 验收组织 MallChat IM 系统生产化增强任务。"
canonical_path: "docs/plans/001-im-production-task-orchestration-plan.md"
status: draft
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "docs/prd/001-im-system-srd.md"
  - "docs/design/D-001-im-production-architecture.md"
outputs:
  - "docs/acceptance/001-im-e2e-rag-acceptance.md"
triggers:
  - "创建 IM 生产化 Issue 前"
  - "拆分 IM feature、安排并行任务或定义验收边界时"
downstream:
  - "GitHub Issue"
  - "Pull Request"
  - "docs/acceptance/001-im-e2e-rag-acceptance.md"
---

# IM 生产化任务编排计划

## 1. 背景

本计划把 IM 系统增强拆成长期可执行的 feature 队列。任务按 feature 分类，不按 PR 编号分类。实际 PR 可以继续遵守仓库规则使用编号化分支和标题，但需求、验收和计划均以 feature 为主。

## 2. 总 Epic

`IM 系统生产化增强`

目标：在现有 MallChat 后端上完成好友、聊天、会话、实时投递、朋友圈、通知和运维能力的生产化闭环，并保证每个 feature 都具备 TDD、E2E 和 RAG 验收证据。

## 3. 阶段 0：规格与测试底座

### 3.1 `im-system-srd`

1. 输出：`docs/prd/001-im-system-srd.md`。
2. 范围：P0/P1/P2 能力、非目标、数据权限边界、RAG 定义。
3. 验收：SRD 可作为 Issue 和 PR 的需求来源。

### 3.2 `im-e2e-test-harness`

1. 输出：E2E 测试入口或可执行验收脚本。
2. 范围：用户 A/B、好友、私聊、消息、会话、MQ/WebSocket、朋友圈。
3. 红灯：E2E 入口不存在或主链路失败。
4. 绿灯：E2E smoke 可稳定执行并输出结果。

### 3.3 `im-rag-acceptance-dashboard`

1. 输出：`docs/acceptance/001-im-e2e-rag-acceptance.md`。
2. 范围：feature RAG 状态、红绿测试、E2E 场景、残余风险。
3. 验收：每个 P0 feature 都能落到 Red、Amber 或 Green。

## 4. 阶段 1：消息可靠性 P0

### 4.1 `message-send-idempotency`

1. 做：`clientMsgId + fromUserId + roomId` 幂等、发送权限、参数校验、事实落库。
2. 不做：MQ 重投、前端消息状态 UI。
3. TDD：重复发送、非成员发送、参数错误、重复 `clientMsgId` 并发。
4. E2E：A 给 B 发送同一 `clientMsgId` 两次，只生成一条事实消息。
5. 风格门禁：复用现有 `ChatMessageService`、`ChatMessageMapper`、`ChatMessageConvert` 和 DTO/VO 风格。

### 4.2 `message-delivery-reliability`

1. 做：落库后 MQ/WebSocket 投递，失败不回滚事实，重复投递可识别。
2. 不做：替换 RabbitMQ 或 WebSocket 协议。
3. TDD：MQ 发送失败替身、WebSocket 离线、重复事件去重。
4. E2E：模拟投递失败后，消息历史仍可查。
5. 风格门禁：复用 `ImWebSocketEvent`、`WebSocketMessage` 和现有 RabbitMQ 模型。

### 4.3 `session-consistency`

1. 做：`chat_session` lastMessage、未读数、重复/乱序消息保护。
2. 不做：复杂多端同步策略。
3. TDD：重复消息不重复加未读，旧消息不覆盖新消息。
4. E2E：A 发消息后 B 未读 +1，重复发送不重复 +1。
5. 风格门禁：复用 `ChatSessionService` 和现有 Convert/VO。

### 4.4 `message-recovery-observability`

1. 做：核心表备份恢复、消息链路日志指标、focused tests 和 CI 守护。
2. 不做：完整灾备平台。
3. TDD：恢复脚本或守护测试先失败后通过。
4. E2E：恢复后核心关系无孤儿数据，消息和会话可对齐。
5. 风格门禁：只增强长期脚本和 CI，不引入过程性报告。

## 5. 阶段 2：好友与权限 P0

### 5.1 `friend-apply-lifecycle`

1. 做：申请、通过、拒绝、重复申请幂等。
2. TDD：重复申请、目标不存在、本人申请本人、已是好友。
3. E2E：A 申请 B，B 通过，双方好友列表一致。

### 5.2 `friend-message-permission`

1. 做：非好友不可私聊，拉黑后权限收敛。
2. TDD：非好友发消息失败、拉黑后发送失败。
3. E2E：拉黑后发送失败，历史事实消息不被删除。

### 5.3 `friend-cache-degradation`

1. 做：Redis 缺失时回源数据库。
2. TDD：缓存 miss 和缓存 hit 均覆盖。
3. E2E：清缓存后权限判断仍正确。

## 6. 阶段 3：群聊 P0/P1

1. `group-member-lifecycle`：建群、加入、退出、踢人、成员事实一致。
2. `group-message-permission`：非成员不可发，退出后不可发。
3. `group-session-consistency`：群消息会话、未读和成员变化一致。

## 7. 阶段 4：朋友圈 P0/P1

1. `moment-publish-feed`：动态发布、公开可见、好友可见。
2. `moment-like-comment-idempotency`：点赞幂等、评论事实独立。
3. `moment-permission-boundary`：非好友不可看好友可见动态，拉黑后权限收敛。

## 8. 阶段 5：通知与运维

1. `notification-fact-aggregation`：通知只聚合，不替代业务事实表。
2. `im-production-ci-gates`：CI 接入 focused tests 和 E2E smoke。
3. `im-runbook`：故障排查、恢复、回滚和观测说明。

## 9. 并行与所有权

1. 每轮最多并行 2-3 个 feature。
2. 多个执行者不得同时修改同一个核心 Service 实现。
3. 消息可靠性 Green 前，不进入朋友圈生产化实现。
4. 安全、权限、事务、消息事实和数据恢复类 feature 必须安排只读 reviewer。
5. 主代理负责收口：合并冲突、更新验收文档、跑回归、提交和 PR。

## 10. 通用验收模板

每个 feature Issue 必须包含：

1. Parent Epic。
2. 文件所有权。
3. TDD 红灯测试。
4. Green 验证命令。
5. E2E 验收场景。
6. RAG 初始状态和目标状态。
7. 代码风格一致性门禁。
8. 残余风险和延期项。

## 11. 首批执行顺序

1. `im-e2e-test-harness`
2. `im-rag-acceptance-dashboard`
3. `message-send-idempotency`
4. `message-delivery-reliability`
5. `session-consistency`
6. `message-recovery-observability`

## 12. 验收门禁

1. 任务按 feature 分类，不按 PR 编号分类。
2. 每个 P0 feature 都有 TDD、E2E、RAG 和代码风格一致性验收项。
3. 任务顺序优先消息可靠性，再好友/群聊/朋友圈。
4. `bash scripts/validate-repository.sh` 通过。

## 13. 变更记录

| 日期 | 作者 | 版本 | 变更说明 |
| --- | --- | --- | --- |
| 2026-06-05 | StephenQiu30 | 0.1.0 | 初始化 IM 生产化任务编排计划 |
