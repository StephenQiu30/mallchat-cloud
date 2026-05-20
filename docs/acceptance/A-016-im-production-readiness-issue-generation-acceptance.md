---
layer: Acceptance
doc_no: "A-016"
audience:
  - PM
  - Dev
  - QA
  - Ops
feature_area: im-production-readiness
purpose: "记录 MallChat 后端生产可用 P0 Issue 编排、创建和后续消费门禁的验收事实。"
canonical_path: "docs/acceptance/A-016-im-production-readiness-issue-generation-acceptance.md"
status: review
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "docs/superpowers/specs/2026-05-20-im-production-readiness-issue-design.md"
  - "docs/superpowers/plans/2026-05-20-im-production-readiness-p0.md"
  - "docs/plans/PL-004-im-production-readiness-issue-plan.md"
  - "AGENTS.md"
outputs:
  - "P0 GitHub Issue 创建结果"
  - "编排阶段验证命令"
  - "后续 TDD 消费验收门禁"
triggers:
  - "检查 P0 GitHub Issue 是否已按计划创建"
  - "开始或继续消费 IM 生产化 Issue"
  - "评估 P1/P2 是否可以进入 GitHub 队列"
downstream:
  - "GitHub Issues #2-#19"
  - "openspec/changes/*"
---

# IM 生产可用 P0 Issue 编排验收

## 1. 背景

本验收文档记录 `SP-001` 和 `PL-004` 的首批编排结果。当前阶段只验证任务队列和项目规范已经落地，不声称 P0 功能已经完成。

## 2. 验收结论

1. P0 Epic 已创建 4 个。
2. P0 子 Issue 已创建 14 个。
3. P1/P2 未进入 GitHub 首批队列，仍保留在候选池。
4. `AGENTS.md` 已写入 IM 生产化 Issue 消费、TDD、OpenSpec 和多子智能体协作规范。
5. 后续每个功能 Issue 必须按 RED -> GREEN -> REFACTOR 消费，并同步 OpenSpec tasks 与 GitHub Issue 状态。

## 3. GitHub Issue 创建结果

### 3.1 P0 Epic

| Issue | 标题 | 范围 |
| --- | --- | --- |
| [#2](https://github.com/StephenQiu30/mallchat-cloud/issues/2) | `[EPIC][P0] 生产安全与访问控制` | WebSocket 握手、连接治理、核心接口限流、敏感操作审计 |
| [#3](https://github.com/StephenQiu30/mallchat-cloud/issues/3) | `[EPIC][P0] 消息可靠性与可恢复` | RabbitMQ 发布观测、推送失败指标、断线补偿、消息幂等 |
| [#4](https://github.com/StephenQiu30/mallchat-cloud/issues/4) | `[EPIC][P0] 可观测性与运维门禁` | 健康检查、关键业务指标、生产 Runbook |
| [#5](https://github.com/StephenQiu30/mallchat-cloud/issues/5) | `[EPIC][P0] 数据安全与备份恢复` | 核心表恢复、Redis 恢复、文件边界 |

### 3.2 P0 子 Issue

| Issue | 标题 | Parent Epic | 建议 OpenSpec change id |
| --- | --- | --- | --- |
| [#6](https://github.com/StephenQiu30/mallchat-cloud/issues/6) | `[P0][backend][security] WebSocket 握手鉴权与 Origin 校验` | #2 | `harden-websocket-handshake-security` |
| [#7](https://github.com/StephenQiu30/mallchat-cloud/issues/7) | `[P0][backend][security] WebSocket 连接频率限制与异常断开审计` | #2 | `harden-websocket-runtime-guard` |
| [#8](https://github.com/StephenQiu30/mallchat-cloud/issues/8) | `[P0][backend][security] IM 核心接口限流策略` | #2 | `add-im-api-rate-limit` |
| [#9](https://github.com/StephenQiu30/mallchat-cloud/issues/9) | `[P0][backend][security] 敏感操作审计日志` | #2 | `add-im-audit-log-mvp` |
| [#10](https://github.com/StephenQiu30/mallchat-cloud/issues/10) | `[P0][backend][mq] RabbitMQ 发布确认与失败观测 MVP` | #3 | `add-rabbitmq-publish-observability` |
| [#11](https://github.com/StephenQiu30/mallchat-cloud/issues/11) | `[P0][backend][message] 推送失败指标化` | #3 | `add-im-push-failure-metrics` |
| [#12](https://github.com/StephenQiu30/mallchat-cloud/issues/12) | `[P0][backend][message] 断线重连补偿真实链路验收` | #3 | `verify-reconnect-message-recovery` |
| [#13](https://github.com/StephenQiu30/mallchat-cloud/issues/13) | `[P0][backend][message] 消息幂等与重复投递验收加固` | #3 | `harden-message-idempotency` |
| [#14](https://github.com/StephenQiu30/mallchat-cloud/issues/14) | `[P0][backend][ops] 后端服务健康检查与启动门禁` | #4 | `add-backend-health-gates` |
| [#15](https://github.com/StephenQiu30/mallchat-cloud/issues/15) | `[P0][backend][observability] IM 关键业务指标埋点` | #4 | `add-im-business-metrics` |
| [#16](https://github.com/StephenQiu30/mallchat-cloud/issues/16) | `[P0][backend][ops] 生产上线 Runbook` | #4 | `document-im-production-runbook` |
| [#17](https://github.com/StephenQiu30/mallchat-cloud/issues/17) | `[P0][backend][data] 核心 IM 表备份恢复验收` | #5 | `verify-im-core-data-recovery` |
| [#18](https://github.com/StephenQiu30/mallchat-cloud/issues/18) | `[P0][backend][cache] Redis 缓存失效恢复验收` | #5 | `verify-redis-cache-recovery` |
| [#19](https://github.com/StephenQiu30/mallchat-cloud/issues/19) | `[P0][backend][file] 文件上传安全边界` | #5 | `harden-file-upload-boundary` |

## 4. 标签验收

已创建或确认存在以下标签：

1. `type:epic`
2. `type:task`
3. `priority:p0`
4. `priority:p1`
5. `area:backend`
6. `area:security`
7. `area:message`
8. `area:ops`
9. `area:data`
10. `area:file`
11. `area:mq`
12. `area:observability`
13. `area:cache`
14. `needs:openspec`
15. `needs:tdd`
16. `agent:ready`
17. `agent:blocked`

## 5. 后续消费门禁

每个子 Issue 进入实现前必须满足：

1. 已确认 Parent Epic 和建议 OpenSpec change id。
2. 已写入或更新 OpenSpec proposal、tasks 和 spec delta。
3. 已明确文件所有权，避免多个子智能体写同一核心文件。
4. 已写 RED 测试并确认失败原因正确。
5. GREEN 实现只覆盖测试要求的最小行为。
6. 回归命令包含相关 Maven 测试、`openspec validate --all --strict` 和 `git diff --check`。
7. 完成后更新 GitHub Issue、OpenSpec tasks、验收文档和中文提交。

## 6. 编排阶段验证命令

编排阶段需要验证以下命令：

```bash
gh issue list --limit 30 --state open --json number,title,labels,url
rg -n "TB[D]|TO[DO]|待[定]|FIX[ME]|占位[符]" AGENTS.md docs/plans/PL-004-im-production-readiness-issue-plan.md docs/acceptance/A-016-im-production-readiness-issue-generation-acceptance.md docs/superpowers/plans/2026-05-20-im-production-readiness-p0.md
git diff --check
openspec validate --all --strict
```

## 7. 残余风险

1. 本文档只验收 P0 队列创建，不代表 P0 功能已经实现。
2. GitHub Issue 已创建，后续如调整标题或范围，必须同步更新本文档。
3. P1/P2 暂不创建，防止干扰 P0 生产化主线。
4. 多子智能体消费时仍需主智能体把关文件所有权和 OpenSpec 串行写入。

## 8. 变更记录

| 日期 | 作者 | 版本 | 变更说明 |
| --- | --- | --- | --- |
| 2026-05-20 | StephenQiu30 | 0.1.0 | 初始化 P0 Issue 编排验收记录 |
