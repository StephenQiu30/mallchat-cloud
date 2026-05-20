# Change: harden-message-flow-push-degradation

## Summary

让消息发送、已读上报、消息撤回这三类消息主链路操作在实时推送失败时降级处理，避免 MQ/WebSocket 抖动回滚已经写入的消息、已读边界或撤回状态。

## Motivation

MallChat 当前已经逐步采用“业务事实优先，实时推送失败只记录日志”的后端风格。退群、会话操作、群资料更新、群解散等路径已对齐该边界，但 `ChatMessageServiceImpl` 中仍有消息发送、已读上报、消息撤回后直接调用 MQ 的路径。对于 IM 系统，`chat_message`、`chat_room_member.last_read_message_id` 和会话未读事实才是重连后的权威事实；实时推送失败不应让客户端操作在服务端表现为失败。

本变更只补齐降级边界，不新增 outbox、MQ 重试表、离线补偿、新通知或新接口。

## Scope

- 消息落库成功后，聊天消息实时推送失败不回滚消息事实。
- 已读边界和会话未读数更新成功后，已读事件推送或会话刷新推送失败不回滚已读事实。
- 消息撤回状态更新成功后，撤回事件推送或成员会话刷新推送失败不回滚撤回事实。
- 保持现有接口、权限校验、bizId 和数据写入流程不变。

## Non-Goals

- 不新增 outbox、MQ 重试表或定时补偿任务。
- 不改变客户端重连补偿接口。
- 不新增送达回执、离线推送或通知中心记录。
- 不改变消息发送、已读、撤回的现有 HTTP API。

## Validation

- `openspec validate harden-message-flow-push-degradation --strict`
- `mvn -pl :mallchat-chat-service -am test -Dtest=ChatMessageServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
- `mvn -pl :mallchat-chat-service -am test`
- `openspec validate --all --strict`
