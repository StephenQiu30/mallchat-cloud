---
layer: Design
doc_no: "D-001"
audience:
  - Dev
  - QA
  - Ops
feature_area: im-system
purpose: "定义 MallChat IM 生产化增强的后端架构、数据流、失败补偿、E2E 测试和代码风格一致性设计。"
canonical_path: "docs/design/D-001-im-production-architecture.md"
status: draft
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "docs/prd/001-im-system-srd.md"
  - "AGENTS.md"
  - "sql/mallchat.sql"
  - "mallchat-service/mallchat-chat-service"
  - "mallchat-common/mallchat-common-websocket"
  - "mallchat-common/mallchat-common-rabbitmq"
outputs:
  - "docs/plans/001-im-production-task-orchestration-plan.md"
  - "docs/acceptance/001-im-e2e-rag-acceptance.md"
triggers:
  - "实现 IM 生产化 feature 前"
  - "调整消息、会话、好友、群聊、朋友圈或实时投递设计时"
downstream:
  - "docs/plans/001-im-production-task-orchestration-plan.md"
  - "docs/acceptance/001-im-e2e-rag-acceptance.md"
---

# IM 生产化架构设计

## 1. 背景

本设计基于现有 MallChat 后端继续增强，不引入平行 IM 架构。核心约束是：数据库事实先成立，缓存和实时投递只做加速或分发；测试优先定义行为，再做最小实现。

## 2. 架构边界

1. `mallchat-api-*`：跨服务 DTO、VO、Feign 契约。
2. `mallchat-service/mallchat-chat-service`：好友、群聊、消息、会话、朋友圈业务实现。
3. `mallchat-common/mallchat-common-websocket`：WebSocket 连接管理和帧处理。
4. `mallchat-common/mallchat-common-rabbitmq`：RabbitMQ 消息模型、发送、消费去重和分发。
5. `sql/mallchat.sql`：数据库结构唯一事实源。
6. `scripts/*im*`：备份、恢复、仓库验证等可执行运维入口。

## 3. 核心数据流

### 3.1 消息发送

1. Controller 接收业务 Request DTO。
2. Service 校验登录用户、房间存在、成员权限、消息类型和内容。
3. Service 使用 `clientMsgId + fromUserId + roomId` 做幂等判断。
4. 不存在重复消息时写入 `chat_message`。
5. 在同一业务链路内更新发送方和接收方 `chat_session`。
6. 消息事实和会话更新成功后，触发 RabbitMQ/WebSocket 投递。
7. 投递失败只记录失败和指标，不回滚已成立的数据库事实。

### 3.2 实时投递

1. 投递事件优先复用 `ImWebSocketEvent`、`WebSocketMessage` 和 RabbitMQ 现有模型。
2. 在线用户通过 WebSocket 接收事件。
3. 离线用户不依赖实时事件保存事实，重新上线后通过消息历史和会话接口恢复状态。
4. 重复投递必须带有可识别消息 ID 或业务幂等键，客户端或消费端可去重。

### 3.3 会话一致性

1. `chat_session.last_message_id` 不得被旧消息覆盖。
2. 重复消息不得重复增加未读数。
3. 成员权限变化后，新消息发送权限必须实时收敛。
4. 会话更新失败应作为 Red 风险处理，不能只记录日志后静默通过。

### 3.4 朋友圈

1. 动态事实使用 `chat_moment`，媒体使用 `chat_moment_media`，点赞和评论使用独立事实表。
2. 朋友圈 feed 不复用 `chat_message`。
3. 好友可见、公开可见和拉黑边界必须在查询侧显式校验。

## 4. 失败补偿边界

| 场景 | 事实数据 | 补偿策略 | RAG 默认状态 |
| --- | --- | --- | --- |
| 重复发送同一 `clientMsgId` | 返回已有消息或拒绝重复写入 | 幂等查询保护 | Red 直到有测试 |
| MQ 发布失败 | `chat_message` 保留 | 记录日志和指标，后续补偿重投 | Amber |
| WebSocket 用户离线 | `chat_message` 保留 | 依赖历史拉取和会话未读 | Amber |
| 会话更新失败 | 不应静默通过 | 事务或可恢复补偿设计 | Red |
| Redis 缓存缺失 | 数据库为准 | 回源数据库并回填缓存 | Amber |
| 非成员发送 | 不写事实 | 返回权限错误 | Red 直到有测试 |

## 5. TDD 与 E2E 设计

### 5.1 TDD 层级

1. Unit：Convert、Helper、幂等 key、参数校验。
2. Service：发送、权限、会话、朋友圈互动、失败补偿。
3. Component：RabbitMQ sender/consumer、WebSocket manager、缓存退化。
4. Contract：Controller/Feign/DTO/VO 一致性。
5. E2E：A/B 用户真实业务链路或可执行脚本。

### 5.2 E2E 主线

1. 准备用户 A 和用户 B。
2. A 发起好友申请，B 通过。
3. 创建或获取 A/B 私聊房间。
4. A 使用 `clientMsgId` 发送文本消息。
5. 验证 `chat_message` 只存在一条事实。
6. 重复发送同一 `clientMsgId`，验证不重复落库。
7. 验证 B 的会话未读和 last message。
8. 模拟 MQ 或 WebSocket 失败，验证事实消息不回滚。
9. B 离线后重新拉取历史，验证消息不丢。

## 6. 代码风格一致性门禁

1. 不新增平行数据访问层。
2. 不把跨服务 DTO/VO 放入 service 模块。
3. 不新增一次性 JSON/MQ/WebSocket 包装格式。
4. 不绕过现有 `Chat*Service`、Mapper、Entity、Convert 风格。
5. 文件膨胀时只做服务于当前 feature 的拆分。
6. 新增守护规则必须能说明来源于 SRD、Design 或 Acceptance。

## 7. 关联文档

### 7.1 输入文档

1. `docs/prd/001-im-system-srd.md`

### 7.2 输出文档

1. `docs/plans/001-im-production-task-orchestration-plan.md`
2. `docs/acceptance/001-im-e2e-rag-acceptance.md`

## 8. 验收门禁

1. 设计覆盖消息、会话、好友、群聊、朋友圈、通知和运维边界。
2. 每个 P0 feature 有明确事实源、权限边界、幂等边界、并发边界和失败补偿边界。
3. E2E 测试主线可映射到 Acceptance 文档。
4. 代码风格一致性门禁可映射到 PR checklist。

## 9. 风险与边界

1. 真实 WebSocket E2E 成本可能较高，首批可使用可控替身验证投递契约。
2. MQ 失败补偿是否需要持久化重投表，需在 `message-delivery-reliability` feature 中确认。
3. 会话更新是否与消息落库同事务，需要基于当前实现和测试决定最小改动。

## 10. 待确认问题

1. E2E 是否优先用 JUnit 集成测试，还是 shell 脚本驱动服务。
2. MQ 失败后是否需要首批实现自动重投，还是先记录 Amber 风险。

## 11. 变更记录

| 日期 | 作者 | 版本 | 变更说明 |
| --- | --- | --- | --- |
| 2026-06-05 | StephenQiu30 | 0.1.0 | 初始化 IM 生产化架构设计 |
