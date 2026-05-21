---
layer: Plan
doc_no: "PL-009"
audience:
  - PM
  - Dev
  - QA
  - Ops
feature_area: backend-engineering-consistency
purpose: "细化 E2/E3/E4 后端工程化一致性治理的领域拆分、Issue 编排、TDD 验收和自审结论。"
canonical_path: "docs/plans/PL-009-backend-engineering-consistency-followup-plan.md"
status: review
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "docs/design/D-003-backend-engineering-consistency-design.md"
  - "docs/plans/PL-008-backend-engineering-consistency-plan.md"
  - "docs/acceptance/A-026-m11-chat-consistency-acceptance.md"
outputs:
  - "E2/E3/E4 后端工程化一致性治理 Issue 编排"
  - "m12/m13/m14 分批治理边界"
triggers:
  - "m11 Chat 工程化一致性治理完成后"
  - "准备消费 log/file/notification/user/ai/gateway/common 领域治理任务"
downstream:
  - "docs/acceptance/A-027-backend-engineering-followup-issue-acceptance.md"
  - "m12/m13/m14 GitHub Issue 与 PR"
---

# 后端工程化一致性后续批次计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans when implementing code changes from this plan. Each implementation issue must follow TDD when API, behavior, permission, conversion, gateway or common contract changes are involved.

**Goal:** 将 m11 的 Chat 治理样板扩展到支撑领域、基础领域和工程化守护，同时避免一次性全仓扫荡。

**Architecture:** E2 先治理支撑服务契约，E3 再治理基础领域和跨服务边界，E4 最后把已稳定的规则沉淀为轻量测试或脚本守护。每批先审查事实，再按 P0/P1/P2 执行最小修正。

**Tech Stack:** Java 21, Spring Cloud, Spring MVC / WebFlux Gateway, OpenFeign, MyBatis Plus, Maven, JUnit, OpenSpec, GitHub Issues / PR.

---

## 1. 背景

m11 已完成 `chat` 领域工程化一致性治理，并形成以下样板：

1. API 契约优先放在 `mallchat-api-*`。
2. 面向前端或跨服务生成契约的接口使用 DTO Request / QueryRequest。
3. 面向端侧或跨服务的响应使用 VO，不长期暴露裸 `Boolean`、裸 `Long`。
4. 行为或契约变化必须先 RED，再 GREEN。
5. 只读 Code Review 的 Critical / Important 必须合并前处理。

后续 E2/E3/E4 不应机械复制 m11 的所有改法，而是按领域职责审查：支撑服务重在事实记录与通知/文件边界，基础领域重在权限、登录、网关和 common 兼容性，工程化守护重在轻量可维护的规则。

## 2. 总体批次

| 批次 | 计划 PR | 范围 | 目标 |
| --- | --- | --- | --- |
| E2 | `m12` | `log` / `file` / `notification` | 统一支撑服务查询请求、事实记录、文件上传、通知返回 VO 和 Feign 契约 |
| E3 | `m13` | `user` / `ai` / `gateway` / `common` | 统一基础领域权限、公共响应、公共工具、跨服务契约和网关边界 |
| E4 | `m14` | 工程化守护 | 将稳定规则沉淀为轻量测试、脚本或 PR 规则，不引入复杂平台 |

> 说明：原批次表中的 `m144` 视为笔误，后续统一使用 `m14`。

## 3. E2 支撑领域编排

### 3.1 审查范围

| 子领域 | API 契约 | Service 实现 | 重点 |
| --- | --- | --- | --- |
| `log` | `mallchat-api/mallchat-api-log/src/main/java/com/stephen/cloud/api/log` | `mallchat-service/mallchat-log-service/src/main/java/com/stephen/cloud/log` | 日志新增/删除/查询、VO 转换、Feign 返回类型 |
| `file` | `mallchat-api/mallchat-api-file/src/main/java/com/stephen/cloud/api/file` | `mallchat-service/mallchat-file-service/src/main/java/com/stephen/cloud/file` | 文件上传请求边界、`bizType` 枚举、上传记录、文件 VO |
| `notification` | `mallchat-api/mallchat-api-notification/src/main/java/com/stephen/cloud/api/notification` | `mallchat-service/mallchat-notification-service/src/main/java/com/stephen/cloud/notification` | 通知创建/读取/删除/未读数、MQ 事实、Feign 返回类型 |

### 3.2 初始事实信号

本轮只做编排，不在本计划中直接修代码。当前静态扫描发现的候选问题包括：

1. `LogFeignClient` 和 log Controller 存在 `BaseResponse<Boolean>`。
2. log 删除接口复用通用 `DeleteRequest`。
3. `FileController.uploadFile` 和 `FileFeignClient.uploadFile` 使用 `@RequestParam("bizType") String bizType`，需要审查是否应定义文件上传请求 DTO 或保留 multipart 兼容边界。
4. `NotificationController` 存在 `BaseResponse<Long>`、`BaseResponse<Boolean>`、`BaseResponse<List<Long>>`、通用 `DeleteRequest` 和 `@RequestParam("id")`。
5. `NotificationFeignClient` 存在 `BaseResponse<Long>` 和 `@RequestParam("id")`。

### 3.3 Issue 拆分

| Issue | 优先级 | 范围 | 产出 |
| --- | --- | --- | --- |
| [#58](https://github.com/StephenQiu30/mallchat-cloud/issues/58) | P1 | E2 总 Epic | 串联 m12 子任务、PR、验收文档 |
| [#59](https://github.com/StephenQiu30/mallchat-cloud/issues/59) | P1 | log/file/notification 事实审查 | 输出 A-028 审查清单，不改代码 |
| [#60](https://github.com/StephenQiu30/mallchat-cloud/issues/60) | P1 | log API / Controller / Feign | TDD 收敛删除请求、布尔响应和日志 VO 契约 |
| [#61](https://github.com/StephenQiu30/mallchat-cloud/issues/61) | P1 | file API / Controller / Service | 审查 multipart 请求边界，避免为了 DTO 破坏上传调用；必要时新增响应 VO 或请求元数据 DTO |
| [#62](https://github.com/StephenQiu30/mallchat-cloud/issues/62) | P1 | notification API / Controller / Feign / MQ handler | TDD 收敛通知 ID、操作结果、未读数和批量操作响应 |
| [#63](https://github.com/StephenQiu30/mallchat-cloud/issues/63) | P1 | E2 验收 | focused tests、compile、OpenSpec、只读 Code Review 和 PR 证据 |

### 3.4 E2 验收命令

```bash
mvn -B -pl mallchat-service/mallchat-log-service -am -Dsurefire.failIfNoSpecifiedTests=false test
mvn -B -pl mallchat-service/mallchat-file-service -am -Dsurefire.failIfNoSpecifiedTests=false test
mvn -B -pl mallchat-service/mallchat-notification-service -am -Dsurefire.failIfNoSpecifiedTests=false test
mvn -B -DskipTests compile
openspec validate --all --strict
git diff --check
bash scripts/validate-repository.sh
```

## 4. E3 基础领域编排

### 4.1 审查范围

| 子领域 | 路径 | 重点 |
| --- | --- | --- |
| `user` | `mallchat-api-user`、`mallchat-user-service` | 登录、登出、用户增删改查、管理员判断、用户 VO 和 Feign 契约 |
| `ai` | `mallchat-api-ai`、`mallchat-ai-service` | AI 聊天请求/响应、聊天记录查询/删除、模型枚举、MQ 记录 |
| `gateway` | `mallchat-gateway` | 鉴权过滤、Header 清洗、限流、异常响应和路由边界 |
| `common` | `mallchat-common-*` | 公共响应、公共请求、认证、缓存、Web、RabbitMQ、WebSocket、日志切面 |

### 4.2 初始事实信号

当前静态扫描发现的候选问题包括：

1. `UserController` 存在 `BaseResponse<Boolean>`、`BaseResponse<Long>`、通用 `DeleteRequest` 和多个 `@RequestParam`。
2. `UserFeignClient` 存在 `@RequestParam` 和 `BaseResponse<Boolean>`。
3. `AiFeignClient` / `AiChatRecordController` 复用通用 `DeleteRequest`，并返回 `BaseResponse<Boolean>`。
4. `common` 中的 `DeleteRequest`、`BaseResponse`、`PageRequest` 属于历史公共契约，不应在 E3 中直接删除或改语义；只能补规范、测试或限定使用边界。
5. `gateway` 是 WebFlux 边界，不能套用 Spring MVC Controller DTO 规则；重点应是异常响应、鉴权跳过路径、Header 透传/清洗和限流规则一致性。

### 4.3 Issue 拆分

| Issue | 优先级 | 范围 | 产出 |
| --- | --- | --- | --- |
| [#64](https://github.com/StephenQiu30/mallchat-cloud/issues/64) | P1 | E3 总 Epic | 串联 user/ai/gateway/common 子任务 |
| [#65](https://github.com/StephenQiu30/mallchat-cloud/issues/65) | P1 | user API / Controller / Feign | TDD 审查登录、用户管理、管理员判断和 VO 响应 |
| [#66](https://github.com/StephenQiu30/mallchat-cloud/issues/66) | P1 | ai API / Controller / Feign / MQ | TDD 审查 AI 聊天记录、删除请求、AI 响应命名 |
| [#67](https://github.com/StephenQiu30/mallchat-cloud/issues/67) | P1 | gateway filters / handler / config | 审查鉴权、Header 清洗、限流和异常响应，不强行引入 MVC DTO |
| [#68](https://github.com/StephenQiu30/mallchat-cloud/issues/68) | P1 | common core/auth/cache/web/rabbitmq/websocket/log | 审查公共契约使用边界，只补护栏，不做破坏性重命名 |
| [#69](https://github.com/StephenQiu30/mallchat-cloud/issues/69) | P1 | E3 验收 | focused tests、compile、OpenSpec、只读 Code Review 和 PR 证据 |

### 4.4 E3 验收命令

```bash
mvn -B -pl mallchat-service/mallchat-user-service -am -Dsurefire.failIfNoSpecifiedTests=false test
mvn -B -pl mallchat-service/mallchat-ai-service -am -Dsurefire.failIfNoSpecifiedTests=false test
mvn -B -pl mallchat-gateway -am -Dsurefire.failIfNoSpecifiedTests=false test
mvn -B -DskipTests compile
openspec validate --all --strict
git diff --check
bash scripts/validate-repository.sh
```

如果修改 `mallchat-common-*`，必须按受影响模块增加 focused tests，例如：

```bash
mvn -B -pl mallchat-common/mallchat-common-web -am test
mvn -B -pl mallchat-common/mallchat-common-auth -am test
mvn -B -pl mallchat-common/mallchat-common-rabbitmq -am test
```

## 5. E4 工程化守护编排

### 5.1 守护范围

E4 只沉淀已经在 E1-E3 验证有效的规则，不提前写复杂平台。候选守护包括：

1. API 契约反射测试：扫描目标 Controller / Feign 是否存在不允许的裸响应或通用请求。
2. 文档/Issue/PR checklist：要求记录 RED/GREEN、focused tests、compile、OpenSpec 和 Code Review。
3. 轻量脚本：复用 `scripts/validate-repository.sh`，必要时补充工程化一致性检查入口。
4. CI：只加入稳定、快速、可解释的检查，不把全仓历史问题一次性变成阻塞。

### 5.2 Issue 拆分

| Issue | 优先级 | 范围 | 产出 |
| --- | --- | --- | --- |
| [#70](https://github.com/StephenQiu30/mallchat-cloud/issues/70) | P1 | E4 总 Epic | 串联守护规则、CI 和验收 |
| [#71](https://github.com/StephenQiu30/mallchat-cloud/issues/71) | P1 | Java 反射测试或模块测试 | 抽取 E1-E3 稳定规则，限制回归 |
| [#72](https://github.com/StephenQiu30/mallchat-cloud/issues/72) | P1 | `scripts/validate-repository.sh` | 增加轻量可解释的工程化检查，不引入复杂平台 |
| [#73](https://github.com/StephenQiu30/mallchat-cloud/issues/73) | P1 | docs / PR template / AGENTS 补充 | 固化 TDD、Code Review、DTO/VO 和兼容性说明 |
| [#74](https://github.com/StephenQiu30/mallchat-cloud/issues/74) | P1 | `.github/workflows` | 只接入快速稳定检查，避免 CI 噪声 |
| [#75](https://github.com/StephenQiu30/mallchat-cloud/issues/75) | P1 | E4 验收 | 验证脚本、CI、文档和只读 Code Review |

### 5.3 E4 验收命令

```bash
bash scripts/validate-repository.sh
mvn -B -pl mallchat-service/mallchat-chat-service -am -Dtest=ChatApiContractConsistencyTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -B -pl mallchat-service/mallchat-log-service -am -Dtest=LogApiContractConsistencyTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -B -pl mallchat-service/mallchat-file-service -am -Dtest=FileUploadContractConsistencyTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -B -pl mallchat-service/mallchat-notification-service -am -Dtest=NotificationApiContractConsistencyTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -B -pl mallchat-service/mallchat-user-service -am -Dtest=UserApiContractConsistencyTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -B -pl mallchat-service/mallchat-ai-service -am -Dtest=AiApiContractConsistencyTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -B -pl mallchat-gateway -am -Dtest=GatewayAuthWhitelistConfigTest,RateLimitConfigTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -B -pl mallchat-common/mallchat-common-rabbitmq -Dtest=RabbitMqSenderTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -B -DskipTests compile
openspec validate --all --strict
git diff --check
```

如果新增 Java 守护测试，需要增加对应 focused test 命令，并确保 CI 能在合理时间内完成。

## 6. 编排自审

### 6.1 逻辑闭环

1. E2 先处理支撑服务，因为 log/file/notification 被 chat、user、admin 等上层流程依赖。
2. E3 再处理基础领域，因为 user/common/gateway/ai 的影响面更大，需要 E2 的支撑边界先稳定。
3. E4 最后沉淀守护，因为守护规则必须来自 E1-E3 的已验证经验，不能先写工具再倒逼业务改法。

### 6.2 风格一致性

1. DTO/VO 规则沿用 m11，但不机械套到 multipart 上传和 WebFlux gateway。
2. Feign 与 Controller 契约必须同步，避免 m11 首轮 review 暴露的响应不一致问题回流。
3. 公共 `DeleteRequest`、`BaseResponse`、`PageRequest` 只限制对外稳定契约中的滥用，不在 E3 直接删除。

### 6.3 TDD 与验收

1. 纯审查 issue 不写伪 RED，只运行 OpenSpec、diff check 和仓库校验。
2. API 或行为变化 issue 必须先写 RED 测试证明缺口。
3. 每个 Epic 必须有 QA / review issue 收口，记录只读 reviewer 的 Critical / Important / Minor。

### 6.4 过度设计拦截

1. 不引入新接口生成框架。
2. 不新增复杂静态分析平台。
3. 不为单个返回值创建大量空壳 VO；简单 ID / 操作结果可以按领域复用清晰 VO。
4. 不把历史低收益命名差异升级成 P0。

## 7. 待执行顺序

1. 先消费 [#59](https://github.com/StephenQiu30/mallchat-cloud/issues/59)，确认 m12 支撑领域 P0/P1/P2 后再进入代码修正。
2. m12 按 [#58](https://github.com/StephenQiu30/mallchat-cloud/issues/58) 一个 Epic PR 收口，对应计划 PR `m12`。
3. m13 按 [#64](https://github.com/StephenQiu30/mallchat-cloud/issues/64) 一个 Epic PR 收口，对应计划 PR `m13`。
4. m14 按 [#70](https://github.com/StephenQiu30/mallchat-cloud/issues/70) 一个 Epic PR 收口，对应计划 PR `m14`。
5. 不按单个小修频繁提交 PR；必要中间提交可以保留在同一 Epic 分支内。

## 8. 变更记录

| 日期 | 作者 | 版本 | 变更说明 |
| --- | --- | --- | --- |
| 2026-05-21 | StephenQiu30 | 0.1.0 | 初始化 E2/E3/E4 后端工程化一致性后续批次计划 |
