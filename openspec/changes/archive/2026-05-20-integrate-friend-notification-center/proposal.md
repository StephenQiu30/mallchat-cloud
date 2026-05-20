## Why

QQ-like IM 中，好友申请和通过既需要实时提醒，也需要在通知中心留下可回看、可计数、可标记已读的展示事实。当前 `UserFriendApplyServiceImpl` 已经在申请和通过时发送 WebSocket 事件，但没有创建 notification-service 记录，导致用户离线或错过实时事件后，通知中心无法补齐该类社交提醒。

## What Changes

- 好友申请成功后，为被申请人创建一条通知中心记录。
- 好友通过成功后，为申请人创建一条通知中心记录。
- 保留现有 `sendFriendApply` / `sendFriendApprove` WebSocket 事件和 `user_friend_apply` 业务事实。
- 通知中心创建失败时只降级记录日志，不回滚好友申请、好友关系或私聊房间创建。

## Non-Goals

- 不新增通知类型枚举，好友类提醒复用现有 `user` 类型。
- 不改造好友申请表结构、WebSocket 事件格式或前端跳转协议。
- 不实现群邀请、群审核或系统广播的通知中心接入。
- 不引入事务同步、异步队列、outbox 或额外补偿表。

## Impact

- `mallchat-chat-service`：好友申请服务新增 notification-service Feign 调用和降级测试。
- `mallchat-notification-service`：复用已有业务通知创建接口，不改运行时代码。
- OpenSpec：补充好友申请通知中心持久记录契约。
