---
layer: Plan
doc_no: "PL-IM-010"
audience:
  - PM
  - Dev
  - QA
  - Ops
feature_area: observability-recovery
purpose: "定义 MallChat 可观测性与恢复能力的阶段计划、任务拆解、依赖关系和交付顺序。"
canonical_path: "docs/plans/PL-IM-010-observability-recovery-plan.md"
status: draft
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "docs/prd/P-IM-010-observability-recovery-prd.md"
outputs:
  - "可观测性与恢复能力的实施计划、子 Issue 拆分"
triggers:
  - "开始可观测性相关开发任务前阅读"
downstream:
  - "P0/P1/P2 子 Issue 的具体实现"
---

# PL-IM-010 可观测性与恢复 实施计划

## 1. 阶段计划

### 1.1 优先级定义

| 优先级 | 定义 | 交付周期 |
|--------|------|----------|
| P0 | 指标采集基础设施，无此则后续所有指标工作无法开展 | 1 周 |
| P1 | 核心业务可观测能力，MQ/WS 指标暴露、DLQ 持久化、日志增强 | 2 周 |
| P2 | 高可用与恢复能力，熔断降级、恢复演练、一致性检查 | 3 周 |

### 1.2 P0：Prometheus + Micrometer 指标体系

**目标**：所有微服务暴露 Prometheus 指标端点，自定义业务指标可被采集。

**任务拆解**：

1. 添加 `micrometer-registry-prometheus` 依赖到 `mallchat-service/pom.xml`
2. 配置 Nacos `common-web.yml` 启用 Prometheus 端点
3. 验证所有服务 `/actuator/prometheus` 端点返回 200
4. 验证 `mallchat.rabbitmq.*` 和 `mallchat.im.push.*` 指标出现在 Prometheus 输出中
5. 添加 docker-compose Prometheus 服务配置
6. 添加 Prometheus 抓取配置（scrape_configs）

**依赖**：无

**验收**：
- 所有服务 `/actuator/prometheus` 返回 200
- Prometheus 可抓取所有服务指标
- docker-compose 启动后 Prometheus targets 页面显示所有服务 UP

### 1.3 P1-A：MQ 与 WebSocket 指标暴露

**目标**：将现有 AtomicLong 计数器注册为 Micrometer Gauge，使 WS 连接指标可被 Prometheus 采集。

**任务拆解**：

1. 在 `ChannelManager` 中注入 `MeterRegistry`，注册以下 Gauge：
   - `mallchat.websocket.connections`（当前在线连接数）
   - `mallchat.websocket.connections.rejected`（拒绝连接数）
   - `mallchat.websocket.connections.abnormal_disconnect`（异常断开数）
2. 添加 `serverId` tag 支持多实例区分
3. 编写单元测试验证 Gauge 注册和值更新
4. 添加 Grafana docker-compose 服务和预配置面板
5. 创建 MQ 消息吞吐 Grafana 面板 JSON
6. 创建 WebSocket 连接 Grafana 面板 JSON

**依赖**：P0（Prometheus 端点可用）

**验收**：
- `/actuator/prometheus` 包含 `mallchat.websocket.connections` 指标
- Grafana 面板可展示 MQ 发布/确认/失败趋势
- Grafana 面板可展示 WS 在线连接数按 serverId 分组

### 1.4 P1-B：DLQ 持久化与重处理 API

**目标**：死信消息持久化到数据库，提供查询和重处理接口。

**任务拆解**：

1. 设计 `mallchat_dlq_message` 表结构（id, original_queue, message_body, failure_reason, status, retry_count, created_at, updated_at）
2. 创建 Entity、Mapper、Service、Controller（沿用 MallChat 分层风格）
3. 修改 WebSocket DLQ handler 和 Notification DLQ handler，注入 `DlqMessageService` 持久化
4. 实现 GET `/api/admin/dlq` 分页查询接口
5. 实现 POST `/api/admin/dlq/{id}/retry` 重处理接口（重新发送到原始队列）
6. 实现 DLQ 消息过期清理（30 天）
7. 编写单元测试和集成测试

**依赖**：无（可与 P0 并行）

**验收**：
- 死信消息写入 `mallchat_dlq_message` 表
- 管理员可查询和重处理死信消息
- 过期消息自动清理

### 1.5 P1-C：操作日志增强与健康检查脚本

**目标**：增强操作日志查询能力，添加运行时健康检查脚本。

**任务拆解**：

1. 为 `mallchat-log-service` 添加操作日志查询 API（按模块、操作类型、时间范围筛选）
2. 添加 `scripts/check-service-health.sh`：检查各服务 Actuator 健康端点
3. 添加 `scripts/check-mq-connectivity.sh`：检查 RabbitMQ 管理 API 连通性
4. 扩展 `scripts/validate-repository.sh` 支持新文档路径校验

**依赖**：P0（健康检查依赖 Actuator 端点）

**验收**：
- 操作日志可按条件查询
- 健康检查脚本返回 0 表示所有服务健康
- MQ 连通性脚本可检测队列状态

### 1.6 P2-A：Gateway 熔断降级

**目标**：Gateway 层配置 Sentinel 熔断规则，下游故障时自动降级。

**任务拆解**：

1. 创建 `nacos-config/common-sentinel.yml`，配置基础熔断规则
2. 在 Gateway 配置中启用 Sentinel 限流和熔断
3. 配置慢调用比例规则：RT > 1000ms，比例 > 50%，熔断 10s
4. 实现降级返回 503 + 标准错误响应
5. 编写集成测试验证熔断和恢复行为

**依赖**：无（可与 P0 并行）

**验收**：
- 下游服务超时时 Gateway 返回 503
- 熔断窗口结束后自动恢复
- 熔断状态可通过 Actuator 端点查看

### 1.7 P2-B：恢复演练与一致性检查

**目标**：无人值守恢复演练和数据一致性检查。

**任务拆解**：

1. 增强 `scripts/backup-im-core-tables.sh` 添加备份完整性校验
2. 增强 `scripts/verify-im-core-data-recovery.sh` 添加更多一致性检查项
3. 添加 `scripts/run-recovery-drill.sh`：一键执行备份 → 恢复 → 验证全流程
4. 添加恢复演练结果报告输出（JSON 格式）
5. 编写 CI 集成验证脚本

**依赖**：无（可与 P0 并行）

**验收**：
- 恢复演练脚本无人值守执行返回 0
- 验证报告包含所有检查项结果
- CI 可集成运行

## 2. 任务依赖图

```text
P0 (Prometheus) ──┬──→ P1-A (MQ/WS 指标 + Grafana)
                  │
                  └──→ P1-C (健康检查脚本)

P1-B (DLQ 持久化) ──→ 独立，可与 P0 并行

P2-A (Sentinel 熔断) ──→ 独立，可与 P0 并行

P2-B (恢复演练) ──→ 独立，可与 P0 并行
```

## 3. 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| Prometheus 依赖冲突 | P0 阻塞 | 先在单服务验证依赖兼容性 |
| DLQ 消息体序列化异常 | P1-B 毒丸消息无法持久化 | 保留现有 ERROR 日志作为兜底，持久化失败不影响原有流程 |
| Sentinel 与现有限流冲突 | P2-A 网关行为异常 | 先在 dev 环境验证，prod 环境灰度开启 |
| 恢复演练依赖本地 MySQL | P2-B CI 环境无法运行 | CI 使用 Testcontainers 启动临时 MySQL |

## 4. 交付顺序

1. **第 1 周**：P0（Prometheus 接入）+ P1-B（DLQ 持久化）+ P2-A（Sentinel 配置）
2. **第 2 周**：P1-A（WS 指标 + Grafana）+ P1-C（健康检查脚本）
3. **第 3 周**：P2-B（恢复演练）+ 端到端验证 + 文档完善

## 5. 关联文档

### 5.1 输入文档

1. `docs/prd/P-IM-010-observability-recovery-prd.md`

### 5.2 输出文档

1. P0/P1/P2 子 Issue（Linear）

### 5.3 下游文档

1. 各子 Issue 的具体实现 PR
