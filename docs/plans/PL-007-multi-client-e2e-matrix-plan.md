---
layer: Plan
doc_no: "PL-007"
audience:
  - PM
  - Dev
  - QA
  - Ops
feature_area: multi-client-e2e-matrix
purpose: "定义 MallChat 多端 E2E 自动化的最小验收矩阵，作为 Taro、UniApp、Flutter 和 Admin 后续自动化落地基线。"
canonical_path: "docs/plans/PL-007-multi-client-e2e-matrix-plan.md"
status: review
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "GitHub Issue #48"
  - "docs/plans/PL-006-im-p2-backend-experience-plan.md"
  - "docs/prd/P-001-im-real-time-communication-prd.md"
  - "docs/prd/P-007-qzone-like-moments-feed-prd.md"
outputs:
  - "多端 E2E 验收矩阵"
  - "后续端侧自动化优先级"
triggers:
  - "进入 Taro、UniApp、Flutter 或 Admin 自动化建设"
  - "新增核心 IM API、WebSocket 事件或治理后台能力"
downstream:
  - "docs/acceptance/A-025-m10-admin-audit-e2e-acceptance.md"
  - "openspec/specs/multi-client-e2e-matrix/spec.md"
---

# 多端 E2E 自动化矩阵计划

## 1. 背景

MallChat 的产品方向是 QQ-like IM，后端已经按 P0、P1、P2 分阶段补齐实时消息、好友、群聊、动态、通知、治理和审计能力。多端自动化不能先于后端契约盲目展开，因此 m10 先定义矩阵和证据要求，后续再逐端实现。

## 2. 目标

1. 定义 Taro、UniApp、Flutter、Admin 的最小 E2E 验收范围。
2. 明确每个场景依赖的后端 API、WebSocket 事件或治理后台契约。
3. 为后续端侧自动化保留证据格式：命令、截图、日志、报告路径和阻塞项。
4. 保持“先后端，后多端”的推进顺序，不把端侧实现并入 m10。

## 3. 非目标

1. 不在 m10 内实现 Taro、UniApp、Flutter 或 Admin 自动化脚本。
2. 不在本计划内锁定新的端侧 E2E 框架选型。
3. 不用矩阵替代具体 PR 的测试命令和验收报告。

## 4. 多端矩阵

| 优先级 | 验收面 | Taro | UniApp | Flutter | Admin | 后端契约 |
| --- | --- | --- | --- | --- | --- | --- |
| P0 | 登录态与未登录态 | 必测 | 跟随 | 跟随 | 必测 | 用户登录态、鉴权失败响应 |
| P0 | 会话列表与未读数 | 必测 | 跟随 | 跟随 | 只读治理 | `chat-session`、消息事件 |
| P0 | 私聊消息发送与接收 | 必测 | 跟随 | 跟随 | 可检索 | `chat-message`、WebSocket 推送 |
| P0 | 群聊消息发送与接收 | 必测 | 跟随 | 跟随 | 可检索 | `chat-room-access`、`chat-message` |
| P0 | 通知恢复 | 必测 | 跟随 | 跟随 | 可检索 | 通知中心、推送失败降级 |
| P1 | 好友申请、通过、拉黑 | 必测 | 跟随 | 跟随 | 可治理 | `chat-friend` |
| P1 | 群邀请、入群审核、群管理员 | 必测 | 跟随 | 跟随 | 可治理 | `chat-room-access` |
| P1 | 撤回、已读、回复、转发 | 必测 | 跟随 | 跟随 | 可检索 | `chat-message`、`chat-session` |
| P1 | 图片、文件、语音、视频、贴纸消息 | 必测 | 跟随 | 跟随 | 可检索 | `file-upload-boundary`、`chat-message` |
| P1 | 动态发布、列表、互动、公开广场 | 必测 | 跟随 | 跟随 | 可治理 | `moments-feed`、`chat-report` |
| P2 | 管理后台审计检索 | 不适用 | 不适用 | 不适用 | 必测 | `sensitive-operation-audit` |
| P2 | 跨端一致性回归 | 抽样 | 抽样 | 抽样 | 抽样 | API、WebSocket、通知事实 |

## 5. 状态矩阵

每个端侧场景进入自动化实现前，至少需要覆盖以下状态：

1. 真实数据态：使用真实后端接口或可复用测试数据，不只 mock UI。
2. 空态：无好友、无会话、无动态、无通知时页面可用。
3. 未登录态：接口 401/未登录时引导明确，不出现空白页。
4. 加载态：慢接口和 WebSocket 重连时有可见状态。
5. 错误态：权限拒绝、目标不存在、重复提交和网络失败有稳定处理。
6. 窄屏布局：小屏设备上按钮、文本、会话和消息气泡不重叠。

## 6. 证据要求

1. 每个端侧 PR 必须记录执行命令、设备或浏览器环境、截图或报告路径。
2. WebSocket 场景必须保留连接、消息事件和恢复结果证据。
3. 后端治理场景必须能追溯到 API 响应、审计日志或验收文档。
4. 未覆盖项必须标为残余风险，不得写成“已完成”。

## 7. 风险与边界

1. Taro 是移动端 UI 还原和 IM 主流程验证优先落点，UniApp 和 Flutter 跟随后端契约补齐。
2. Admin 只验收治理能力，不承担移动端聊天体验验收。
3. 端侧自动化建设开始前，需要先确认各端启动命令、测试环境和登录态注入方式。

## 8. 变更记录

| 日期 | 作者 | 版本 | 变更说明 |
| --- | --- | --- | --- |
| 2026-05-21 | StephenQiu30 | 0.1.0 | 初始化多端 E2E 自动化矩阵计划 |
