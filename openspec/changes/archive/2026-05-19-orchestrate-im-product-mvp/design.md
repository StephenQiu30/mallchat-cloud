## Context

当前后端已有 `chat-friend`、`chat-message`、`chat-online-status`、`chat-room-access`、`chat-session` specs，并且代码中存在 ChatRoom、ChatMessage、ChatSession、UserFriend、WebSocket、Notification 等模块。外部参考显示，IM MVP 应优先覆盖会话、好友资料、群聊、消息漫游/未读、最近联系人和多消息类型。

## Design

1. 使用 `im-product-mvp` 作为产品级能力，不替代已有后端 `chat-*` 能力。
2. 后端继续作为接口和领域行为源头，Taro 作为首个视觉还原端。
3. 已读回执、音视频、空间动态发布等增强能力进入 P1/P2，不阻塞 MVP。
4. 所有多端同步任务必须从 Taro 已验证体验拆分单独 OpenSpec change。

## Data and API Boundary

- 不新增表。
- 不新增 API。
- 不改变现有 DTO/VO。
- 后续如需新增接口，必须落到对应 `chat-*` spec，而不是写入产品级 spec。

## Verification

- `openspec validate orchestrate-im-product-mvp --strict`
- `mvn -B -DskipTests compile`
