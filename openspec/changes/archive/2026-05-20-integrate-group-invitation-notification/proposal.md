## Why

P-004 已定义群聊创建与邀请是 QQ-like IM 的基础能力，P-008 要求群通知逐步进入统一通知中心。当前 `ChatRoomServiceImpl` 在创建群和邀请成员时已经直接添加群成员、更新会话并推送 session update，但被邀请人离线或错过实时事件后，通知中心没有可回看的群邀请提醒。

## What Changes

- 创建群聊时，为初始受邀成员创建通知中心记录。
- 已有群聊邀请好友入群时，为新增成员创建通知中心记录。
- 通知使用现有 `user` 类型，`relatedType=chat_room`，`relatedId=roomId`。
- 通知失败只降级记录日志，不回滚群成员、会话或已有 session update 事实。

## Non-Goals

- 不实现群邀请待确认、入群审核或群通知页。
- 不新增群邀请表、通知类型枚举或 MQ 事件格式。
- 不改变现有“邀请好友即直接入群”的 MVP 流程。
- 不处理群解散、踢人、群公告变更通知。

## Impact

- `mallchat-chat-service`：群聊服务新增 notification-service Feign 调用和降级测试。
- `mallchat-notification-service`：复用已有业务通知创建接口，不改运行时代码。
- OpenSpec：补充群邀请通知中心持久记录契约。
