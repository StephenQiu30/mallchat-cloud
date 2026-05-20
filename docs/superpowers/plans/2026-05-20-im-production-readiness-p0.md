# IM Production Readiness P0 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 MallChat 后端生产可用 P0 能力一次性编排为 GitHub Epic/Issue，并按 TDD 顺序持续消费安全、可靠性、可观测和恢复任务。

**Architecture:** 先固定文档、AGENTS 规范和 GitHub issue 队列，再按 2-3 个 issue 一组消费。每个后端功能遵循 Red-Green-Refactor：先补失败测试，再写最小实现，最后回归 OpenSpec 与 Maven 测试。

**Tech Stack:** Java 21, Spring Cloud, Sa-Token, Netty WebSocket, RabbitMQ, Redis, MyBatis-Plus, JUnit 5, Mockito, OpenSpec, GitHub CLI.

**PR Rule:** 生产化子 Issue 必须通过 m 系列 PR 消费，编号从 `m1` 开始；`m1` 对应 Issue #6，分支为 `m1-websocket-handshake-security`。

---

## File Structure

**Create**
- `docs/plans/PL-004-im-production-readiness-issue-plan.md` - 长期任务编排和 Issue 队列。
- `docs/acceptance/A-016-im-production-readiness-issue-generation-acceptance.md` - Issue 创建、规范落地和首批消费验收记录。

**Modify**
- `AGENTS.md` - 写入 IM 生产化任务消费、TDD 和多智能体协作规范。

**Later OpenSpec Changes**
- `openspec/changes/harden-websocket-handshake-security/` - WebSocket 握手鉴权和 Origin 校验。
- `openspec/changes/harden-websocket-runtime-guard/` - WebSocket 连接频率和异常断开审计。
- `openspec/changes/add-im-api-rate-limit/` - IM 核心接口限流。
- `openspec/changes/add-im-audit-log-mvp/` - 敏感操作审计日志。
- `openspec/changes/add-rabbitmq-publish-observability/` - RabbitMQ 发布确认和失败观测。
- `openspec/changes/add-im-push-failure-metrics/` - 推送失败指标化。
- `openspec/changes/verify-reconnect-message-recovery/` - 断线重连补偿验收。
- `openspec/changes/harden-message-idempotency/` - 消息幂等验收。
- `openspec/changes/add-backend-health-gates/` - 健康检查和启动门禁。
- `openspec/changes/add-im-business-metrics/` - IM 关键业务指标。
- `openspec/changes/document-im-production-runbook/` - 生产上线 Runbook。
- `openspec/changes/verify-im-core-data-recovery/` - 核心 IM 表备份恢复验收。
- `openspec/changes/verify-redis-cache-recovery/` - Redis 缓存失效恢复验收。
- `openspec/changes/harden-file-upload-boundary/` - 文件上传安全边界。

**First Consumption Code/Test Targets**
- `mallchat-common/mallchat-common-websocket/src/main/java/com/stephen/cloud/common/websocket/handler/HttpHeadersHandler.java`
- `mallchat-common/mallchat-common-websocket/src/main/java/com/stephen/cloud/common/websocket/config/WebSocketProperties.java`
- `mallchat-common/mallchat-common-websocket/src/test/java/com/stephen/cloud/common/websocket/handler/HttpHeadersHandlerTest.java`
- `mallchat-common/mallchat-common-websocket/src/test/java/com/stephen/cloud/common/websocket/config/WebSocketPropertiesTest.java`

## Task 1: Persist Project Governance

**Files:**
- Modify: `AGENTS.md`

- [ ] **Step 1: Add IM production work rules**

Insert a new section after `## 研发流程`:

```markdown
## IM 生产化任务消费规范

1. 生产化任务先进入 `docs/superpowers/specs/` 和 `docs/plans/`，再创建 GitHub Issue；不得先写代码后补规格。
2. 首批 GitHub Issue 只创建 P0，范围限定为安全、消息可靠性、可观测性和数据恢复；P1/P2 只作为候选池。
3. 后端功能必须遵循 TDD：先写失败测试并确认失败，再写最小实现，再回归相关模块测试和 `openspec validate --all --strict`。
4. 每个子 Issue 必须声明 Parent Epic、建议 OpenSpec change id、文件所有权、TDD 验收、生产验收和完成标准。
5. 每轮并行最多消费 2-3 个子 Issue；多个子智能体不得同时写同一个模块核心文件或同一个 OpenSpec spec 文件。
6. 安全、权限、事务、消息事实和数据恢复类 Issue 必须安排测试验证人或只读 reviewer 复核。
7. 主智能体负责汇总子智能体结果、解决冲突、运行回归、更新 OpenSpec tasks、归档完成 change、提交和推送。
```

- [ ] **Step 2: Verify wording is consistent**

Run:

```bash
rg -n "IM 生产化任务消费规范|TDD|OpenSpec|子 Issue|子智能体" AGENTS.md
```

Expected: the new section appears once and uses `chat-*`/OpenSpec wording consistently with the existing file.

- [ ] **Step 3: Commit governance with the task plan**

Commit happens in Task 4 after docs are created.

## Task 2: Write Long-Term Task Orchestration Docs

**Files:**
- Create: `docs/plans/PL-004-im-production-readiness-issue-plan.md`
- Create: `docs/acceptance/A-016-im-production-readiness-issue-generation-acceptance.md`

- [ ] **Step 1: Write `PL-004`**

The plan must include:
- 4 P0 Epic titles.
- 14 P0 child Issue titles.
- P1/P2 candidate pool marked as not created in GitHub yet.
- TDD and OpenSpec gates.
- Multi-agent consumption batches.
- The rule that GitHub issues are created after docs and before code.

- [ ] **Step 2: Write `A-016` in draft state**

The acceptance document must include:
- Issue creation checklist.
- Validation commands.
- GitHub issue URL table left with empty result rows only until issue creation.
- The rule that rows are filled immediately after `gh issue create`.

- [ ] **Step 3: Verify docs are not placeholders**

Run:

```bash
rg -n "TB[D]|TO[DO]|待[定]|FIX[ME]|占位[符]" docs/plans/PL-004-im-production-readiness-issue-plan.md docs/acceptance/A-016-im-production-readiness-issue-generation-acceptance.md
```

Expected: command exits with no matches.

## Task 3: Create GitHub Labels and P0 Issue Queue

**Files:**
- Modify after creation: `docs/acceptance/A-016-im-production-readiness-issue-generation-acceptance.md`

- [ ] **Step 1: Ensure labels exist**

Run:

```bash
for label in \
  "type:epic" "type:task" "priority:p0" "priority:p1" \
  "area:backend" "area:security" "area:message" "area:ops" "area:data" "area:file" \
  "needs:openspec" "needs:tdd" "agent:ready" "agent:blocked"; do
  gh label create "$label" --color "ededed" --description "MallChat IM production readiness" || true
done
```

Expected: labels are created or already exist.

- [ ] **Step 2: Create the 4 P0 Epic issues**

Run one `gh issue create` command per Epic:

```bash
gh issue create --title "[EPIC][P0] 生产安全与访问控制" --label "type:epic,priority:p0,area:backend,area:security" --body "目标：让正式用户入口、WebSocket 入口、核心 IM 接口和敏感操作有明确安全边界。范围：WebSocket 握手、连接治理、核心接口限流、敏感操作审计。非目标：不建设独立风控系统或认证中心。验收：子 Issue 全部完成，OpenSpec 全部归档，相关 Maven 测试与 openspec validate 通过。"
gh issue create --title "[EPIC][P0] 消息可靠性与可恢复" --label "type:epic,priority:p0,area:backend,area:message" --body "目标：让消息事实、推送事实和补偿事实可以被测试、观测和恢复。范围：RabbitMQ 发布观测、推送失败指标、断线重连补偿、消息幂等。非目标：不默认引入 outbox 或新消息中间件。验收：子 Issue 全部完成，OpenSpec 全部归档，相关 Maven 测试与 openspec validate 通过。"
gh issue create --title "[EPIC][P0] 可观测性与运维门禁" --label "type:epic,priority:p0,area:backend,area:ops" --body "目标：让后端服务在生产环境中可以启动前检查、运行中观察、故障时定位。范围：健康检查、关键业务指标、生产 Runbook。非目标：不新增复杂部署平台。验收：子 Issue 全部完成，OpenSpec 全部归档，Runbook 可按步骤执行。"
gh issue create --title "[EPIC][P0] 数据安全与备份恢复" --label "type:epic,priority:p0,area:backend,area:data" --body "目标：让核心 IM 数据、缓存和文件在故障后可以恢复业务事实。范围：核心表备份恢复、Redis 失效恢复、文件上传安全边界。非目标：不建设完整灾备系统。验收：子 Issue 全部完成，OpenSpec 全部归档，恢复演练有记录。"
```

Expected: each command returns a GitHub issue URL.

- [ ] **Step 3: Create the 14 P0 child issues**

Create each child issue with:
- Parent Epic title in the body.
- Suggested OpenSpec change id.
- TDD acceptance.
- Production acceptance.
- Code style consistency note.

The 14 titles are:
- `[P0][backend][security] WebSocket 握手鉴权与 Origin 校验`
- `[P0][backend][security] WebSocket 连接频率限制与异常断开审计`
- `[P0][backend][security] IM 核心接口限流策略`
- `[P0][backend][security] 敏感操作审计日志`
- `[P0][backend][mq] RabbitMQ 发布确认与失败观测 MVP`
- `[P0][backend][message] 推送失败指标化`
- `[P0][backend][message] 断线重连补偿真实链路验收`
- `[P0][backend][message] 消息幂等与重复投递验收加固`
- `[P0][backend][ops] 后端服务健康检查与启动门禁`
- `[P0][backend][observability] IM 关键业务指标埋点`
- `[P0][backend][ops] 生产上线 Runbook`
- `[P0][backend][data] 核心 IM 表备份恢复验收`
- `[P0][backend][cache] Redis 缓存失效恢复验收`
- `[P0][backend][file] 文件上传安全边界`

- [ ] **Step 4: Fill acceptance table with actual issue URLs**

Update `docs/acceptance/A-016-im-production-readiness-issue-generation-acceptance.md` with the 18 returned URLs.

## Task 4: Verify and Commit Orchestration

**Files:**
- `AGENTS.md`
- `docs/plans/PL-004-im-production-readiness-issue-plan.md`
- `docs/acceptance/A-016-im-production-readiness-issue-generation-acceptance.md`
- `docs/superpowers/plans/2026-05-20-im-production-readiness-p0.md`

- [ ] **Step 1: Run docs checks**

Run:

```bash
rg -n "TB[D]|TO[DO]|待[定]|FIX[ME]|占位[符]" AGENTS.md docs/plans/PL-004-im-production-readiness-issue-plan.md docs/acceptance/A-016-im-production-readiness-issue-generation-acceptance.md docs/superpowers/plans/2026-05-20-im-production-readiness-p0.md
git diff --check
openspec validate --all --strict
```

Expected:
- No placeholder matches.
- `git diff --check` exits 0.
- OpenSpec reports all specs passed.

- [ ] **Step 2: Commit and push**

Run:

```bash
git add AGENTS.md docs/plans/PL-004-im-production-readiness-issue-plan.md docs/acceptance/A-016-im-production-readiness-issue-generation-acceptance.md docs/superpowers/plans/2026-05-20-im-production-readiness-p0.md
git commit -m "docs: 编排IM生产可用P0任务"
git push origin main
```

Expected: `main` pushes successfully.

## Task 5: Consume First Issue with TDD

**Issue:** `[P0][backend][security] WebSocket 握手鉴权与 Origin 校验`

**PR:** `[m1] 加固 WebSocket 握手鉴权与 Origin 校验`

**Branch:** `m1-websocket-handshake-security`

**Files:**
- Create: `mallchat-common/mallchat-common-websocket/src/test/java/com/stephen/cloud/common/websocket/handler/HttpHeadersHandlerTest.java`
- Modify: `mallchat-common/mallchat-common-websocket/src/main/java/com/stephen/cloud/common/websocket/handler/HttpHeadersHandler.java`
- Modify: `mallchat-common/mallchat-common-websocket/src/main/java/com/stephen/cloud/common/websocket/config/WebSocketProperties.java`
- Test: `mallchat-common/mallchat-common-websocket/src/test/java/com/stephen/cloud/common/websocket/config/WebSocketPropertiesTest.java`

- [ ] **Step 1: Create OpenSpec change**

Run:

```bash
mkdir -p openspec/changes/harden-websocket-handshake-security/specs/websocket-runtime-contract
```

Add `proposal.md`, `tasks.md`, and `specs/websocket-runtime-contract/spec.md` describing:
- `Authorization: Bearer <token>` and `token` query parameter support.
- Missing or invalid token is rejected for IM runtime.
- Optional Origin allowlist rejects disallowed origins.
- Existing legal WebSocket connections still work.

- [ ] **Step 2: RED - write failing tests**

Add tests in `HttpHeadersHandlerTest.java` for:
- missing token closes or rejects the request.
- invalid token closes or rejects the request.
- legal token binds `HttpHeadersHandler.ATTR_USER_ID`.
- disallowed Origin closes or rejects the request when allowlist is configured.

- [ ] **Step 3: Verify RED**

Run:

```bash
mvn -pl mallchat-common/mallchat-common-websocket -Dtest=HttpHeadersHandlerTest test
```

Expected: tests fail because `HttpHeadersHandler` currently does not reject invalid/missing tokens and has no Origin allowlist behavior.

- [ ] **Step 4: GREEN - implement minimal behavior**

Implement only:
- token extraction remains compatible with header and query parameter.
- missing or invalid token returns `401 Unauthorized` and closes the channel.
- disallowed Origin returns `403 Forbidden` and closes the channel.
- accepted token stores `ATTR_USER_ID` and forwards the request.

- [ ] **Step 5: Verify GREEN and regression**

Run:

```bash
mvn -pl mallchat-common/mallchat-common-websocket -Dtest=HttpHeadersHandlerTest,WebSocketPropertiesTest test
openspec validate harden-websocket-handshake-security --strict
openspec validate --all --strict
```

Expected: tests and OpenSpec validation pass.

- [ ] **Step 6: Update issue and commit**

Run:

```bash
git add mallchat-common/mallchat-common-websocket/src/main/java/com/stephen/cloud/common/websocket/handler/HttpHeadersHandler.java mallchat-common/mallchat-common-websocket/src/main/java/com/stephen/cloud/common/websocket/config/WebSocketProperties.java mallchat-common/mallchat-common-websocket/src/test/java/com/stephen/cloud/common/websocket/handler/HttpHeadersHandlerTest.java mallchat-common/mallchat-common-websocket/src/test/java/com/stephen/cloud/common/websocket/config/WebSocketPropertiesTest.java openspec/changes/harden-websocket-handshake-security
git commit -m "test: 覆盖WebSocket握手安全边界"
git commit -m "impl: 加固WebSocket握手鉴权"
```

Expected: test commit records RED coverage, implementation commit records GREEN behavior.

## Task 6: Continue Consumption Batches

**Batch 2**
- `[P0][backend][security] WebSocket 连接频率限制与异常断开审计`
- `[P0][backend][security] IM 核心接口限流策略`

**Batch 3**
- `[P0][backend][security] 敏感操作审计日志`
- `[P0][backend][mq] RabbitMQ 发布确认与失败观测 MVP`

**Batch 4**
- `[P0][backend][message] 推送失败指标化`
- `[P0][backend][message] 断线重连补偿真实链路验收`
- `[P0][backend][message] 消息幂等与重复投递验收加固`

**Batch 5**
- `[P0][backend][ops] 后端服务健康检查与启动门禁`
- `[P0][backend][observability] IM 关键业务指标埋点`
- `[P0][backend][ops] 生产上线 Runbook`

**Batch 6**
- `[P0][backend][data] 核心 IM 表备份恢复验收`
- `[P0][backend][cache] Redis 缓存失效恢复验收`
- `[P0][backend][file] 文件上传安全边界`

Each batch must repeat the same gate:
- Write or update OpenSpec change.
- RED failing test.
- Confirm RED.
- GREEN minimal implementation.
- Module tests.
- `openspec validate --all --strict`.
- Update GitHub issue.
- Create or update the matching m-series PR.
