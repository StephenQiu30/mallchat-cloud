---
layer: Design
doc_no: "SP-001"
audience:
  - PM
  - Dev
  - QA
  - Ops
feature_area: im-production-readiness
purpose: "定义 MallChat 后端优先的生产可用 IM 能力补齐路线、Issue 拆解规则和多子智能体消费规范。"
canonical_path: "docs/superpowers/specs/2026-05-20-im-production-readiness-issue-design.md"
status: review
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "AGENTS.md"
  - "docs/README.md"
  - "docs/plans/PL-003-im-backend-long-task-consolidation-plan.md"
  - "docs/acceptance/A-015-im-backend-long-task-acceptance-summary.md"
  - "openspec/specs/chat-friend/spec.md"
  - "openspec/specs/chat-message/spec.md"
  - "openspec/specs/chat-realtime-delivery/spec.md"
  - "openspec/specs/chat-room-access/spec.md"
  - "openspec/specs/chat-session/spec.md"
  - "openspec/specs/moments-feed/spec.md"
outputs:
  - "生产可用 IM 后端能力缺口清单"
  - "GitHub Epic 与子 Issue 拆解模型"
  - "后续 AGENTS.md 工作规范补充项"
  - "多子智能体并行消费 Issue 的边界规则"
triggers:
  - "准备把 MallChat 从 MVP 推向正式生产可用 IM 系统"
  - "需要创建或消费 IM 后端生产化 GitHub Issue"
  - "需要判断某个 IM 能力是否应先后端落地、后多端对齐"
downstream:
  - "docs/plans/PL-004-im-production-readiness-issue-plan.md"
  - "docs/design/D-003-im-production-readiness-architecture.md"
  - "docs/acceptance/A-016-im-production-readiness-issue-generation-acceptance.md"
  - "AGENTS.md"
---

# IM 生产可用 Issue 路线设计

## 1. 背景

MallChat 当前后端已经具备 IM MVP 的关键基础：用户、好友、会话、群聊、消息、动态、通知、文件、WebSocket、RabbitMQ 与 Redis 等能力已经形成可运行闭环。前一轮后端长任务已经完成并归档了好友通知、动态可见性、会话未读、实时投递等基础增强。

下一阶段目标不是继续堆叠类似 QQ 的全部功能，而是把项目推进到“可以正式上线运行”的后端优先状态。正式生产可用要求系统具备安全边界、可靠消息、可观测性、数据恢复、基础治理和可验收测试。多端体验随后按同一接口契约逐步对齐，不在本轮把 Taro、UniApp、Flutter 和 Admin 混成不可验收的大改动。

本文件是 Superpowers brainstorming 阶段的设计产物。它只定义路线、拆解和规范，不直接修改业务代码、不创建 GitHub Issue、不进入 OpenSpec 实施。

## 2. 已确认决策

1. 目标等级：正式生产可用，不停留在 Demo/MVP，也不追求完整 QQ Clone。
2. 执行顺序：先后端，后多端。
3. Issue 拆法：采用混合拆法，即 Epic Issue 承载业务域和生产目标，子 Issue 承载可执行的 TDD 任务。
4. 文档策略：完整路线写入 `docs/`，一次性过程文件不进入长期文档。
5. 代码策略：不做过度工程化设计，优先复用现有微服务模式、代码风格和技术栈。
6. 测试策略：遵循 TDD，新增或调整核心逻辑前先补失败测试，再实现，再回归。
7. 并行策略：允许多个子智能体消费 Issue，但必须按文件所有权和 OpenSpec 边界拆开，主智能体负责最终合并、回归、归档和提交。

## 3. 目标

### 3.1 产品目标

1. 支持正式上线 IM 的后端最小安全闭环：鉴权、限流、审计、文件安全和权限校验可验收。
2. 支持正式上线 IM 的消息可靠闭环：消息写入、投递、断线补偿、幂等和失败观测可验收。
3. 支持正式上线 IM 的运维闭环：健康检查、业务指标、日志定位、Runbook 和回滚边界可验收。
4. 支持正式上线 IM 的数据恢复闭环：核心数据备份、Redis 失效恢复、文件边界和恢复演练可验收。
5. 支持后续 QQ-like 体验扩展：拉黑、举报、好友备注、群治理、消息搜索、通知偏好等能力有清晰候选池。

### 3.2 工程目标

1. 所有可执行子 Issue 都能落到后端现有模块、OpenSpec change 和测试命令。
2. Issue 描述必须包含 Parent Epic、代码参考、OpenSpec 建议、TDD 验收、生产验收和完成标准。
3. 不默认新增中间件、不重写现有协议、不引入与现有代码平行的消息或权限体系。
4. 代码变更保持现有风格：Controller -> Service -> Mapper -> Entity -> Convert -> DTO/VO。
5. 每轮并行消费后必须回填文档和 OpenSpec 验收状态，避免只改代码不沉淀事实。

## 4. 非目标

1. 本文件不实现任何业务代码。
2. 本文件不创建 GitHub Issue，只定义后续创建规则和首批范围。
3. 本文件不要求一次性完成所有 QQ 功能。
4. 本文件不把管理后台、小程序端、Flutter 端和后端合并成同一批实现任务。
5. 本文件不默认引入 Kafka、Elasticsearch、独立审计中心、独立风控系统或分布式追踪平台。
6. 本文件不把端到端加密、复杂表情商城、空间装扮、音视频通话作为 P0。

## 5. 方案权衡

### 5.1 方案 A：后端生产化优先

先补齐安全、可靠性、可观测、数据恢复和基础治理，再让多端按稳定接口对齐。

优点：风险可控，符合当前后端已具备 MVP 基础的状态；测试和 OpenSpec 可以先把接口契约锁住；多端后续不需要追随频繁变化的后端。

缺点：短期看不到完整多端体验；部分前端空态和交互需要等后端稳定后再进入。

### 5.2 方案 B：全端并行推进

后端、Taro、UniApp、Flutter、Admin 同时拆 Issue 并开发。

优点：产品感知推进快，多端缺口会更早暴露。

缺点：接口尚未完全生产化时，多端容易产生返工；并行文件所有权复杂，验收成本高。

### 5.3 方案 C：QQ 功能全集路线

按 QQ 功能全集一次性建立大范围路线，包括音视频、表情、空间、个性装扮、推荐和复杂内容审核。

优点：产品愿景完整。

缺点：明显超过当前阶段，容易过度设计，并稀释上线最关键的安全、可靠和恢复能力。

### 5.4 推荐结论

采用方案 A，并保留方案 B 的后续多端队列。P0 只处理生产可用必须项；P1 处理上线后第一阶段体验增强；P2 保留高级能力候选池。这样既能避免过度设计，又能让后续 QQ-like 功能有稳定扩展路径。

## 6. 当前能力与缺口

### 6.1 已具备基础

1. 好友关系：好友申请、同意、拒绝、好友列表和通知链路已经具备基础。
2. 会话消息：消息发送、会话列表、未读数、已读、撤回等已经形成服务层和接口基础。
3. 群聊权限：群成员、群访问、会话权限已有 OpenSpec 和部分服务实现基础。
4. 动态：动态发布、媒体、点赞、评论、可见性和互动通知已经具备 MVP。
5. 实时链路：WebSocket、在线状态、RabbitMQ 推送和部分投递降级已有实现。
6. 文件：文件服务和上传入口已存在，可作为安全边界增强对象。

### 6.2 生产化缺口

1. 安全边界仍需补强：WebSocket 握手鉴权、Origin 校验、连接限流、核心接口限流和审计日志需要统一验收。
2. 消息可靠性仍需补强：RabbitMQ 发布失败、推送失败、重复投递、断线补偿和补偿验收需要形成闭环。
3. 可观测性仍需补强：IM 关键业务指标、健康检查、日志定位和 Runbook 需要沉淀。
4. 数据恢复仍需补强：核心表备份恢复、Redis 失效恢复、文件安全和恢复演练需要可执行。
5. 用户治理仍需补强：拉黑、举报、好友备注、轻量分组等上线后治理能力尚未纳入稳定队列。
6. 群治理仍需补强：群管理员、入群审批、通知偏好和消息搜索尚未进入可执行计划。

## 7. Issue 生成模型

### 7.1 总体规则

1. docs 中保留完整路线：6 个 Epic 和 21 个可执行子 Issue。
2. GitHub 首批只创建 P0：4 个 Epic 和 14 个子 Issue。
3. P1/P2 暂存为候选池：等 P0 完成 70%-80%，并且关键回归稳定后再进入 GitHub。
4. 每个子 Issue 必须能被一个 agent 在明确文件所有权内完成，不允许一个 Issue 同时横跨后端、Taro、Flutter 和 Admin。
5. 每个子 Issue 必须关联一个 Parent Epic，并标注建议 OpenSpec change id。
6. 一个 OpenSpec change 可以覆盖多个高度相关子 Issue，但同一个 spec 文件的修改必须串行合并。
7. Issue 状态不能替代 OpenSpec 状态；完成代码后仍需更新 OpenSpec tasks 和归档。

### 7.2 标题与标签

Epic 标题格式：

```text
[EPIC][P0] 生产安全与访问控制
```

子 Issue 标题格式：

```text
[P0][backend][security] WebSocket 握手鉴权与 Origin 校验
```

推荐标签：

1. `type:epic`
2. `type:task`
3. `priority:p0`
4. `priority:p1`
5. `area:backend`
6. `area:security`
7. `area:message`
8. `area:ops`
9. `area:data`
10. `area:room`
11. `needs:openspec`
12. `needs:tdd`
13. `agent:ready`
14. `agent:blocked`

### 7.3 子 Issue 模板

每个可执行子 Issue 必须包含以下字段：

1. 背景：说明为什么生产可用需要该能力。
2. 目标：列出本 Issue 必须完成的可验证结果。
3. 范围：列出允许修改的模块或文件域。
4. 非目标：明确不做的扩展能力。
5. Parent Epic：链接所属 Epic。
6. 代码参考：列出现有 Controller、Service、Mapper、Entity、测试或配置入口。
7. OpenSpec 建议：写出建议 change id 和涉及 spec。
8. TDD 验收：先写哪些失败测试，再实现哪些行为。
9. 生产验收：接口、日志、指标、回归或恢复演练如何证明可用。
10. 代码风格一致性：说明应复用的现有异常、枚举、工具和命名方式。
11. 依赖关系：说明前置 Issue 或必须串行的 spec。
12. 完成标准：代码、测试、OpenSpec、文档和提交状态。

## 8. P0 首批 GitHub Issue

### 8.1 Epic 1：[EPIC][P0] 生产安全与访问控制

目标：让正式用户入口、WebSocket 入口、核心 IM 接口和敏感操作有明确安全边界。

#### 8.1.1 [P0][backend][security] WebSocket 握手鉴权与 Origin 校验

1. 建议 OpenSpec：`harden-websocket-handshake-security`。
2. 代码参考：WebSocket 连接入口、登录态解析、在线状态和推送链路。
3. TDD 验收：未登录、非法 token、非法 Origin、合法登录四类握手测试先失败后通过。
4. 生产验收：非法连接被拒绝，合法连接保持现有实时消息能力。
5. 非目标：不引入独立认证中心，不改造客户端登录协议。

#### 8.1.2 [P0][backend][security] WebSocket 连接频率限制与异常断开审计

1. 建议 OpenSpec：`harden-websocket-runtime-guard`。
2. 代码参考：WebSocket session 管理、在线状态、日志工具。
3. TDD 验收：同用户连接上限、短时间重复连接、异常断开记录。
4. 生产验收：滥用连接可被拒绝或降级，并能在日志中定位。
5. 非目标：不建设完整风控系统。

#### 8.1.3 [P0][backend][security] IM 核心接口限流策略

1. 建议 OpenSpec：`add-im-api-rate-limit`。
2. 代码参考：消息发送、好友申请、动态发布、文件上传接口。
3. TDD 验收：超过阈值返回明确错误码，正常请求不受影响。
4. 生产验收：刷消息、刷好友申请、刷动态的请求可被限制。
5. 非目标：不引入复杂分布式限流平台，优先复用现有 Redis 或网关能力。

#### 8.1.4 [P0][backend][security] 敏感操作审计日志

1. 建议 OpenSpec：`add-im-audit-log-mvp`。
2. 代码参考：好友删除、群成员变更、消息撤回、文件上传、登录态相关入口。
3. TDD 验收：关键操作成功和失败时写入审计事实。
4. 生产验收：可以按 userId、bizId、operation 定位敏感操作。
5. 非目标：不建设独立审计后台。

### 8.2 Epic 2：[EPIC][P0] 消息可靠性与可恢复

目标：让消息事实、推送事实和补偿事实可以被测试、观测和恢复。

#### 8.2.1 [P0][backend][mq] RabbitMQ 发布确认与失败观测 MVP

1. 建议 OpenSpec：`add-rabbitmq-publish-observability`。
2. 代码参考：现有 RabbitMQ 发送配置、消息推送事件、通知事件。
3. TDD 验收：发布成功、发布失败、返回异常时有可观测记录。
4. 生产验收：失败不吞没，日志和指标能定位消息类型与 bizId。
5. 非目标：不默认引入 outbox 表，除非测试证明现有模型无法满足。

#### 8.2.2 [P0][backend][message] 推送失败指标化

1. 建议 OpenSpec：`add-im-push-failure-metrics`。
2. 代码参考：WebSocket 推送、通知推送、好友申请推送、动态互动推送。
3. TDD 验收：推送成功、用户离线、推送异常分别记录指标或日志。
4. 生产验收：能统计按消息类型划分的推送失败数量。
5. 非目标：不改变业务写入成功语义。

#### 8.2.3 [P0][backend][message] 断线重连补偿真实链路验收

1. 建议 OpenSpec：`verify-reconnect-message-recovery`。
2. 代码参考：会话列表、未读数、消息拉取、在线状态和 WebSocket 重连。
3. TDD 验收：离线期间收到消息，重连后能通过拉取和未读恢复事实。
4. 生产验收：断线不丢业务事实，实时失败时接口补偿可用。
5. 非目标：不要求所有离线消息都通过 WebSocket 重放。

#### 8.2.4 [P0][backend][message] 消息幂等与重复投递验收加固

1. 建议 OpenSpec：`harden-message-idempotency`。
2. 代码参考：消息发送、消息表唯一约束、会话未读更新。
3. TDD 验收：重复 clientMsgId 或重复投递不会产生重复业务事实。
4. 生产验收：消息、会话、未读数在重复请求下保持一致。
5. 非目标：不设计跨数据中心全局一致性方案。

### 8.3 Epic 3：[EPIC][P0] 可观测性与运维门禁

目标：让后端服务在生产环境中可以启动前检查、运行中观察、故障时定位。

#### 8.3.1 [P0][backend][ops] 后端服务健康检查与启动门禁

1. 建议 OpenSpec：`add-backend-health-gates`。
2. 代码参考：Spring Boot Actuator、网关、依赖服务配置。
3. TDD 验收：数据库、Redis、RabbitMQ、核心服务健康状态可区分。
4. 生产验收：缺失关键依赖时健康检查返回可诊断状态。
5. 非目标：不新增复杂部署平台。

#### 8.3.2 [P0][backend][observability] IM 关键业务指标埋点

1. 建议 OpenSpec：`add-im-business-metrics`。
2. 代码参考：消息发送、消息投递、好友申请、群消息、动态互动。
3. TDD 验收：关键行为能产生计数、耗时或失败指标。
4. 生产验收：能够回答消息发送量、推送失败量、好友申请量、动态互动量。
5. 非目标：不强绑定某一个商业监控平台，优先兼容 Micrometer/OpenTelemetry 思路。

#### 8.3.3 [P0][backend][ops] 生产上线 Runbook

1. 建议 OpenSpec：`document-im-production-runbook`。
2. 代码参考：启动脚本、配置文件、健康检查、已有 docs/operations。
3. TDD 验收：本 Issue 以文档验收为主，不新增业务测试。
4. 生产验收：包含启动、检查、常见故障、回滚、数据恢复和告警处理步骤。
5. 非目标：不替代真实部署平台文档。

### 8.4 Epic 4：[EPIC][P0] 数据安全与备份恢复

目标：让核心 IM 数据、缓存和文件在故障后可以恢复业务事实。

#### 8.4.1 [P0][backend][data] 核心 IM 表备份恢复验收

1. 建议 OpenSpec：`verify-im-core-data-recovery`。
2. 代码参考：`chat_message`、`chat_session`、`chat_friend`、`chat_group`、`chat_moment` 相关表。
3. TDD 验收：优先使用脚本或集成测试验证备份导出和恢复后的核心查询。
4. 生产验收：恢复后消息、会话、好友、群成员、动态事实可查询。
5. 非目标：不建设完整灾备系统。

#### 8.4.2 [P0][backend][cache] Redis 缓存失效恢复验收

1. 建议 OpenSpec：`verify-redis-cache-recovery`。
2. 代码参考：登录态、会话缓存、在线状态、限流缓存。
3. TDD 验收：清空相关缓存后，核心接口能从数据库或重新登录路径恢复。
4. 生产验收：缓存丢失不会造成永久业务事实丢失。
5. 非目标：不要求所有缓存自动预热。

#### 8.4.3 [P0][backend][file] 文件上传安全边界

1. 建议 OpenSpec：`harden-file-upload-boundary`。
2. 代码参考：文件上传接口、消息图片/文件消息、动态媒体。
3. TDD 验收：文件大小、类型、空文件、异常文件名和越权访问测试。
4. 生产验收：非法文件被拒绝，合法图片/文件消息保持可用。
5. 非目标：不建设完整内容审核系统。

## 9. P1 候选池

P1 不在首批 GitHub Issue 中创建。只有当 P0 完成 70%-80%，并且消息、安全、恢复相关回归稳定后，才从候选池进入 GitHub。

### 9.1 Epic 5：[EPIC][P1] 用户安全与关系治理

#### 9.1.1 [P1][backend][friend] 拉黑与解除拉黑

1. 生产价值：降低骚扰风险，形成用户安全底线。
2. 代码风格：建议沿用 `chat-friend` 和 `ChatFriend*` 命名，不新建平行关系系统。
3. 验收重点：被拉黑用户不能发消息、不能发好友申请、不能查看受限动态。

#### 9.1.2 [P1][backend][report] 举报用户/消息/动态 MVP

1. 生产价值：为正式上线后的内容治理留入口。
2. 代码风格：可新增 `ChatReport` 类实体，但保持现有 Mapper/Service/VO 分层。
3. 验收重点：举报对象合法性、重复举报策略、审计日志。

#### 9.1.3 [P1][backend][friend] 好友备注与轻量分组

1. 生产价值：提升基础 QQ-like 联系人体验。
2. 代码风格：优先扩展好友关系模型或配套表，不引入复杂联系人系统。
3. 验收重点：备注修改、分组查询、默认分组、权限边界。

### 9.2 Epic 6：[EPIC][P1] 群聊治理与消息体验

#### 9.2.1 [P1][backend][room] 群管理员任免

1. 生产价值：正式群聊需要群主之外的轻量治理能力。
2. 代码风格：沿用 `chat-room-access` 和现有群成员角色模型。
3. 验收重点：群主授权、管理员权限边界、越权拒绝。

#### 9.2.2 [P1][backend][room] 入群审批 MVP

1. 生产价值：群聊正式开放后需要控制入群质量。
2. 代码风格：参考好友申请的申请/审核模式，避免新建完全不同的审批模型。
3. 验收重点：申请、同意、拒绝、重复申请、通知。

#### 9.2.3 [P1][backend][message] 消息搜索 MVP

1. 生产价值：提升正式 IM 的可用性。
2. 代码风格：首版优先数据库条件查询和索引评估，不默认引入 Elasticsearch。
3. 验收重点：用户只能搜索有权限的私聊或群聊消息。

#### 9.2.4 [P1][backend][notification] 通知偏好与群免打扰

1. 生产价值：降低正式使用中的噪声。
2. 代码风格：优先复用现有 notification 和 session 设置，不新增平行通知中心。
3. 验收重点：免打扰不影响消息事实，只影响推送和未读展示策略。

## 10. P2 候选池

P2 只保留产品方向，不进入近期执行。

1. 语音消息。
2. 视频消息。
3. 表情和贴纸。
4. 消息转发和合并转发。
5. 动态公开广场。
6. 动态推荐流。
7. 内容审核 AI。
8. 复杂管理后台审计检索。
9. 多端完整 E2E 自动化矩阵。

## 11. 代码风格一致性规则

后续执行 Issue 时必须遵守以下规则，并写入项目规范文件。

1. 后端命名继续使用 `chat-*` / `Chat*`，例如 `chat-blacklist`、`ChatReport`、`ChatAuditLog`。
2. 接口契约优先放在 `mallchat-api-*`，实现放在 `mallchat-service/*`，通用能力放在 `mallchat-common/*`。
3. Controller、Service、Mapper、Entity、Convert、DTO、VO 分层保持现有风格。
4. 异常处理复用 `ThrowUtils`、`BusinessException`、`ErrorCode`，不新增平行异常体系。
5. SQL 风格参考 `sql/mallchat.sql`，保持逻辑删除、时间字段、唯一键和索引命名一致。
6. 实时事件优先复用 `ImWebSocketEvent`、RabbitMQ 事件和现有推送模型。
7. 失败处理遵循“业务事实优先、推送失败降级、日志和指标可观察”。
8. 新增数据库字段或表必须先有 OpenSpec 和测试说明，再进入实现。
9. 不为了单个功能引入新基础设施；确有必要时必须先通过设计文档证明现有栈无法满足。

## 12. TDD 与验收规则

1. 每个后端子 Issue 必须先写失败测试或补充可失败的验收脚本。
2. 核心逻辑测试至少覆盖正常路径、权限拒绝、重复请求、异常依赖和回归场景。
3. 消息链路测试必须覆盖消息事实、会话事实、未读事实和推送事实的边界。
4. 好友和群权限测试必须覆盖非好友、非群成员、被移除成员、管理员和群主。
5. 动态和文件测试必须覆盖所有者、好友可见、越权访问、非法参数和逻辑删除。
6. 提交前至少运行对应模块的 Maven 测试和 `openspec validate --all --strict`。
7. 如果某个验证命令因环境缺失不能运行，必须在交付说明中写清楚缺失条件和残余风险。

## 13. 多子智能体消费协议

1. 每轮并行最多 2-3 个子 Issue，避免评审和合并压力失控。
2. 每个子智能体必须拥有明确文件所有权，不允许两个写入型子智能体同时修改同一模块核心文件。
3. 读-only 子智能体适合做代码调研、测试验证、OpenSpec 审核和安全边界复核。
4. 写入型子智能体必须被分配清晰目录，例如只负责 `chat-service`、只负责 `chat-api` 或只负责 `docs/operations`。
5. 涉及同一 OpenSpec spec 的修改必须串行，不能并行写同一 spec 文件。
6. 安全、权限、事务、消息事实和数据恢复类 Issue 必须安排测试验证人或只读 reviewer 复核。
7. 主智能体负责汇总子智能体结果、解决冲突、运行回归、更新 OpenSpec、归档 change、提交和推送。
8. 子智能体不得自行扩大范围到多端实现，除非当前 Issue 明确授权。

## 14. 后续执行流程

本文件经用户 review 通过后，进入 Superpowers `writing-plans` 阶段。计划阶段应生成或更新以下长期文件：

1. `docs/plans/PL-004-im-production-readiness-issue-plan.md`
2. `docs/design/D-003-im-production-readiness-architecture.md`
3. `docs/acceptance/A-016-im-production-readiness-issue-generation-acceptance.md`
4. `AGENTS.md`

计划通过后，再执行以下动作：

1. 使用 OpenSpec 为 P0 创建对应 change。
2. 按 4 个 Epic 和 14 个子 Issue 创建首批 GitHub Issue。
3. 给每个子 Issue 写入 Parent Epic、OpenSpec 建议、TDD 验收和代码风格规则。
4. 按 2-3 个 Issue 一组分配子智能体消费。
5. 每组完成后回归测试、更新 OpenSpec tasks、归档完成 change、提交并推送。
6. P0 完成 70%-80% 后，再评审 P1 候选池是否进入 GitHub。

## 15. 验收门禁

本设计文档通过以下门禁后，才允许进入 implementation plan：

1. 文档明确区分完整路线和首批 GitHub Issue 范围。
2. 文档没有要求一次性实现多端或 QQ 全功能。
3. 文档没有默认新增基础设施或平行协议。
4. P0 Issue 都具备生产可用价值、TDD 验收和代码参考方向。
5. P1/P2 被明确标记为候选池，不会误触发立即执行。
6. 代码风格一致性、TDD、OpenSpec 和多子智能体消费规则已经写清楚。
7. 用户 review 后明确同意，才进入 `writing-plans`。

## 16. 风险与边界

1. 生产化能力容易被功能扩张稀释，因此 P0 必须优先安全、可靠、恢复和运维。
2. 多子智能体并行可能造成同一文件冲突，因此必须按文件所有权拆分。
3. 指标和审计容易过度工程化，首版只要求能定位和验收，不建设大型平台。
4. RabbitMQ 可靠性增强可能引出 outbox 需求，首版先补发布确认、失败观测和补偿验收。
5. 数据恢复验收需要真实环境配合，无法在本地完整验证时必须记录残余风险。

## 17. 参考资料

1. OWASP WebSocket Security Cheat Sheet：用于安全边界和握手校验参考，见 <https://cheatsheetseries.owasp.org/cheatsheets/WebSocket_Security_Cheat_Sheet.html>。
2. RabbitMQ Reliability 与 Publisher Confirms 文档：用于消息发布确认和失败观测参考，见 <https://www.rabbitmq.com/docs/reliability> 与 <https://www.rabbitmq.com/docs/confirms>。
3. OpenTelemetry 文档：用于可观测性术语和兼容方向参考，见 <https://opentelemetry.io/docs/>。
4. Redis at Scale 文档：用于缓存失效和恢复边界参考，见 <https://redis.io/learn/operate/redis-at-scale/>。

## 18. 变更记录

| 日期 | 作者 | 版本 | 变更说明 |
| --- | --- | --- | --- |
| 2026-05-20 | StephenQiu30 | 0.1.0 | 初始化 IM 生产可用 Issue 路线设计 |
