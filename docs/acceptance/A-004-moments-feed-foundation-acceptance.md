---
layer: Acceptance
doc_no: "A-004"
audience:
  - PM
  - Dev
  - QA
feature_area: moments-feed-foundation
purpose: "记录动态 feed 基础切片的验收结果、测试证据与剩余风险。"
canonical_path: "docs/acceptance/A-004-moments-feed-foundation-acceptance.md"
status: complete
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "../prd/P-007-qzone-like-moments-feed-prd.md"
  - "../design/D-002-qq-like-im-mvp-architecture.md"
  - "../superpowers/plans/2026-05-20-moments-feed-mvp.md"
  - "../../openspec/changes/archive/2026-05-20-add-moments-feed-mvp"
outputs:
  - "动态 feed 基础切片验收结论"
triggers:
  - "add-moments-feed-mvp 完成后验收"
downstream:
  - "../../openspec/specs/moments-feed/spec.md"
  - "后续 OpenSpec change: enhance-moments-interaction"
---

# 动态 Feed 基础切片验收

## 1. 变更验收范围

- `POST /chat/moment/publish`：发布文字或图片动态。
- `GET /chat/moment/list`：按“本人 + 好友”可见作者集合分页返回动态。
- `DELETE /chat/moment/delete`：作者删除自己的动态，重复删除保持幂等。
- `chat_moment` 与 `chat_moment_media` 表结构。
- 动态 feed 与 `chat_message`、`chat_session`、`notification` 的边界。

## 2. TDD 证据

| 阶段 | 命令 | 结果 | 结论 |
| --- | --- | --- | --- |
| 红灯 1 | `mvn -pl :mallchat-chat-service -am test -Dtest=ChatMomentServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false` | 缺少 `ChatMoment*` 类型，编译失败 | 通过 |
| 红灯 2 | 同上 | 12 个测试中 11 个业务断言失败 | 通过 |
| 绿灯 | 同上 | `Tests run: 12, Failures: 0, Errors: 0` | 通过 |
| chat-service 回归 | `mvn -pl :mallchat-chat-service -am test` | `Tests run: 96, Failures: 0, Errors: 0` | 通过 |
| OpenSpec 全量 | `openspec validate --all --strict` | `9 passed, 0 failed` | 通过 |

## 3. 结论

本切片完成了动态 feed 的基础后端闭环：发布、好友可见列表和作者删除。实现保持 `ChatMoment*` 命名，未复用 `chat_message`，不会创建聊天消息或会话未读；列表查询先确定可见作者集合再分页，避免先分页后过滤。

## 4. 残余风险

1. 本切片不是完整动态 MVP，点赞、评论和互动通知仍需后续 `enhance-moments-interaction`。
2. 当前测试以 service-first 为主，未补 controller 鉴权集成测试。
3. 媒体 URL 只校验非空和长度，尚未校验文件服务归属。
4. 动态作者用户资料、头像和名称尚未聚合到 VO，前端可先按用户 ID 展示或后续补齐。

## 5. 后续建议

1. 下一阶段优先做 `enhance-moments-interaction`：点赞、评论、互动通知，通知失败不回滚互动事实。
2. 补动态列表 controller 层未登录、非法分页和参数错误测试。
3. 前端 Taro 动态页接入真实 `chat/moment` API。
