---
layer: Plan
doc_no: "PL-010"
audience:
  - PM
  - Dev
  - QA
  - Ops
feature_area: im-backend-production-roadmap
purpose: "沉淀 MallChat 后端从 QQ-like IM MVP 到生产可用与工程化一致性的长期执行路线。"
canonical_path: "docs/plans/PL-010-im-backend-production-roadmap.md"
status: accepted
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "docs/prd/P-001-im-real-time-communication-prd.md"
  - "docs/prd/P-003-friend-discovery-and-relationship-prd.md"
  - "docs/design/D-002-qq-like-im-mvp-architecture.md"
  - "docs/design/D-003-backend-engineering-consistency-design.md"
  - "AGENTS.md"
outputs:
  - "后端生产化路线图"
  - "分批任务执行顺序"
  - "TDD、Code Review 和验收门禁"
triggers:
  - "继续消费后端 IM Issue"
  - "判断后续任务是否应进入 OpenSpec 或 GitHub Issue"
  - "检查后端工程化一致性治理范围"
downstream:
  - "openspec/changes/*"
  - "GitHub Issues / PRs"
---

# IM 后端生产化路线图

## 1. 背景

MallChat 后端目标是支撑 QQ-like IM 的基础能力：好友聊天、群组、实时消息、动态、通知和基础运营治理。此前根目录 `task_plan.md`、`findings.md`、`progress.md` 记录了大量执行过程，其中有长期价值的是任务顺序、范围边界、TDD 门禁和工程化治理批次。

本文件只保留可复用的执行路线，不收录一次性进展日志、PR 评论流水、临时验收报告或本地排查过程。单次实现仍以 OpenSpec、GitHub Issue 和 PR 为准。

## 2. 执行原则

1. 先后端，后多端；后端契约稳定后再推进 Taro、UniApp、Flutter 和 Admin 联调。
2. 一个任务必须能形成最小可用闭环，不把好友、群聊、消息、动态、通知和管理后台混成不可验收的大改动。
3. 行为变化遵循 TDD：RED 失败测试、GREEN 最小实现、REFACTOR 小范围整理。
4. 代码风格跟随现有 `chat-*` / `Chat*`、DTO Request、VO Response、MyBatis Plus、`ResultUtils.success` 和 Convert 风格。
5. 文档只沉淀长期有效结论；过程文件、临时验收和分散 SQL 迁移脚本不进入长期文档。

## 3. 路线分层

| 层级 | 目标 | 代表任务 | 完成信号 |
| --- | --- | --- | --- |
| MVP 后端闭环 | 支撑基础 QQ-like IM 主链路 | 好友发现、申请、私聊房间、群聊治理、消息、会话、已读、撤回、动态、通知 | OpenSpec 归档、focused tests 通过、PR 关联 Issue |
| P0 生产可用 | 保证上线前核心运行安全与可靠 | WebSocket 鉴权、连接治理、接口限流、审计、MQ 发布观测、推送失败指标、断线补偿、消息幂等、健康检查、备份恢复、文件边界 | 关键服务可启动，失败有可观测记录，核心事实可恢复 |
| P1 后端治理 | 补齐关系、群治理和消息体验 | 拉黑/举报、好友备注、群管理员、入群审批、消息搜索、通知偏好、群免打扰 | 不破坏现有接口兼容，权限和幂等测试通过 |
| P2 体验扩展 | 增强富消息、动态和多端验收 | 语音/视频消息、表情、转发、动态公开广场、动态治理、审计检索、多端 E2E | 后端契约明确，多端按稳定接口接入 |
| 工程化一致性 | 控制长期维护成本 | chat、log/file/notification、user/ai/gateway/common 分批治理，最后沉淀轻量守护 | DTO/VO 契约统一，CI/脚本低噪声且可解释 |

## 4. 关键任务池

### 4.1 MVP 后端闭环

1. 好友关系：搜索用户、返回 `friendStatus`、申请审批、删除好友幂等、私聊权限收敛。
2. 群组治理：创建、邀请、成员列表、群资料更新、群主移除普通成员、退群、解散。
3. 消息与会话：文本、图片、文件、引用回复、撤回、发送幂等、历史分页、未读数和读边界。
4. 实时能力：WebSocket 运行契约、在线状态、MQ 推送、会话刷新、缓存缺失兜底。
5. 动态与通知：好友可见动态、点赞、评论、互动通知、好友申请通知和群治理通知。

### 4.2 P0 生产可用

1. 安全入口：WebSocket 握手鉴权、Origin 校验、连接频率限制、异常断开审计。
2. 接口治理：消息发送、好友申请、动态发布、文件上传等核心接口限流。
3. 可靠投递：RabbitMQ 发布确认、推送失败指标、断线重连补偿、消息幂等与重复投递验收。
4. 可观测性：服务健康检查、核心业务指标、启动门禁、生产上线 Runbook。
5. 数据恢复：核心 IM 表备份恢复、Redis 缓存失效恢复、文件上传安全边界。

### 4.3 P1 后端治理

1. 用户关系治理：拉黑/解除拉黑、举报用户/消息/动态、好友备注和轻量分组。
2. 群聊治理：群管理员任免、入群审批、群公告或群设置的最小闭环。
3. 消息体验：消息搜索、通知偏好、群免打扰、撤回和已读边界补测。
4. 验收重点：权限、幂等、重复操作、缓存退化、事实数据和推送失败不互相污染。

### 4.4 P2 体验扩展

1. 富消息：语音、视频、表情、贴纸和消息转发，优先扩展现有消息模型，不新建平行消息表。
2. 动态治理：公开动态、推荐流、内容审核和审计检索必须先补设计，不进入聊天主链路。
3. 多端验收：Taro 优先，UniApp、Flutter、Admin 按后端稳定契约逐步接入。

### 4.5 工程化一致性

| 批次 | 范围 | 检查点 |
| --- | --- | --- |
| E1 | `chat` 领域 | `mallchat-api-chat` DTO/VO/Enum，chat-service Controller/Service/Entity/Convert/Mapper，前端可生成接口契约 |
| E2 | `log/file/notification` 支撑领域 | 查询请求、日志记录、文件上传、通知事实、Feign 返回 VO |
| E3 | `user/ai/gateway/common` 基础领域 | 权限、公共响应、公共工具、跨服务契约、Gateway WebFlux 边界 |
| E4 | 工程化守护 | 轻量测试、脚本、CI 和 PR 规则；只沉淀已验证稳定的规则 |

## 5. 单批执行流程

1. 自审：确认本批做什么、不做什么、依赖哪些已有模块、哪些测试先失败。
2. 规格：较大变更先建 OpenSpec proposal/design/tasks/spec delta；小修可直接走 Issue/PR。
3. 任务：Issue 必须写清 Parent Epic、范围、非目标、TDD 验收和完成标准。
4. RED：先写失败测试，证明当前行为或契约缺口真实存在。
5. GREEN：实现最小代码使测试通过，不借机扩大重构。
6. REVIEW：安排只读 Code Review，Critical / Important 必须处理。
7. 验证：运行 focused tests、必要模块 compile、`openspec validate --all --strict` 和 `git diff --check`。
8. 收口：同步 PR/Issue/OpenSpec 状态，只把长期有效结论写回 docs。

## 6. 验收门禁

后端任务按影响范围选择命令：

```bash
mvn -B -pl mallchat-service/mallchat-chat-service -am test
mvn -B -pl mallchat-service/mallchat-notification-service -am test
mvn -B -pl mallchat-service/mallchat-file-service -am test
mvn -B -pl mallchat-service/mallchat-user-service -am test
mvn -B -pl mallchat-gateway -am test
mvn -B -DskipTests compile
openspec validate --all --strict
git diff --check
bash scripts/validate-repository.sh
```

聚焦 chat-service 测试时，应使用 reactor 上下文，例如 `-pl :mallchat-chat-service -am`，避免依赖模块未构建导致误判。

## 7. 风险与边界

1. 当前路线图不代表所有高级 QQ 功能已实现；音视频、复杂表情商城、空间装扮、公开广场等仍需单独确认。
2. 管理后台、多端 UI 和后端能力不能混在同一个不可回滚 PR 中。
3. 数据库结构事实源只维护 `sql/mallchat.sql`，不维护分散的中间迁移脚本。
4. 根目录 `task_plan.md`、`findings.md`、`progress.md` 不作为长期项目文档；有价值结论应进入本文件、PRD、Design、OpenSpec 或 Issue。

## 8. 变更记录

| 日期 | 作者 | 版本 | 变更说明 |
| --- | --- | --- | --- |
| 2026-05-21 | StephenQiu30 | 0.1.0 | 沉淀根目录执行计划和历史计划文档中的长期任务路线 |
