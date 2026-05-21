---
layer: Acceptance
doc_no: "A-022"
audience:
  - PM
  - Dev
  - QA
  - Ops
feature_area: im-p2-backend-experience
purpose: "记录 MallChat P2 后端体验 Issue 编排、创建和 OpenSpec 归档基线的验收事实。"
canonical_path: "docs/acceptance/A-022-im-p2-issue-generation-acceptance.md"
status: review
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "docs/plans/PL-006-im-p2-backend-experience-plan.md"
  - "docs/superpowers/specs/2026-05-20-im-production-readiness-issue-design.md"
outputs:
  - "P2 GitHub Issue 创建结果"
  - "P0/P1 OpenSpec 归档基线"
triggers:
  - "开始或继续消费 IM P2 Issue"
downstream:
  - "GitHub Issues #36-#48"
  - "m8/m9/m10 Epic PR"
---

# IM P2 Issue 编排验收

## 1. 背景

P0 与 P1 后端 Issue 已经合并关闭。进入 P2 前，需要先把已完成的 OpenSpec active changes 归档到正式 spec，避免新 P2 change 和旧完成项混在一起；同时把 P2 候选池提升为可追踪的 GitHub Issue。

## 2. 验收结论

1. OpenSpec active changes 已清空，完成项已经归档到 `openspec/changes/archive/2026-05-21-*`。
2. `openspec validate --all --strict` 通过，当前正式 spec 共 20 项。
3. P2 Epic 已创建 3 个。
4. P2 子 Issue 已创建 10 个。
5. Issue 正文已抽样验证为真实 Markdown，不含字面量 `\n`。
6. 下一轮实现从 `m8-backend-rich-message-epic` 开始，优先消费 #36 下 #37、#38、#39、#40。

## 3. GitHub Issue 创建结果

| Issue | 类型 | 标题 | 计划 PR |
| --- | --- | --- | --- |
| [#36](https://github.com/StephenQiu30/mallchat-cloud/issues/36) | Epic | `[EPIC][P2] 富消息与转发体验` | `m8` |
| [#37](https://github.com/StephenQiu30/mallchat-cloud/issues/37) | Task | `[P2][backend][message] 语音消息 MVP` | `m8` |
| [#38](https://github.com/StephenQiu30/mallchat-cloud/issues/38) | Task | `[P2][backend][message] 视频消息 MVP` | `m8` |
| [#39](https://github.com/StephenQiu30/mallchat-cloud/issues/39) | Task | `[P2][backend][message] 表情与贴纸消息 MVP` | `m8` |
| [#40](https://github.com/StephenQiu30/mallchat-cloud/issues/40) | Task | `[P2][backend][message] 单条消息转发 MVP` | `m8` |
| [#41](https://github.com/StephenQiu30/mallchat-cloud/issues/41) | Task | `[P2][backend][message] 合并转发后端骨架` | `m8` 设计或后续 |
| [#42](https://github.com/StephenQiu30/mallchat-cloud/issues/42) | Epic | `[EPIC][P2] 动态发现与内容治理` | `m9` |
| [#43](https://github.com/StephenQiu30/mallchat-cloud/issues/43) | Task | `[P2][backend][moments] 动态公开广场 MVP` | `m9` |
| [#44](https://github.com/StephenQiu30/mallchat-cloud/issues/44) | Task | `[P2][backend][moments] 动态推荐流轻量排序` | `m9` |
| [#45](https://github.com/StephenQiu30/mallchat-cloud/issues/45) | Task | `[P2][backend][ai] 内容审核 AI 接入预留` | `m9` 设计 |
| [#46](https://github.com/StephenQiu30/mallchat-cloud/issues/46) | Epic | `[EPIC][P2] 审计检索与多端验收` | `m10` |
| [#47](https://github.com/StephenQiu30/mallchat-cloud/issues/47) | Task | `[P2][backend][admin] 管理后台审计检索后端 API` | `m10` |
| [#48](https://github.com/StephenQiu30/mallchat-cloud/issues/48) | Task | `[P2][qa] 多端 E2E 自动化矩阵规划` | `m10` |

## 4. 标签验收

已新增或确认以下标签：

1. `priority:p2`
2. `area:rich-message`
3. `area:moments`
4. `area:admin`
5. `area:qa`
6. `area:ai`

## 5. 编排阶段验证命令

```bash
openspec validate --all --strict
openspec list
gh issue list --state open --limit 50 --json number,title,labels
git diff --check
```

## 6. 残余风险

1. 本文档只验收 P2 队列创建，不代表 P2 功能已经完成。
2. #41、#45、#48 以设计或规划为主，不应在 m8 首批实现中强行扩大范围。
3. 富消息后端不包含端侧录音、录像、播放、贴纸商城和上传体验。

## 7. 变更记录

| 日期 | 作者 | 版本 | 变更说明 |
| --- | --- | --- | --- |
| 2026-05-21 | StephenQiu30 | 0.1.0 | 初始化 P2 Issue 编排验收记录 |
