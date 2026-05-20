# Change: harden-friend-notification-after-commit

## Summary

让好友申请和好友通过的通知中心写入在本地事务同步存在时延迟到 `afterCommit` 执行，避免本地事务回滚后留下跨服务孤儿通知。

## Motivation

群邀请通知已经采用 `afterCommit` 发送，确保本地成员和会话事实提交后再创建 notification-service 展示事实。好友申请/通过通知也属于同类跨服务展示事实，但当前 `UserFriendApplyServiceImpl#trySendFriendNotification` 会立即调用 notification-service。尤其是好友通过流程带有本地事务，若通知先创建而后续本地事务失败，会出现通知中心已提示但好友申请/关系事实未提交的逻辑漏洞。

本变更只补齐通知发送时机，不新增 outbox、重试表、事件表或新通知通道。

## Scope

- 当事务同步存在时，好友申请通知注册 `afterCommit` 后再发送。
- 当事务同步存在时，好友通过通知注册 `afterCommit` 后再发送。
- 无事务同步时，保持现有立即发送行为。
- 保持现有通知内容、`bizId`、降级日志和 notification-service 调用不变。

## Non-Goals

- 不新增 outbox、MQ 重试表或定时补偿任务。
- 不改变好友申请/通过 HTTP API。
- 不改变 notification-service 的幂等规则、实时推送或数据结构。
- 不改变好友申请/通过 WebSocket/MQ 事件发送时机。

## Validation

- `openspec validate harden-friend-notification-after-commit --strict`
- `mvn -pl :mallchat-chat-service -am test -Dtest=UserFriendApplyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
- `mvn -pl :mallchat-chat-service -am test`
- `openspec validate --all --strict`
