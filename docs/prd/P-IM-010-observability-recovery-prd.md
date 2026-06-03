---
layer: PRD
doc_no: "P-IM-010"
audience:
  - PM
  - Dev
  - QA
  - Ops
feature_area: observability-recovery
purpose: "定义 MallChat 可观测性与恢复能力的产品边界、验收标准和不做事项，使生产环境具备指标采集、日志追踪、告警响应和故障恢复的端到端闭环。"
canonical_path: "docs/prd/P-IM-010-observability-recovery-prd.md"
status: draft
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "MallChat Cloud 现有 Actuator、Micrometer、RabbitMQ DLQ、WebSocket ChannelManager、OperationLog 代码"
outputs:
  - "可观测性与恢复能力的 PRD 需求规格、子 Issue 拆分"
triggers:
  - "生产环境需要指标采集、日志追踪、告警响应或故障恢复时阅读"
downstream:
  - "docs/plans/PL-IM-010-observability-recovery-plan.md"
---

# P-IM-010 可观测性与恢复 生产级蓝图

## 1. 背景

MallChat Cloud 当前已具备 Spring Boot Actuator 健康探针（liveness/readiness）、自定义 Micrometer 计数器（RabbitMQ 发布确认、IM 推送结果）、操作日志 AOP 框架、Gateway 全链路 traceId 和数据库备份/恢复脚本。

但存在以下关键差距，导致生产环境无法实现可观测闭环：

1. **指标不可采集**：无 `micrometer-registry-prometheus` 依赖，无 Prometheus 端点暴露，自定义计数器无法被 Prometheus 抓取。
2. **无监控面板**：docker-compose 中无 Prometheus、Grafana 或 ELK 服务。
3. **DLQ 仅日志**：死信消息仅记录 ERROR 日志，无持久化、重处理或告警机制。
4. **WebSocket 指标未暴露**：`ChannelManager` 中 `AtomicLong` 计数器未注册为 Micrometer Gauge。
5. **无熔断器**：Gateway 引用 `common-sentinel.yml` 但文件不存在，无 Resilience4j 或 Sentinel 配置。
6. **无运行时健康检查脚本**：现有脚本仅校验仓库结构，不校验服务运行时健康。

## 2. 产品目标

### 2.1 SMART 目标

| 维度 | 目标 |
|------|------|
| Specific | 为 MallChat 全部微服务接入 Prometheus 指标采集、Grafana 面板、DLQ 持久化与告警、WebSocket 指标暴露、熔断降级、运行时健康检查和恢复演练能力 |
| Measurable | 所有服务 `/actuator/prometheus` 端点返回 200；Grafana 面板可展示 MQ/WS/业务指标；DLQ 消息持久化到数据库且支持重处理；健康检查脚本返回 0 |
| Achievable | 沿用现有 Micrometer + Actuator 体系，增量添加 Prometheus 依赖和配置；不引入全新监控框架 |
| Relevant | 直接服务于生产环境稳定性和故障恢复能力 |
| Time-bound | P0（指标采集）1 周内完成；P1（MQ/WS 指标 + DLQ + 日志增强）2 周内完成；P2（熔断 + 演练 + 一致性）3 周内完成 |

### 2.2 BDD 场景

```gherkin
Given 所有微服务已启动且注册到 Nacos
When 运维人员访问任意服务的 /actuator/prometheus 端点
Then 返回 HTTP 200 且包含 mallchat.rabbitmq.* 和 mallchat.im.push.* 指标

Given RabbitMQ 消息处理重试 3 次后仍失败
When 消息进入死信队列
Then 死信消息持久化到 mallchat_dlq_message 表
And 触发 ERROR 级别日志和可选告警通知

Given WebSocket 连接数超过阈值
When Grafana 面板展示 mallchat.websocket.connections 指标
Then 运维人员可实时观察在线连接数、拒绝连接数和异常断开数

Given 下游服务不可用
When Gateway 请求失败率超过 50%
Then Sentinel 熔断器自动降级，返回 503 而非超时
And 熔断恢复后自动探测恢复

Given 定期恢复演练计划已配置
When 执行 scripts/backup-im-core-tables.sh 和 scripts/verify-im-core-data-recovery.sh
Then 备份和恢复验证脚本返回 0 且无孤儿数据
```

## 3. 非目标

- 不引入全新监控框架（如 SkyWalking、OpenTelemetry），沿用 Micrometer + Prometheus 体系。
- 不实现分布式链路追踪（如 Jaeger、Zipkin），当前 traceId + MDC 已满足基本需求。
- 不为所有服务实现完整 Sentinel 规则，仅在 Gateway 层配置基础熔断。
- 不实现实时告警推送（如邮件、钉钉），仅记录告警事件到日志和数据库。
- 不覆盖 AI 服务的 Token 使用量监控（已有独立体系）。

## 4. 核心用户故事

### 4.1 运维人员：指标采集与面板

| 故事 | 验收标准 |
|------|----------|
| 作为运维人员，我希望所有微服务暴露 Prometheus 指标端点 | 所有服务 `/actuator/prometheus` 返回 200，包含 Micrometer 默认指标和自定义业务指标 |
| 作为运维人员，我希望通过 Grafana 面板查看 MQ 消息吞吐和延迟 | 面板展示 `mallchat.rabbitmq.publish.total`、`mallchat.rabbitmq.confirm.total` 按 bizType 分组 |
| 作为运维人员，我希望查看 WebSocket 在线连接数 | 面板展示 `mallchat.websocket.connections` 按 serverId 分组 |

### 4.2 开发人员：DLQ 持久化与重处理

| 故事 | 验收标准 |
|------|----------|
| 作为开发人员，我希望死信消息持久化到数据库 | DLQ handler 将消息写入 `mallchat_dlq_message` 表，包含原始队列、消息体、失败原因 |
| 作为开发人员，我希望通过 API 查询和重处理死信消息 | 提供 GET `/api/admin/dlq` 列表和 POST `/api/admin/dlq/{id}/retry` 重处理接口 |

### 4.3 运维人员：熔断降级

| 故事 | 验收标准 |
|------|----------|
| 作为运维人员，我希望下游服务故障时 Gateway 自动熔断 | Sentinel 配置慢调用比例规则，超过阈值后返回 503 |
| 作为运维人员，我希望熔断后自动恢复 | 熔断窗口结束后进入半开状态，探测成功则关闭熔断器 |

### 4.4 QA/运维：恢复演练与一致性检查

| 故事 | 验收标准 |
|------|----------|
| 作为 QA，我希望定期执行恢复演练 | 备份脚本和恢复验证脚本可无人值守执行，返回 0 表示成功 |
| 作为运维人员，我希望检查核心数据一致性 | 验证脚本检查 19 项孤儿关系，全部为 0 则通过 |

## 5. 数据与权限边界

- **默认采集**：Actuator 内置指标（JVM、HTTP、RabbitMQ、DataSource）+ 自定义业务指标（MQ 发布、IM 推送、WS 连接）。
- **默认不采集**：用户消息内容、敏感请求参数（密码/token/secret 已在 `OperationLogAspect` 中脱敏）。
- **存储**：DLQ 消息存储在 MySQL `mallchat_dlq_message` 表，默认保留 30 天。
- **删除**：DLQ 消息超过 30 天自动清理；操作日志按现有保留策略。
- **权限**：DLQ 查询/重处理接口仅限管理员角色（复用现有 Sa-Token 权限体系）。

## 6. 状态模型

### 6.1 DLQ 消息状态

| 状态 | 含义 | 流转 |
|------|------|------|
| PENDING | 待处理 | → RETRYING / EXPIRED |
| RETRYING | 重试中 | → RESOLVED / PENDING |
| RESOLVED | 已解决 | 终态 |
| EXPIRED | 已过期 | 终态 |

### 6.2 熔断器状态

| 状态 | 含义 | 流转 |
|------|------|------|
| CLOSED | 正常 | → OPEN（失败率超阈值） |
| OPEN | 熔断 | → HALF_OPEN（窗口结束） |
| HALF_OPEN | 半开 | → CLOSED（探测成功）/ OPEN（探测失败） |

## 7. 技术方案

仅写影响产品边界和验收的技术方案，详细设计见 `docs/plans/PL-IM-010-observability-recovery-plan.md`。

1. **Prometheus 接入**：添加 `micrometer-registry-prometheus` 依赖，配置 `management.prometheus.metrics.export.enabled=true`。
2. **WebSocket 指标暴露**：将 `ChannelManager` 中 `AtomicLong` 注册为 Micrometer Gauge。
3. **DLQ 持久化**：在 DLQ handler 中注入 `DlqMessageMapper`，将死信消息写入数据库。
4. **Sentinel 熔断**：创建 `common-sentinel.yml`，配置 Gateway 层慢调用比例规则。
5. **健康检查脚本**：扩展 `scripts/` 添加运行时健康检查脚本，覆盖 Actuator 端点和 MQ 连通性。

## 8. 关联文档

### 8.1 输入文档

1. `nacos-config/common-web.yml` — Actuator 配置
2. `nacos-config/common-rabbitmq.yml` — MQ 配置
3. `mallchat-common/mallchat-common-rabbitmq/src/main/java/.../RabbitMqPublishObservation.java` — MQ 指标
4. `mallchat-service/mallchat-notification-service/src/main/java/.../ImPushMetricsRecorder.java` — 推送指标
5. `mallchat-common/mallchat-common-websocket/src/main/java/.../ChannelManager.java` — WS 连接管理

### 8.2 输出文档

1. `docs/plans/PL-IM-010-observability-recovery-plan.md` — 实施计划

### 8.3 下游文档

1. 子 Issue P0/P1/P2 — 具体实现任务

## 9. 验收门禁

- PRD 和 Plan 已写入 docs 并通过仓库校验。
- P0/P1/P2 子 Issue 已创建并关联本 Epic。
- 后续实现必须遵循 TDD 和功能项端到端验收。

## 10. 风险与边界

- Prometheus 依赖添加后需确认不影响现有 Actuator 端点行为。
- DLQ 持久化需处理消息体序列化异常（poison pill）。
- Sentinel 配置需与现有 Gateway 限流规则协调，避免冲突。
- 恢复演练脚本依赖本地 MySQL 环境，CI 环境需额外配置。

## 11. 待确认问题

- Sentinel 版本选择：Spring Cloud Alibaba Sentinel vs Resilience4j？
- Grafana 面板是否需要导入到 docker-compose 还是独立部署？
- DLQ 消息保留策略是否需要可配置？
