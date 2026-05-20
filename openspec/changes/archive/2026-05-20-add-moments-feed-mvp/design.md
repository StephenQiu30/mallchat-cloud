## Context

OpenIM、Rocket.Chat、Mattermost 等开源 IM 系统都把聊天事实、会话入口、通知入口区分开来；Matrix 的事件模型也强调服务端需要校验事件结构和权限。MallChat 当前已经用 `chat_room/chat_message/chat_session` 承接聊天，但动态 feed 的权限与生命周期不同，不能复用房间消息表。

## Goals

1. 动态 feed 有独立事实表和接口。
2. 本 change 只完成动态 MVP 的基础切片：发布、好友可见列表、作者删除。
3. 作者自己始终可见自己的动态。
4. 删除好友后，好友流不再返回对方动态。
5. 所有接口先写失败测试，再写实现。
6. 完整动态 MVP 还需要后续 change 完成点赞、评论和互动通知。

## Decisions

### Dynamic Domain Naming

使用 `ChatMoment*` 命名，保持现有 `Chat*` 代码风格一致：

- `ChatMoment`
- `ChatMomentMedia`
- `ChatMomentController`
- `ChatMomentService`
- `ChatMomentServiceImpl`

### Visibility

首版使用好友关系判断可见性：

- `viewerId == authorId`：可见。
- 互为好友：可见。
- 非好友：不可见。

列表查询不得“先分页后过滤”。实现时应先得到可见作者集合（本人 + 好友 ID），再按 `user_id IN (...)`、`status=0`、`is_delete=0`、`create_time desc` 分页。若现有 `UserFriendService` 只有 `listFriendIdsForNotification`，应新增中性方法 `listMutualFriendIds(Long userId)`，避免动态域依赖 notification 命名。

### Data Source

`chat_moment` 是动态主体事实源，`chat_moment_media` 是图片事实源。点赞和评论后续单独建表，不在本 change 实现。

发布动态必须在事务内同时保存主体和媒体。若媒体保存失败，主体写入也要回滚，避免 `media_count` 与媒体表不一致。

### Input Boundary

MVP 输入边界：

- `content` trim 后最大 1000 个字符。
- `content` trim 后为空且 media 为空时拒绝。
- `media` 最多 9 条。
- `media.url` 必填，长度不超过 1024。
- 本 change 仅校验 URL 非空与长度，不校验文件归属；文件服务归属校验后续单独补齐。

### Delete Idempotency

删除规则：

- 不存在的 `momentId` 返回目标不存在。
- 已存在且未删除，作者本人可删除。
- 已存在且已删除，作者本人重复删除返回成功。
- 已存在但非作者删除应拒绝，不通过“已删除/不存在”暴露资源归属。

### Notification

本 change 不接入 notification-service。后续点赞/评论接入通知时，通知失败不得回滚动态互动事实。

## Alternatives Considered

### Reuse chat_message

拒绝。动态不是房间内消息，会污染会话未读、撤回、历史分页和房间成员权限。

### Add a new moments microservice

拒绝。当前目标是不增加部署复杂度，且动态 MVP 依赖好友关系，放在 `mallchat-chat-service` 内更贴近现有边界。

### Implement likes/comments together

暂缓。发布、列表、删除先形成最小闭环，点赞/评论/通知作为下一个 OpenSpec change，降低一次性变更风险。

## Test Strategy

- `ChatMomentServiceImplTest` 使用 service-first 的单元测试风格，和现有 `UserFriendServiceImplTest`、`ChatRoomServiceImplTest` 保持一致。
- 红灯测试先覆盖缺失类型/方法，再实现最小代码。
- 每个接口覆盖成功、参数错误、越权、目标不存在和权限过滤。

## Rollout

1. 创建 OpenSpec change。
2. 更新 docs 和 Superpowers 计划。
3. 先提交红灯测试，再提交最小实现。
4. 跑 chat-service 回归和 OpenSpec 全量校验。
5. 归档 change 后进入点赞/评论/通知 change。
