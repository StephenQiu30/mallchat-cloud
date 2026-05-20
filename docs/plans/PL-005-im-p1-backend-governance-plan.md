---
layer: Plan
doc_no: "PL-005"
audience:
  - PM
  - Dev
  - QA
  - Ops
feature_area: im-p1-backend-governance
purpose: "编排 MallChat P1 后端用户关系治理、群治理、消息搜索和通知偏好 Issue 队列。"
canonical_path: "docs/plans/PL-005-im-p1-backend-governance-plan.md"
status: review
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "docs/superpowers/specs/2026-05-20-im-production-readiness-issue-design.md"
  - "docs/prd/P-003-friend-discovery-and-relationship-prd.md"
  - "docs/prd/P-004-group-chat-management-prd.md"
  - "docs/prd/P-006-read-receipt-and-message-state-prd.md"
  - "docs/prd/P-008-im-notification-center-prd.md"
outputs:
  - "P1 GitHub Epic 与子 Issue 创建队列"
  - "m6/m7 后端 Epic PR 消费顺序"
triggers:
  - "P0 后端生产化 Issue 全部关闭"
  - "准备进入 P1 后端治理能力"
downstream:
  - "GitHub P1 Issues"
  - "openspec/changes/*"
  - "docs/acceptance/*"
---

# IM P1 后端治理 Issue 编排计划

## 1. 背景

P0 后端生产化队列已经完成并合并到 `main`。下一轮仍遵循“先后端，后多端”的顺序，只把已经写入候选池的 P1 后端能力提升到 GitHub 队列，不直接开启 Taro、UniApp、Flutter 或 Admin 的混合实现。

## 2. 目标

1. 创建 2 个 P1 Epic 和 7 个 P1 子 Issue。
2. 子 Issue 必须包含 Parent Epic、建议 OpenSpec change id、代码参考、TDD 验收、生产验收和非目标。
3. PR 不再按单个子 Issue 过频提交，改为按 Epic 聚合：
   - `m6`：用户安全与关系治理 Epic。
   - `m7`：群聊治理与消息体验 Epic。
4. 每个 Epic PR 内仍按子 Issue 顺序执行 RED -> GREEN -> REFACTOR。
5. 实现必须复用现有 `chat-*` / `Chat*` 领域模型、Mapper/Service/VO 分层和现有异常/日志风格。

## 3. 非目标

1. 不在本轮实现多端页面。
2. 不引入 Elasticsearch、独立风控、独立审核后台或复杂权限平台。
3. 不一次性实现 QQ 全量群管理、空间治理或高级通知规则。
4. 不绕过 OpenSpec、TDD 和 GitHub Issue 关联。

## 4. P1 Epic 队列

| 序号 | Epic | 目标 | 计划 PR |
| --- | --- | --- | --- |
| E5 | `[EPIC][P1] 用户安全与关系治理` | 拉黑、举报、好友备注与轻量分组 | `m6` |
| E6 | `[EPIC][P1] 群聊治理与消息体验` | 群管理员、入群审批、消息搜索、通知偏好 | `m7` |

## 5. P1 子 Issue 队列

| 序号 | 子 Issue | Parent Epic | 建议 OpenSpec change id | 首要测试入口 |
| --- | --- | --- | --- | --- |
| P1-01 | `[P1][backend][friend] 拉黑与解除拉黑` | E5 | `add-chat-friend-blocklist` | `mallchat-chat-service` |
| P1-02 | `[P1][backend][report] 举报用户/消息/动态 MVP` | E5 | `add-chat-report-mvp` | `mallchat-chat-service`、`mallchat-log-service` |
| P1-03 | `[P1][backend][friend] 好友备注与轻量分组` | E5 | `add-friend-remark-group` | `mallchat-chat-service` |
| P1-04 | `[P1][backend][room] 群管理员任免` | E6 | `add-room-admin-role` | `mallchat-chat-service` |
| P1-05 | `[P1][backend][room] 入群审批 MVP` | E6 | `add-room-join-approval` | `mallchat-chat-service`、`mallchat-notification-service` |
| P1-06 | `[P1][backend][message] 消息搜索 MVP` | E6 | `add-message-search-mvp` | `mallchat-chat-service` |
| P1-07 | `[P1][backend][notification] 通知偏好与群免打扰` | E6 | `add-notification-preferences` | `mallchat-chat-service`、`mallchat-notification-service` |

## 6. GitHub Issue 映射

| GitHub Issue | 类型 | 对应任务 | 计划 PR |
| --- | --- | --- | --- |
| [#25](https://github.com/StephenQiu30/mallchat-cloud/issues/25) | Epic | 用户安全与关系治理 | `m6` |
| [#26](https://github.com/StephenQiu30/mallchat-cloud/issues/26) | Epic | 群聊治理与消息体验 | `m7` |
| [#27](https://github.com/StephenQiu30/mallchat-cloud/issues/27) | Task | 拉黑与解除拉黑 | `m6` |
| [#28](https://github.com/StephenQiu30/mallchat-cloud/issues/28) | Task | 举报用户/消息/动态 MVP | `m6` |
| [#29](https://github.com/StephenQiu30/mallchat-cloud/issues/29) | Task | 好友备注与轻量分组 | `m6` |
| [#30](https://github.com/StephenQiu30/mallchat-cloud/issues/30) | Task | 群管理员任免 | `m7` |
| [#31](https://github.com/StephenQiu30/mallchat-cloud/issues/31) | Task | 入群审批 MVP | `m7` |
| [#32](https://github.com/StephenQiu30/mallchat-cloud/issues/32) | Task | 消息搜索 MVP | `m7` |
| [#33](https://github.com/StephenQiu30/mallchat-cloud/issues/33) | Task | 通知偏好与群免打扰 | `m7` |

## 7. 消费顺序

### 7.1 m6：用户安全与关系治理

1. 先实现拉黑与解除拉黑，因为它会影响好友申请、聊天权限和动态可见性。
2. 再实现举报 MVP，复用现有审计日志和内容对象校验，不建设审核后台。
3. 最后实现好友备注与轻量分组，避免它和黑名单权限逻辑互相污染。

### 7.2 m7：群聊治理与消息体验

1. 先实现群管理员任免，锁定群角色权限边界。
2. 再实现入群审批，参考好友申请的申请/审核模式。
3. 再实现消息搜索 MVP，首版使用数据库条件查询和权限过滤。
4. 最后实现通知偏好与群免打扰，保证它只影响推送和展示，不影响消息事实写入。

## 8. 验收门禁

1. 每个子 Issue 必须先补失败测试，并在 PR 说明中记录 RED 命令和失败摘要。
2. 每个 Epic PR 必须通过对应模块 Maven 测试、`openspec validate --all --strict`、`git diff --check` 和 GitHub CI。
3. 每个子 Issue 完成后更新验收文档和 Issue 评论；Epic PR 合并后关闭对应 Epic。
4. 多子智能体可以并行只读调研，但同一个 Epic 的写入由主线串行整合，避免文件冲突。

## 9. 风险与边界

1. 拉黑、举报和动态可见性存在交叉，必须优先写权限测试再实现。
2. 群管理员与入群审批会影响成员角色模型，不能新建平行权限系统。
3. 消息搜索首版不引入搜索引擎，后续数据量压力另拆 P2。
4. 通知偏好不能让消息、会话和未读事实丢失，只能影响推送和展示策略。

## 10. 变更记录

| 日期 | 作者 | 版本 | 变更说明 |
| --- | --- | --- | --- |
| 2026-05-20 | StephenQiu30 | 0.1.0 | 初始化 P1 后端治理 Issue 编排计划 |
