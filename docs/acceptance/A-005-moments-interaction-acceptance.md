---
layer: Acceptance
doc_no: "A-005"
audience:
  - PM
  - Dev
  - QA
feature_area: moments-interaction
purpose: "记录动态互动与通知闭环的验收结果、测试证据与剩余风险。"
canonical_path: "docs/acceptance/A-005-moments-interaction-acceptance.md"
status: complete
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "../prd/P-007-qzone-like-moments-feed-prd.md"
  - "../acceptance/A-004-moments-feed-foundation-acceptance.md"
  - "../../openspec/changes/archive/2026-05-20-enhance-moments-interaction"
outputs:
  - "动态互动与通知闭环验收结论"
triggers:
  - "enhance-moments-interaction 完成后验收"
downstream:
  - "../../openspec/specs/moments-feed/spec.md"
---

# 动态互动与通知闭环验收

## 1. 变更验收范围

- `POST /chat/moment/like`：对自己可见动态幂等点赞。
- `DELETE /chat/moment/like`：对自己可见动态幂等取消点赞。
- `POST /chat/moment/comment`：创建一级评论。
- `GET /chat/moment/comment/list`：分页查询自己可见动态的未删除评论。
- `chat_moment_like` 与 `chat_moment_comment` 表结构。
- 点赞/评论通知复用 notification 服务业务创建入口。

## 2. TDD 证据

| 阶段 | 命令 | 结果 | 结论 |
| --- | --- | --- | --- |
| 红灯 | `mvn -pl :mallchat-chat-service -am test -Dtest=ChatMomentServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false` | 缺少 `ChatMomentCommentRequest`、`ChatMomentCommentVO`、`ChatMomentLike`、`ChatMomentComment` 等契约，编译失败 | 通过 |
| 绿灯 | 同上 | `Tests run: 26, Failures: 0, Errors: 0` | 通过 |
| chat + notification 回归 | `mvn -pl mallchat-service/mallchat-chat-service,mallchat-service/mallchat-notification-service -am test` | `Tests run: 110, Failures: 0, Errors: 0` | 通过 |
| OpenSpec change | `openspec validate enhance-moments-interaction --strict` | change 合法 | 通过 |
| OpenSpec 全量 | `openspec validate --all --strict` | `10 passed, 0 failed` | 通过 |
| OpenSpec 归档后全量 | `openspec validate --all --strict` | `9 passed, 0 failed` | 通过 |

## 3. 结论

本切片完成了动态 MVP 的互动后端闭环：点赞、取消点赞、评论、评论列表和互动通知。点赞使用 `moment_id + user_id` 唯一约束支撑幂等；通知使用独立业务创建 DTO/Feign，不复用管理员批量发布入口；通知调用参考现有 chat-service 推送降级风格，互动事实更新后直接尝试通知，通知失败不回滚互动事实。

## 4. 残余风险

1. 当前只支持一级评论，不支持评论删除、嵌套评论或评论点赞。
2. 当前没有新增动态专属 WebSocket 事件，互动提醒走 notification 中心。
3. Controller 未补未登录 Web 层集成测试，仍依赖现有 `SecurityUtils` 与网关鉴权约束。
4. 计数字段为展示冗余，后续可补管理端重算或巡检脚本。
