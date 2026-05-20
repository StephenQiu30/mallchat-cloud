---
layer: Acceptance
doc_no: "A-018"
audience:
  - Dev
  - QA
  - Ops
feature_area: im-ops-observability
purpose: "记录 m4 可观测性与运维门禁 Epic 的测试先行、实现范围和验收命令。"
canonical_path: "docs/acceptance/A-018-m4-ops-observability-acceptance.md"
status: review
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "GitHub Issue #14"
  - "GitHub Issue #15"
  - "GitHub Issue #16"
  - "openspec/changes/add-backend-health-gates"
  - "openspec/changes/add-im-business-metrics"
  - "openspec/changes/document-im-production-runbook"
outputs:
  - "m4-backend-ops-observability-epic"
  - "后端健康检查门禁"
  - "IM 关键业务指标"
  - "生产上线 Runbook"
triggers:
  - "创建或更新 m4 PR"
  - "回归可观测性与运维门禁 Epic #4"
downstream:
  - "GitHub Epic #4"
---

# m4 可观测性与运维门禁验收

## 1. 验收范围

本次 m4 聚合消费 Epic #4 下的 #14、#15、#16。实现保持最小生产可用闭环：复用 Spring Boot Actuator、Micrometer 和现有 docs/operations 结构，不引入新的部署平台、指标中台或并行业务协议。

## 2. 结论

1. #14：公共 Web/Nacos 配置已暴露 `health`、`info`、`metrics`，并按 `common-web`、`common-cache`、`common-mysql`、`common-rabbitmq` 递进区分 liveness 与 readiness；gateway 补齐 actuator 依赖。
2. #15：chat-service 已记录 `message_send`、`friend_apply`、`moment_like`、`moment_comment` 的低基数业务计数。
3. #16：新增生产上线 Runbook，覆盖启动、健康检查、关键指标、常见故障、回滚和数据/缓存恢复。

## 3. RED 证据

1. `BackendHealthGateConfigTest` 初次运行失败：`common-web.yml` 与 `common-web-prod.yml` 仅暴露 `health,info`，缺少 `metrics`、liveness/readiness 分组。
2. `ChatBusinessMetricsRecorderTest` 与相关 chat-service 指标测试初次运行失败：缺少 `ChatBusinessMetricsRecorder`。

## 4. GREEN 命令

```bash
mvn -pl mallchat-common/mallchat-common-web -am -Dtest=BackendHealthGateConfigTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl mallchat-service/mallchat-chat-service -am -Dtest=ChatBusinessMetricsRecorderTest,ChatMessageServiceImplTest,UserFriendApplyServiceImplTest,ChatMomentServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl mallchat-gateway -am -DskipTests compile
openspec validate --all --strict
docker compose config
bash scripts/validate-repository.sh
git diff --check
```

## 5. 残余风险

1. `common-*-prod.yml` 是生产配置模板；当前服务导入的是同名 `common-*.yml` dataId，生产环境需要通过独立 Nacos namespace 维护生产值，或显式调整导入策略。
2. 业务指标仅记录低基数最小闭环，不包含用户、房间、消息、动态等高基数字段；如后续需要链路追踪，应通过日志 traceId 或独立 tracing 能力补充。
3. Actuator 生产访问边界已写入 Runbook，但仍依赖部署侧网关、Ingress 或安全组实际执行。
4. OpenSpec changes 暂不归档，待 m4 PR review 通过后统一归档。
