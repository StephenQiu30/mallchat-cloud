---
layer: Acceptance
doc_no: "A-030"
audience:
  - Dev
  - QA
  - Ops
feature_area: backend-engineering-guard
purpose: "记录 m14 工程化守护规则落地的范围、TDD 证据、CI 接入、Code Review 和验收命令。"
canonical_path: "docs/acceptance/A-030-m14-engineering-guard-acceptance.md"
status: ready
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "docs/plans/PL-009-backend-engineering-consistency-followup-plan.md"
  - "GitHub Issue #70"
  - "GitHub Issue #71"
  - "GitHub Issue #72"
  - "GitHub Issue #73"
  - "GitHub Issue #74"
  - "GitHub Issue #75"
outputs:
  - "m14 工程化守护规则"
  - "m14 CI / PR / Agent 规范"
triggers:
  - "消费 m14 Issue #70-#75"
downstream:
  - "后续后端接口契约 PR"
  - "GitHub Issue #78"
---

# m14 工程化守护规则验收

## 1. Issue 范围

| Issue | 标题 | 状态 | 说明 |
| --- | --- | --- | --- |
| [#70](https://github.com/StephenQiu30/mallchat-cloud/issues/70) | `[m14][epic][backend] 工程化守护规则落地` | 待 PR 关闭 | m14 Epic，聚合守护规则、CI 和验收 |
| [#71](https://github.com/StephenQiu30/mallchat-cloud/issues/71) | `[m14][backend][guard] 契约一致性守护测试` | 已完成，待 PR 关闭 | 复用 E1-E3 已稳定的契约测试，并接入 CI |
| [#72](https://github.com/StephenQiu30/mallchat-cloud/issues/72) | `[m14][backend][script] 仓库校验脚本工程化规则` | 已完成，待 PR 关闭 | `validate-repository.sh` 增加低噪声、可解释的规范/模板/CI 检查 |
| [#73](https://github.com/StephenQiu30/mallchat-cloud/issues/73) | `[m14][backend][docs] PR Checklist 与 Agent 规范补充` | 已完成，待 PR 关闭 | 补充 DTO/VO、兼容性例外、只读审查和工程化守护规范 |
| [#74](https://github.com/StephenQiu30/mallchat-cloud/issues/74) | `[m14][backend][ci] 工程化轻量 CI 门禁` | 已完成，待 PR 关闭 | CI 新增 `Run engineering contract guards` |
| [#75](https://github.com/StephenQiu30/mallchat-cloud/issues/75) | `[m14][backend][qa] 工程化守护验收与 Code Review` | 已完成，待 PR 关闭 | 记录 TDD、Code Review 和验收命令 |

## 2. 守护边界

1. 本轮只沉淀 E1-E3 已验证的规则：DTO Request / VO Response、Feign/Controller 契约同步、Gateway 白名单/限流 key、RabbitMQ 公共组件行为测试。
2. `scripts/validate-repository.sh` 只检查仓库结构、长期规范、PR 模板字段和 CI 是否接入契约测试，不扫描 Java 源码。
3. multipart 上传、WebFlux Gateway、common 历史公共契约保留例外说明入口，不机械套用 DTO/VO 禁令。
4. CI 使用 focused tests，不把全仓慢速或不稳定检查强制放进工程化守护步骤。

## 3. TDD 记录

| 阶段 | 命令 | 结果 | 说明 |
| --- | --- | --- | --- |
| RED | `bash scripts/validate-repository.sh` | 失败 | 新增脚本门禁后，因 `AGENTS.md` 缺少 `DTO Request / VO Response` 规则而失败 |
| GREEN | `bash scripts/validate-repository.sh` | 通过 | 补齐 AGENTS、PR 模板和 CI 契约守护入口后转绿 |

## 4. 修正内容

1. `AGENTS.md` 新增接口契约一致性规范和工程化守护门禁，明确 DTO Request / VO Response、兼容性例外、契约测试命名和 CI 主路径。
2. `.github/pull_request_template.md` 新增 Linked Issues / OpenSpec change、Focused contract tests、Contract / Compatibility 和 Code Review 字段。
3. `scripts/validate-repository.sh` 增加 `require_text`，失败时输出明确中文原因，并检查 AGENTS、PR 模板和 CI 中的 m14 守护字段。
4. `.github/workflows/ci.yml` 新增 `Run engineering contract guards`，接入 chat/log/file/notification/user/ai/gateway/common-rabbitmq 的稳定 focused tests。
5. `PL-009` 的 E4 验收命令同步补充实际契约测试命令，避免计划和 CI 脱节。

## 5. Code Review 记录

| Reviewer | 结论 | 处理 |
| --- | --- | --- |
| 只读审查员 | 最小改动应是把已有契约测试接入 CI，并让 PR/AGENTS/脚本补证据字段；不要引入复杂静态分析平台 | 已采纳 |
| 只读审查员 | `AGENTS.md` 中历史 `npm test` 表述与 Java/Maven 后端事实不一致 | 本轮通过新增工程化守护规则和脚本检查 Maven/OpenSpec/ContractConsistencyTest 语义收敛，未继续扩大模板 |
| 只读审查员 | bash 不应扫描 `BaseResponse<Boolean>`、`DeleteRequest`、`@RequestParam` 源码 | 已采纳，源码规则保留在 Java 反射测试中 |
| 只读 Code Reviewer | CI 漏掉 m13 common/RabbitMQ 守护，脚本 CI 检查不完整，AGENTS 残留 `npm test` 旧入口 | 已修复：补 `RabbitMqSenderTest`、补全脚本测试名检查、替换真实 Maven/OpenSpec/CI 门禁描述 |

## 6. 验收命令

| 命令 | 结果 | 说明 |
| --- | --- | --- |
| `bash scripts/validate-repository.sh` | 通过 | 仓库结构、规范、PR 模板、CI 守护字段和 diff check |
| `mvn -B -pl mallchat-service/mallchat-chat-service -am -Dtest=ChatApiContractConsistencyTest -Dsurefire.failIfNoSpecifiedTests=false test` | 通过 | chat 契约守护 |
| `mvn -B -pl mallchat-service/mallchat-log-service -am -Dtest=LogApiContractConsistencyTest -Dsurefire.failIfNoSpecifiedTests=false test` | 通过 | log 契约守护 |
| `mvn -B -pl mallchat-service/mallchat-file-service -am -Dtest=FileUploadContractConsistencyTest -Dsurefire.failIfNoSpecifiedTests=false test` | 通过 | file 上传契约守护 |
| `mvn -B -pl mallchat-service/mallchat-notification-service -am -Dtest=NotificationApiContractConsistencyTest -Dsurefire.failIfNoSpecifiedTests=false test` | 通过 | notification 契约守护 |
| `mvn -B -pl mallchat-service/mallchat-user-service -am -Dtest=UserApiContractConsistencyTest -Dsurefire.failIfNoSpecifiedTests=false test` | 通过 | user 契约守护 |
| `mvn -B -pl mallchat-service/mallchat-ai-service -am -Dtest=AiApiContractConsistencyTest -Dsurefire.failIfNoSpecifiedTests=false test` | 通过 | ai 契约守护 |
| `mvn -B -pl mallchat-gateway -am -Dtest=GatewayAuthWhitelistConfigTest,RateLimitConfigTest -Dsurefire.failIfNoSpecifiedTests=false test` | 通过 | gateway 白名单和限流 key 守护 |
| `mvn -B -pl mallchat-common/mallchat-common-rabbitmq -am -Dtest=RabbitMqSenderTest -Dsurefire.failIfNoSpecifiedTests=false test` | 通过 | RabbitMQ 公共组件事务发送守护 4 tests |
| `mvn -B -DskipTests compile` | 通过 | 全仓 25 个 Maven 模块编译通过 |
| `openspec validate --all --strict` | 通过 | 21 items passed |
| `git diff --check` | 通过 | 补丁格式检查通过 |

## 7. 变更记录

| 日期 | 作者 | 版本 | 变更说明 |
| --- | --- | --- | --- |
| 2026-05-21 | StephenQiu30 | 0.1.0 | 初始化 m14 工程化守护规则验收记录 |
