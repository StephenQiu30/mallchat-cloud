# Change: harden-group-dismiss-push-degradation

## Summary

让群主解散群聊时的会话删除推送具备降级能力：单个成员的 `sendSessionDelete` 失败不应回滚成员删除、会话删除、群资料删除和房间删除等核心业务事实。

## Motivation

`removeMember` 已经把会话删除推送视为实时通知，推送失败只记录日志，不回滚成员移除事实。`dismissRoom` 当前在循环成员时直接发送会话删除 MQ，如果某个推送异常会抛出运行时异常并回滚整个解散事务。对于 IM 系统，数据库中的房间、成员和会话事实应优先完成；客户端可通过会话列表重连补偿恢复最终状态。

本变更只收紧群解散的推送失败边界，不新增解散通知中心、审计表、延迟重试或群主转让。

## Scope

- 新增测试锁定群解散时会话删除推送失败不回滚业务事实。
- `dismissRoom` 捕获单个成员 `sendSessionDelete` 异常并记录 warn 日志。
- 保持群主权限、群聊类型校验、成员删除、会话删除、群资料删除和房间删除现有流程。

## Non-Goals

- 不新增解散通知中心记录。
- 不新增 MQ 重试表或补偿任务。
- 不新增群操作审计。
- 不调整群主转让或退群规则。

## Validation

- `openspec validate harden-group-dismiss-push-degradation --strict`
- `mvn -pl :mallchat-chat-service -am test -Dtest=ChatRoomServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
- `mvn -pl :mallchat-chat-service -am test`
- `openspec validate --all --strict`
