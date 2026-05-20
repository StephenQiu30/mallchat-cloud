## Why

MallChat 的 QQ-like IM 基础聊天、好友、群组、实时推送和已读能力已经完成多轮增强，但动态 feed 仍停留在 PRD 层。用户新的目标明确要求“好友聊天、动态、群组”等基础能力至少需要完成，因此动态不能继续作为 P2 延后事项。

当前后端没有 `moment/feed/timeline` 领域表、控制器或服务。notification-service 虽已有 comment/like 类型，但通知不是动态事实来源。若把动态混入 `chat_message`，会污染房间消息、会话未读和撤回语义。

## What Changes

- 新增 `moments-feed` capability，定义动态主体、媒体、好友可见列表和删除自己的动态。
- 新增后端动态事实模型：`chat_moment`、`chat_moment_media`。
- 新增最小 API：
  - `POST /chat/moment/publish`
  - `GET /chat/moment/list`
  - `DELETE /chat/moment/delete`
- 本 change 是动态 MVP 的基础切片，只做发布、好友可见时间线、删除自己的动态。
- 完整动态 MVP 还需要后续 `enhance-moments-interaction` 完成点赞、评论和互动通知；归档本 change 不代表 P-001/P-007 的动态 MVP 全部完成。

## Capabilities

### New Capabilities

- `moments-feed`: 动态主体、媒体、好友可见时间线、作者删除。

### Modified Capabilities

- `im-product-mvp`: 将基础动态 feed 明确纳入 MVP 必选能力。

## Impact

- 代码：
  - `mallchat-api-chat` 新增动态 DTO/VO。
  - `mallchat-chat-service` 新增 `ChatMoment*` controller/service/mapper/entity。
  - `sql/mallchat.sql` 新增动态表。
- 测试：
  - 新增 `ChatMomentServiceImplTest`，按 TDD 覆盖发布、好友可见列表、删除自己的动态、越权与参数错误。
- 文档：
  - `docs/prd/P-001`、`docs/prd/P-007`、`docs/design/D-002`、`docs/superpowers/plans/2026-05-20-moments-feed-mvp.md`。

## Non-Goals

- 不实现公开广场、推荐流、访客记录、空间装扮和相册体系。
- 不实现点赞、评论、评论回复和互动通知，本轮仅为后续互动预留统计字段。
- 不把动态写入 `chat_message`，不产生聊天会话未读。
- 不新增独立动态微服务；首版放在 `mallchat-chat-service` 内保持最小部署复杂度。
- 不修改 `ChatFeignClient`；本 change 暂无跨服务内部调用方。
