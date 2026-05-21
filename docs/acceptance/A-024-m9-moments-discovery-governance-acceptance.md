---
layer: Acceptance
doc_no: "A-024"
audience:
  - Dev
  - QA
  - Ops
feature_area: im-moments-discovery-governance
purpose: "记录 m9 动态发现与内容治理 Epic 的测试先行、实现范围和验收命令。"
canonical_path: "docs/acceptance/A-024-m9-moments-discovery-governance-acceptance.md"
status: review
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "GitHub Issue #42"
  - "GitHub Issue #43"
  - "GitHub Issue #44"
  - "GitHub Issue #45"
  - "openspec/changes/add-public-moments-square"
  - "openspec/changes/add-moments-light-ranking"
  - "openspec/changes/design-content-moderation-ai-boundary"
outputs:
  - "m9-backend-moments-discovery-epic"
  - "动态公开广场 MVP"
  - "动态公开流轻量排序"
  - "内容审核 AI 接入边界"
triggers:
  - "创建或更新 m9 PR"
  - "回归动态发现与内容治理 Epic #42"
downstream:
  - "GitHub Epic #42"
---

# m9 动态发现与内容治理验收

## 1. 验收范围

本次 m9 聚合消费 Epic #42 下的 #43、#44、#45。实现保持最小可用闭环：动态发布支持好友可见和公开；新增公开动态广场接口；公开广场只返回公开、正常、审核通过且未删除动态；公开广场使用互动量与时间做轻量排序；内容审核只落最小状态边界，不接入外部 AI 厂商。

## 2. 结论

1. #43：`POST /chat/moment/publish` 新增 `visibility`，默认好友可见；`GET /chat/moment/public/list` 返回公开动态。
2. #44：公开广场先做权限和审核过滤，再按点赞数、评论数、创建时间和 ID 稳定排序。
3. #45：`chat_moment.audit_status` 作为动态审核状态，发布默认通过；审核未通过动态不进入公开流，也不可点赞或评论。
4. 公开动态仍遵守双向拉黑边界；非好友可举报公开动态，但拉黑关系下不可查看、互动或举报。
5. 数据库同时更新 `sql/mallchat.sql` 和 `sql/migrations/20260521_m9_moments_discovery_governance.sql`。

## 3. RED 证据

1. `ChatMomentServiceImplTest` 初次编译失败：`ChatMoment.visibility`、`ChatMoment.auditStatus`、`ChatMomentPublishRequest.visibility`、`ChatMomentVO.visibility` 不存在。
2. `ChatMomentServiceImplTest` 初次编译失败：`listPublicMoments` 和 `pagePublicMoments` 尚不存在。
3. 首次未带 `-am` 的 Maven 命令因依赖模块未参与构建失败，随后使用 `-am` 重跑确认红灯来自 m9 新契约缺失。
4. 代码审查后新增拉黑/举报边界测试，初次失败：`ChatMomentServiceImpl` 缺少拉黑判断钩子，`ChatReportServiceImpl` 不接受公开动态非好友举报。

## 4. GREEN 命令

```bash
mvn -B -pl mallchat-service/mallchat-chat-service -am -Dtest=ChatMomentServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -B -pl mallchat-service/mallchat-chat-service -am -Dtest=ChatMomentServiceImplTest,ChatReportServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test
```

focused tests 初始通过 31 个测试；审查修复后动态与举报 focused tests 通过 40 个测试。

## 5. GREEN 扩展验证

```bash
mvn -B -pl mallchat-service/mallchat-chat-service -am test
mvn -B -pl mallchat-service/mallchat-chat-service,mallchat-service/mallchat-notification-service -am -Dtest=ChatRoomServiceImplTest,ChatRoomJoinApplyServiceImplTest,ChatMessageHelperTest,ChatMessageServiceImplTest,ChatSessionServiceImplTest,ChatMomentServiceImplTest,ChatReportServiceImplTest,ChatMessagePushHandlerTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -B -DskipTests compile
openspec validate --all --strict
git diff --check
bash scripts/validate-repository.sh && docker compose config >/tmp/mallchat-compose-config.txt
```

chat-service 相关模块测试通过 217 个测试；CI focused tests 本地通过 163 个测试；全后端编译通过；OpenSpec strict 校验通过 23 项；`git diff --check` 通过；仓库结构与 Docker Compose config 校验通过，仅保留现有 `docker-compose.yml` `version` obsolete warning。

## 6. CI 补充

`.github/workflows/ci.yml` 的 backend focused tests 已把 `ChatMomentServiceImplTest` 纳入 chat-service 质量门禁，保证公开动态和审核边界在 PR 上自动回归。

## 7. OpenSpec 状态

m9 当前包含 3 个 active changes：

1. `add-public-moments-square`
2. `add-moments-light-ranking`
3. `design-content-moderation-ai-boundary`

PR 合并前需全部通过 strict 校验；合并后再归档。

## 8. 残余风险

1. m9 不包含端侧公开广场页面、动态推荐页面或管理后台审核动作。
2. m9 不接入外部 AI 审核，审核状态后续可由 m10 管理后台或异步审核任务扩展。
3. 公开广场排序是轻量规则，不代表个性化推荐。

## 9. 变更记录

| 日期 | 作者 | 版本 | 变更说明 |
| --- | --- | --- | --- |
| 2026-05-21 | StephenQiu30 | 0.1.0 | 初始化 m9 动态发现与内容治理验收 |
