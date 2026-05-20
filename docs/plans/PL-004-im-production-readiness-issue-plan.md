---
layer: Plan
doc_no: "PL-004"
audience:
  - PM
  - Dev
  - QA
  - Ops
feature_area: im-production-readiness
purpose: "编排 MallChat 后端生产可用 P0 GitHub Issue 队列和持续消费顺序。"
canonical_path: "docs/plans/PL-004-im-production-readiness-issue-plan.md"
status: review
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "docs/superpowers/specs/2026-05-20-im-production-readiness-issue-design.md"
  - "docs/superpowers/plans/2026-05-20-im-production-readiness-p0.md"
  - "AGENTS.md"
outputs:
  - "P0 GitHub Epic 与子 Issue 创建队列"
  - "P0 后端生产化持续消费批次"
  - "TDD、OpenSpec 和多子智能体执行门禁"
triggers:
  - "准备创建 IM 生产可用 P0 GitHub Issue"
  - "准备按 TDD 消费后端生产化 Issue"
  - "需要判断 P1/P2 是否可以进入 GitHub 队列"
downstream:
  - "docs/acceptance/A-016-im-production-readiness-issue-generation-acceptance.md"
  - "openspec/changes/*"
  - "GitHub P0 Issues"
---

# IM 生产可用 P0 Issue 编排计划

## 1. 背景

`SP-001` 已确认 MallChat 的下一阶段目标是正式生产可用 IM 后端，而不是一次性复刻 QQ 全功能。当前计划将设计结果拆成 GitHub 可消费队列，并规定后续按 TDD、OpenSpec 和多子智能体协作方式持续推进。

本计划只编排 P0。P1/P2 只保留候选池，不在首批 GitHub Issue 中创建。

## 2. 目标

1. 一次性创建 4 个 P0 Epic 和 14 个 P0 子 Issue。
2. 每个子 Issue 都包含 Parent Epic、建议 OpenSpec change id、代码参考、TDD 验收、生产验收和完成标准。
3. 每轮消费 2-3 个子 Issue，避免并行冲突和不可验收的大改动。
4. 每个功能实现都遵循测试先行：RED 失败测试、GREEN 最小实现、REFACTOR 小范围清理。
5. 每批完成后同步 GitHub Issue、OpenSpec tasks、验收文档、测试命令和中文提交。
6. 每个子 Issue 通过独立 PR 消费，PR 编号从 `m1` 开始递增；首个消费 PR 为 `m1`，对应 Issue #6。

## 3. 非目标

1. 不在首批 GitHub 中创建 P1/P2 Issue。
2. 不一次性修改多端客户端。
3. 不默认引入 Kafka、Elasticsearch、独立风控、独立审计中心或复杂监控平台。
4. 不用 GitHub Issue 状态替代 OpenSpec 和测试验收。

## 4. P0 Epic 队列

| 序号 | Epic | 目标 | 标签 |
| --- | --- | --- | --- |
| E1 | `[EPIC][P0] 生产安全与访问控制` | 鉴权、连接治理、限流、审计 | `type:epic`, `priority:p0`, `area:security` |
| E2 | `[EPIC][P0] 消息可靠性与可恢复` | 发布观测、推送失败、断线补偿、幂等 | `type:epic`, `priority:p0`, `area:message` |
| E3 | `[EPIC][P0] 可观测性与运维门禁` | 健康检查、业务指标、Runbook | `type:epic`, `priority:p0`, `area:ops` |
| E4 | `[EPIC][P0] 数据安全与备份恢复` | 核心表恢复、Redis 恢复、文件边界 | `type:epic`, `priority:p0`, `area:data` |

## 5. P0 子 Issue 队列

| 序号 | 子 Issue | Parent Epic | 建议 OpenSpec change id | 首要测试入口 |
| --- | --- | --- | --- | --- |
| P0-01 | `[P0][backend][security] WebSocket 握手鉴权与 Origin 校验` | E1 | `harden-websocket-handshake-security` | `mallchat-common-websocket` |
| P0-02 | `[P0][backend][security] WebSocket 连接频率限制与异常断开审计` | E1 | `harden-websocket-runtime-guard` | `mallchat-common-websocket` |
| P0-03 | `[P0][backend][security] IM 核心接口限流策略` | E1 | `add-im-api-rate-limit` | `mallchat-service`、`mallchat-gateway` |
| P0-04 | `[P0][backend][security] 敏感操作审计日志` | E1 | `add-im-audit-log-mvp` | `mallchat-service`、`mallchat-log-service` |
| P0-05 | `[P0][backend][mq] RabbitMQ 发布确认与失败观测 MVP` | E2 | `add-rabbitmq-publish-observability` | `mallchat-common-rabbitmq` |
| P0-06 | `[P0][backend][message] 推送失败指标化` | E2 | `add-im-push-failure-metrics` | `mallchat-notification-service` |
| P0-07 | `[P0][backend][message] 断线重连补偿真实链路验收` | E2 | `verify-reconnect-message-recovery` | `mallchat-chat-service` |
| P0-08 | `[P0][backend][message] 消息幂等与重复投递验收加固` | E2 | `harden-message-idempotency` | `mallchat-chat-service` |
| P0-09 | `[P0][backend][ops] 后端服务健康检查与启动门禁` | E3 | `add-backend-health-gates` | root Maven / Actuator |
| P0-10 | `[P0][backend][observability] IM 关键业务指标埋点` | E3 | `add-im-business-metrics` | `mallchat-chat-service`、`mallchat-notification-service` |
| P0-11 | `[P0][backend][ops] 生产上线 Runbook` | E3 | `document-im-production-runbook` | docs lint / OpenSpec |
| P0-12 | `[P0][backend][data] 核心 IM 表备份恢复验收` | E4 | `verify-im-core-data-recovery` | SQL / docs / integration smoke |
| P0-13 | `[P0][backend][cache] Redis 缓存失效恢复验收` | E4 | `verify-redis-cache-recovery` | `mallchat-chat-service`、`mallchat-common-cache` |
| P0-14 | `[P0][backend][file] 文件上传安全边界` | E4 | `harden-file-upload-boundary` | `mallchat-file-service` |

## 6. 消费批次

### 6.1 Batch 1：WebSocket 握手安全

1. `P0-01` 先执行，原因是 WebSocket 是实时 IM 的入口。
2. 文件所有权限定为 `mallchat-common-websocket` 和对应 OpenSpec change。
3. PR 编号为 `m1`，建议分支为 `m1-websocket-handshake-security`，PR 标题为 `[m1] 加固 WebSocket 握手鉴权与 Origin 校验`。
4. TDD 顺序为：
   - RED：未登录、非法 token、非法 Origin、合法 token 四类测试先失败。
   - GREEN：最小实现 401/403 拒绝和合法 userId 绑定。
   - REFACTOR：只清理重复提取逻辑，不改变协议。

### 6.2 Batch 2：连接治理与接口限流

1. `P0-02` 和 `P0-03` 可以并行调研，但写入时必须拆开文件所有权。
2. `P0-02` 负责 WebSocket 连接上限、重复连接、异常断开审计。
3. `P0-03` 负责消息发送、好友申请、动态发布、文件上传等核心接口限流。

### 6.3 Batch 3：审计与 RabbitMQ 发布观测

1. `P0-04` 和 `P0-05` 可以并行。
2. `P0-04` 只做敏感操作审计 MVP，不建设后台检索。
3. `P0-05` 先补发布确认和失败观测，不默认引入 outbox。

### 6.4 Batch 4：消息可靠性

1. `P0-06`、`P0-07`、`P0-08` 一组消费。
2. 先验证推送失败指标，再验证断线补偿，最后加固幂等。
3. 消息事实、会话事实、未读事实和推送事实必须分别测试。

### 6.5 Batch 5：可观测与运维

1. `P0-09`、`P0-10`、`P0-11` 一组消费。
2. 健康检查和指标只做项目现有栈能承载的最小闭环。
3. Runbook 必须说明启动、检查、回滚、恢复和常见故障定位。

### 6.6 Batch 6：数据与文件恢复

1. `P0-12`、`P0-13`、`P0-14` 一组消费。
2. 数据恢复以核心业务事实可查为验收，不建设完整灾备平台。
3. 文件上传安全先覆盖大小、类型、空文件、异常文件名和越权访问。

## 7. 多子智能体分工

1. 主智能体：维护计划、分配 Issue、控制范围、合并结果、运行回归、提交推送。
2. Explorer：只读调研代码、测试入口、OpenSpec 影响和潜在冲突。
3. Builder：在明确文件所有权内按 TDD 实现单个子 Issue。
4. Tester：验证 RED 是否真实失败、GREEN 是否通过、回归命令是否完整。
5. Reporter：更新验收文档、GitHub Issue 和交付说明。

同一轮最多派发 2-3 个子 Issue。涉及同一 OpenSpec spec 或同一核心文件时，必须串行。

## 8. PR 编号与消费规则

1. m 系列 PR 从 `m1` 开始，每个 PR 只消费一个明确的 P0 子 Issue。
2. 分支命名格式为 `m<序号>-<issue-slug>`，例如 `m1-websocket-handshake-security`。
3. PR 标题格式为 `[m<序号>] <中文动作 + 功能名>`，例如 `[m1] 加固 WebSocket 握手鉴权与 Origin 校验`。
4. PR 描述必须包含关联 Issue、OpenSpec change id、RED 测试命令与失败摘要、GREEN 验证命令与通过摘要、影响范围和残余风险。
5. `main` 只接收已 review、已验证、已归档或明确保留未归档状态的 PR，不直接承载生产化子 Issue 的开发提交。

## 9. 通用验收命令

每个后端 Issue 至少运行以下命令中的相关子集：

```bash
mvn -pl mallchat-common/mallchat-common-websocket test
mvn -pl mallchat-common/mallchat-common-rabbitmq test
mvn -pl mallchat-service/mallchat-chat-service test
mvn -pl mallchat-service/mallchat-notification-service test
mvn -pl mallchat-service/mallchat-file-service test
openspec validate --all --strict
git diff --check
```

如果命令因环境缺失无法运行，交付说明必须写清楚缺失条件、影响范围和补验计划。

## 10. P1/P2 候选池

P1 候选池在 P0 完成 70%-80% 后评审，暂不创建 GitHub Issue：

1. 拉黑与解除拉黑。
2. 举报用户/消息/动态 MVP。
3. 好友备注与轻量分组。
4. 群管理员任免。
5. 入群审批 MVP。
6. 消息搜索 MVP。
7. 通知偏好与群免打扰。

P2 候选池只保留方向：语音消息、视频消息、表情和贴纸、消息转发、动态公开广场、动态推荐流、内容审核 AI、复杂管理后台审计检索、多端完整 E2E 自动化矩阵。

## 11. 风险与边界

1. 如果 GitHub Issue 创建失败，先保留 docs 编排，不进入代码消费。
2. 如果 RED 测试无法真实失败，必须修正测试，不能直接写实现。
3. 如果实现需要引入新基础设施，必须暂停并补设计评审。
4. 如果多个子智能体产生冲突，主智能体只合并已验证且范围正确的结果。
5. 如果 OpenSpec change 不能归档，不关闭对应 GitHub Issue。

## 12. 变更记录

| 日期 | 作者 | 版本 | 变更说明 |
| --- | --- | --- | --- |
| 2026-05-20 | StephenQiu30 | 0.1.0 | 初始化 IM 生产可用 P0 Issue 编排计划 |
