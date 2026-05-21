---
layer: Plan
doc_no: "PL-006"
audience:
  - PM
  - Dev
  - QA
  - Ops
feature_area: im-p2-backend-experience
purpose: "编排 MallChat P2 后端优先的富消息、动态发现、审计检索和多端验收 Issue 队列。"
canonical_path: "docs/plans/PL-006-im-p2-backend-experience-plan.md"
status: review
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "docs/superpowers/specs/2026-05-20-im-production-readiness-issue-design.md"
  - "docs/prd/P-005-message-media-and-rich-types-prd.md"
  - "docs/prd/P-007-qzone-like-moments-feed-prd.md"
outputs:
  - "P2 GitHub Epic 与子 Issue 创建队列"
  - "m8/m9/m10 后端优先 Epic PR 消费顺序"
triggers:
  - "P1 后端治理 Issue 全部关闭"
  - "准备进入 P2 高级 IM 体验能力"
downstream:
  - "GitHub P2 Issues"
  - "openspec/changes/*"
  - "docs/acceptance/*"
---

# IM P2 后端体验 Issue 编排计划

## 1. 背景

P0 生产化和 P1 后端治理队列已经合并到 `main`，当前阶段可以把 P2 候选池提升为可消费任务。P2 仍遵循“先后端，后多端”，先稳定消息类型、动态发现、后台检索和验收契约，不把 Taro、UniApp、Flutter、Admin 页面一次性混进同一个 PR。

## 2. 目标

1. 创建 3 个 P2 Epic 和 10 个 P2 子 Issue。
2. `m8` 优先消费富消息与转发体验，覆盖语音、视频、贴纸和单条转发。
3. `m9` 进入动态发现与内容治理，先做公开广场和轻量排序，AI 审核只做边界设计。
4. `m10` 进入审计检索与多端验收，先补后端 API 和 E2E 矩阵。
5. 所有实现继续复用 `chat-*` / `Chat*` 命名、现有分层和 TDD 门禁。

## 3. 非目标

1. 不实现音视频通话、转码、波形分析、复杂推荐系统或表情商城。
2. 不在 P2 后端 PR 中实现多端页面。
3. 不引入 Elasticsearch、独立推荐服务、独立审核平台或新消息中间件。
4. 不跳过 OpenSpec、测试先行、Issue/PR 关联和 CI。

## 4. P2 Epic 队列

| 序号 | Epic | 目标 | 计划 PR |
| --- | --- | --- | --- |
| E7 | `[EPIC][P2] 富消息与转发体验` | 语音、视频、表情/贴纸、单条转发和合并转发设计 | `m8` |
| E8 | `[EPIC][P2] 动态发现与内容治理` | 公开广场、轻量排序和内容审核边界 | `m9` |
| E9 | `[EPIC][P2] 审计检索与多端验收` | 管理后台审计检索后端 API 和多端 E2E 矩阵 | `m10` |

## 5. P2 子 Issue 队列

| 序号 | 子 Issue | Parent Epic | 建议 OpenSpec change id | 首要测试入口 |
| --- | --- | --- | --- | --- |
| P2-01 | `[P2][backend][message] 语音消息 MVP` | E7 | `add-voice-message-mvp` | `ChatMessageHelperTest`, `ChatSessionServiceImplTest` |
| P2-02 | `[P2][backend][message] 视频消息 MVP` | E7 | `add-video-message-mvp` | `ChatMessageHelperTest`, `ChatSessionServiceImplTest` |
| P2-03 | `[P2][backend][message] 表情与贴纸消息 MVP` | E7 | `add-sticker-message-mvp` | `ChatMessageHelperTest`, `ChatSessionServiceImplTest` |
| P2-04 | `[P2][backend][message] 单条消息转发 MVP` | E7 | `add-message-forward-mvp` | `ChatMessageServiceImplTest` |
| P2-05 | `[P2][backend][message] 合并转发后端骨架` | E7 | `design-merged-forward-message` | design first |
| P2-06 | `[P2][backend][moments] 动态公开广场 MVP` | E8 | `add-public-moments-square` | `ChatMomentServiceImplTest` |
| P2-07 | `[P2][backend][moments] 动态推荐流轻量排序` | E8 | `add-moments-light-ranking` | `ChatMomentServiceImplTest` |
| P2-08 | `[P2][backend][ai] 内容审核 AI 接入预留` | E8 | `design-content-moderation-ai-boundary` | design first |
| P2-09 | `[P2][backend][admin] 管理后台审计检索后端 API` | E9 | `add-admin-audit-search-api` | log/admin service tests |
| P2-10 | `[P2][qa] 多端 E2E 自动化矩阵规划` | E9 | `document-multi-client-e2e-matrix` | docs validation |

## 6. GitHub Issue 映射

| GitHub Issue | 类型 | 对应任务 | 计划 PR |
| --- | --- | --- | --- |
| [#36](https://github.com/StephenQiu30/mallchat-cloud/issues/36) | Epic | 富消息与转发体验 | `m8` |
| [#37](https://github.com/StephenQiu30/mallchat-cloud/issues/37) | Task | 语音消息 MVP | `m8` |
| [#38](https://github.com/StephenQiu30/mallchat-cloud/issues/38) | Task | 视频消息 MVP | `m8` |
| [#39](https://github.com/StephenQiu30/mallchat-cloud/issues/39) | Task | 表情与贴纸消息 MVP | `m8` |
| [#40](https://github.com/StephenQiu30/mallchat-cloud/issues/40) | Task | 单条消息转发 MVP | `m8` |
| [#41](https://github.com/StephenQiu30/mallchat-cloud/issues/41) | Task | 合并转发后端骨架 | `m8` 设计或后续 |
| [#42](https://github.com/StephenQiu30/mallchat-cloud/issues/42) | Epic | 动态发现与内容治理 | `m9` |
| [#43](https://github.com/StephenQiu30/mallchat-cloud/issues/43) | Task | 动态公开广场 MVP | `m9` |
| [#44](https://github.com/StephenQiu30/mallchat-cloud/issues/44) | Task | 动态推荐流轻量排序 | `m9` |
| [#45](https://github.com/StephenQiu30/mallchat-cloud/issues/45) | Task | 内容审核 AI 接入预留 | `m9` 设计 |
| [#46](https://github.com/StephenQiu30/mallchat-cloud/issues/46) | Epic | 审计检索与多端验收 | `m10` |
| [#47](https://github.com/StephenQiu30/mallchat-cloud/issues/47) | Task | 管理后台审计检索后端 API | `m10` |
| [#48](https://github.com/StephenQiu30/mallchat-cloud/issues/48) | Task | 多端 E2E 自动化矩阵规划 | `m10` |

## 7. m8 消费顺序

1. 先做语音消息，因为它只扩展消息类型和 extra 校验，风险最低。
2. 再做视频消息，复用语音消息的媒体校验模式。
3. 再做表情与贴纸消息，保持后端只保存结构化消息，不做商城。
4. 再做单条消息转发，重点验证来源房间和目标房间权限。
5. 合并转发先做后端骨架设计，若设计结论明确且不扩大表结构，再进入后续实现。

## 8. 验收门禁

1. 每个新增消息类型必须先补 `ChatMessageHelperTest` 和会话预览测试。
2. 转发能力必须先补权限、撤回、跨房间和不落库不推送测试。
3. m8 PR 必须通过 `openspec validate --all --strict`、`git diff --check`、m8 focused tests、相关模块全量测试和 GitHub CI。
4. Issue 评论和 PR 正文必须使用真实 Markdown 换行，不允许出现字面量 `\n`。

## 9. 风险与边界

1. 富消息只扩展后端消息契约，不解决端侧采集、转码、播放或上传体验。
2. 转发必须复用现有权限和发送链路，避免创建平行消息表。
3. 动态推荐和 AI 审核只能在 P2 后续设计清晰后进入实现。
4. 多端 E2E 矩阵只在后端契约稳定后推进。

## 10. 变更记录

| 日期 | 作者 | 版本 | 变更说明 |
| --- | --- | --- | --- |
| 2026-05-21 | StephenQiu30 | 0.1.0 | 初始化 P2 后端体验 Issue 编排计划 |
