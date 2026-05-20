# Change: harden-session-operation-push-degradation

## Summary

让退群、会话置顶、会话删除这三类已落库的会话操作在实时推送失败时降级处理，避免 MQ/WebSocket 抖动回滚或中断业务事实。

## Motivation

MallChat 已经在成员移除、群资料更新、群解散等路径中采用“业务事实优先，实时推送失败只记录日志”的风格。当前 `quitRoom`、`topSession`、`deleteSession` 仍直接调用 `ChatMqProducer`，推送异常会让退群或会话操作失败。对于 IM 系统，`chat_room_member` 和 `chat_session` 才是重连后的权威事实；实时推送只负责在线即时刷新。

本变更只补齐降级边界，不新增重试任务、通知中心、操作审计或新接口。

## Scope

- 退群后会话删除推送失败不回滚成员离开和会话删除事实。
- 会话置顶后会话刷新推送失败不回滚置顶状态。
- 会话删除后会话删除推送失败不回滚会话删除事实。
- 保持现有接口、bizId、权限校验和数据库写入流程不变。

## Non-Goals

- 不新增 MQ 重试表或补偿任务。
- 不新增会话操作通知中心记录。
- 不新增免打扰、置顶排序或多端同步策略。
- 不改变已存在的 HTTP API。

## Validation

- `openspec validate harden-session-operation-push-degradation --strict`
- `mvn -pl :mallchat-chat-service -am test -Dtest=ChatRoomServiceImplTest,ChatSessionServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
- `mvn -pl :mallchat-chat-service -am test`
- `openspec validate --all --strict`
