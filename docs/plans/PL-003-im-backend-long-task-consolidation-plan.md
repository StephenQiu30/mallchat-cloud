---
layer: Plan
doc_no: "PL-003"
audience:
  - PM
  - Dev
  - QA
feature_area: im-backend-mvp
purpose: "将 IM 后端长任务的中间计划状态整理为可长期维护的阶段收口视图。"
canonical_path: "docs/plans/PL-003-im-backend-long-task-consolidation-plan.md"
status: accepted
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "../prd/P-001-im-real-time-communication-prd.md"
  - "../prd/P-002-im-realtime-delivery-reliability-prd.md"
  - "../prd/P-003-friend-discovery-and-relationship-prd.md"
  - "../prd/P-004-group-chat-management-prd.md"
  - "../prd/P-005-message-media-and-rich-types-prd.md"
  - "../prd/P-006-read-receipt-and-message-state-prd.md"
  - "../prd/P-007-qzone-like-moments-feed-prd.md"
  - "../prd/P-008-im-notification-center-prd.md"
  - "../prd/P-009-websocket-runtime-contract-prd.md"
  - "../../openspec/specs"
outputs:
  - "IM 后端长任务阶段收口视图"
triggers:
  - "检查 IM 后端 MVP 当前完成度"
  - "继续拆分后续 OpenSpec change"
downstream:
  - "../acceptance/A-015-im-backend-long-task-acceptance-summary.md"
---

# IM 后端长任务阶段收口计划

## 1. 背景

本文件用于替代根工作区 `task_plan.md`、`progress.md`、`findings.md` 中可长期复用的中间状态信息。根工作区文件仍作为本地执行记忆使用，正式项目结论以 `docs/`、OpenSpec specs 和已归档 changes 为准。

根据 `docs/` 目录规范，本文只保留阶段边界、完成状态、验收入口和后续可执行方向，不收录逐条工具调用、临时排查记录或一次性执行流水。

## 2. 目标

1. 给 PM、Dev、QA 提供 IM 后端 MVP 当前完成度的单一阅读入口。
2. 将已完成的 OpenSpec/TDD 长任务按能力域归档到长期阶段视图。
3. 明确后续工作应继续按“一个 change 一个可验收切片”的方式执行。

## 3. 非目标

1. 不替代 OpenSpec archived changes 中的完整 proposal、tasks 和 spec delta。
2. 不重复记录每次 Maven/OpenSpec 命令的完整输出。
3. 不承诺 QQ-like IM 的所有高级功能已经完成，例如音视频、复杂表情商城、公开广场或空间装扮。

## 4. 阶段收口状态

| 阶段 | 能力域 | 收口结果 | 主要验收入口 |
| --- | --- | --- | --- |
| Phase 1-5 | 调研、PRD、规范 | 已完成后端项目调研、PRD 拆分、计划漏洞复审和 TDD/高可用规范固化 | `docs/prd/`、`AGENTS.md` |
| Phase 6-8 | WebSocket 与实时可靠性 | 已完成 WebSocket 运行契约、房间成员缓存缺失兜底、重连消息补偿游标 | `openspec/specs/websocket-runtime-contract`、`chat-realtime-delivery` |
| Phase 9 | 好友发现与关系 | 已完成好友搜索、关系状态、删除好友幂等 | `docs/acceptance/A-003-friend-discovery-phase9-acceptance.md` |
| Phase 10-11 | 群资料与成员治理 | 已完成群资料更新、群主移除普通成员和权限边界 | `openspec/specs/chat-room-access/spec.md` |
| Phase 12-13 | 富消息与已读摘要 | 已完成引用回复预览、跨房间防泄露、发送者已读/未读聚合统计 | `openspec/specs/chat-message/spec.md` |
| Phase 14-15 | 动态 feed | 已完成动态发布、好友可见列表、作者删除、点赞、取消点赞、评论、互动通知降级 | `docs/acceptance/A-004-moments-feed-foundation-acceptance.md`、`A-005` |
| Phase 16-17R | 通知中心接入 | 已完成好友申请通知、群邀请通知和群邀请 afterCommit 事务边界 | `docs/acceptance/A-006-friend-notification-center-acceptance.md`、`A-007` |
| Phase 18 | 图片/文件消息契约 | 已加固 IMAGE/FILE extra 的 URL、名称、扩展名、宽高和大小边界 | `docs/acceptance/A-008-message-media-extra-contract-acceptance.md` |
| Phase 19-22 | 事实优先与推送降级 | 已完成群成员加入幂等、群解散、退群、会话操作、消息发送、已读、撤回的推送失败降级 | `docs/acceptance/A-009` 到 `A-012` |
| Phase 23-24 | 好友申请可靠性 | 已完成好友申请/通过推送失败降级，以及好友通知 afterCommit 事务边界 | `docs/acceptance/A-013`、`A-014` |

## 5. 当前完成度

截至 2026-05-20，当前计划内 Phase 1-24 均已完成、归档并推送到 `origin/main`。后端 OpenSpec 当前无 active change，`openspec validate --all --strict` 为 9 项通过。

当前后端 MVP 已具备以下最小闭环：

1. 好友发现、申请、通过、通知和私聊房间初始化。
2. 群聊创建、邀请、资料更新、成员移除、退群和解散。
3. 会话列表、置顶、删除、未读数、读边界和消息游标。
4. 文本、图片、文件、引用回复、撤回和已读摘要。
5. WebSocket 运行契约、在线状态、实时推送和关键推送失败降级。
6. 动态 feed 的发布、好友可见、删除、点赞、评论和互动通知。

## 6. 后续执行方向

后续仍应继续遵循 MVP 与不过度设计原则，优先拆成小型 OpenSpec change：

1. 补齐真实集成环境的 WebSocket 连接、断线重连和多实例推送验收。
2. 补齐端侧对后端新增契约的联调验收，尤其是动态、已读摘要和好友通知。
3. 按用户确认后再拆语音、视频、表情、转发、公开动态广场等高级 QQ-like 功能。
4. 管理后台治理能力应单独拆分，不与聊天主链路混在一个 change 中。

## 7. 验收门禁

1. OpenSpec 无 active change。
2. `openspec validate --all --strict` 通过。
3. `mallchat-chat-service` 回归测试通过。
4. 相关验收文档位于 `docs/acceptance/`，并在 README 中可检索。
5. GitHub `main` 与本地 `HEAD` 同步。

## 8. 风险与边界

1. 当前验证以单元测试和 OpenSpec 为主，未替代完整微服务联调、真实 WebSocket 连接压测或端到端 UI 验收。
2. 部分高级 IM 能力仍属于后续确认范围，不应从当前完成状态推断为已经实现。
3. 后续如果修改好友、群、消息、动态或通知主链路，需要继续先写失败测试再实现。

## 9. 待确认问题

1. 是否需要把移动端 Taro/UniApp/Flutter 的联调验收纳入下一轮 OpenSpec。
2. 是否优先做真实 WebSocket 多实例环境验收，还是先做端侧 UI/接口联调。
3. 高级消息类型的优先级是否仍按语音、视频、表情、转发拆分。

## 10. 变更记录

| 日期 | 作者 | 版本 | 变更说明 |
| --- | --- | --- | --- |
| 2026-05-20 | StephenQiu30 | 0.1.0 | 将 IM 后端长任务中间状态整理为正式阶段收口计划 |
