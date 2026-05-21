## Why

成熟 IM 需要会话级免打扰。当前会话只有置顶和未读数，群消息实时推送会发送给所有成员，无法支持用户关闭某个群聊的强提醒。

## What Changes

- `chat_session` 增加 `mute_status`。
- 会话列表返回免打扰状态。
- 用户可开启/关闭指定会话免打扰。
- 群聊 `CHAT_MESSAGE` 实时推送排除免打扰接收者，但消息事实、重连补偿和未读事实不受影响。
- notification-service 尊重消息中的 `userIds` allowlist，避免 Redis 成员缓存绕过免打扰过滤。

## Non-Goals

- 不新增独立通知偏好中心。
- 不做设备级、时间段或关键词通知规则。
- 不改变 MESSAGE_READ、MESSAGE_RECALL 和 SESSION_UPDATE 的状态同步语义。
