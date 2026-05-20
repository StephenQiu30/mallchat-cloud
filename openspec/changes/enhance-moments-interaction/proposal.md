## Why

P-007 和 `add-moments-feed-mvp` 已完成动态发布、好友可见列表和作者删除，但 QQ-like IM 的动态体验仍缺少点赞、评论和互动通知。没有互动事实表时，前端只能展示静态内容；没有通知挂接时，动态互动无法进入现有通知中心。

本次变更补齐动态 MVP 的互动闭环：用户只能对自己可见的动态点赞和评论，点赞保持幂等，取消点赞保持幂等，评论保存后可分页查询，并复用现有 notification 能力向动态作者发送点赞/评论通知。通知失败不应回滚已经落库的互动事实。

## What Changes

- 新增动态点赞：
  - `POST /chat/moment/like?id={momentId}` 幂等点赞。
  - `DELETE /chat/moment/like?id={momentId}` 幂等取消点赞。
  - 点赞数仅在事实状态真正变化时增减。
- 新增动态评论：
  - `POST /chat/moment/comment` 创建一级评论。
  - `GET /chat/moment/comment/list?momentId={momentId}` 分页查询未删除评论。
  - 评论正文 trim 后不能为空，长度限制为 500。
  - 评论数仅在评论创建成功时增加。
- 新增互动通知：
  - 好友点赞或评论他人动态时，向动态作者创建 `like/comment` 类型通知。
  - 自己互动自己的动态不创建通知。
  - 通知使用 `bizId` 保持幂等，并关联 `relatedType=chat_moment`。
  - 通知失败只记录错误，不回滚点赞/评论事实。

## Capabilities

### Modified Capabilities

- `moments-feed`: 增加点赞、取消点赞、评论、评论列表和互动通知契约。
- `im-product-mvp`: 将动态 MVP 完成条件更新为基础动态 + 互动动态均完成。

## Impact

- 代码：
  - `ChatMomentController`
  - `ChatMomentService`
  - `ChatMomentServiceImpl`
  - `NotificationFeignClient`
  - 新增 `ChatMomentLike`、`ChatMomentComment` 实体和 mapper
  - 新增动态评论 DTO/VO
  - 更新 `sql/mallchat.sql`
- 测试：
  - `ChatMomentServiceImplTest`
- 非目标：
  - 不实现嵌套评论、评论点赞、公开广场、推荐流、空间装扮、访客记录。
  - 不新增动态实时 WebSocket 事件；通知中心负责提醒展示。
