# Change: harden-friend-apply-push-degradation

## Summary

让好友申请和好友通过在 WebSocket/MQ 推送失败时降级处理，避免实时提醒通道抖动回滚已经写入的好友申请、好友关系或私聊房间事实。

## Motivation

MallChat 已经在会话、群聊和消息主链路中采用“业务事实优先，实时推送失败只记录日志”的后端风格。`UserFriendApplyServiceImpl` 当前已经对通知中心写入失败做了降级，但好友申请和好友通过仍直接调用 `chatMqProducer.sendFriendApply` / `sendFriendApprove`。如果 MQ 或 WebSocket 推送异常，已经保存的申请、好友关系和私聊房间初始化可能被调用方视为失败，和 IM 主链路的可靠性边界不一致。

本变更只补齐好友申请/通过推送失败降级，不新增 outbox、重试表、离线补偿、审批模型或新通知通道。

## Scope

- 好友申请记录保存成功后，好友申请 WebSocket/MQ 推送失败不回滚申请事实。
- 好友申请推送失败后，仍继续尝试写入通知中心记录。
- 好友通过时，好友关系和私聊房间初始化完成后，好友通过 WebSocket/MQ 推送失败不回滚业务事实。
- 好友通过推送失败后，仍继续尝试写入通知中心记录。
- 保持现有接口、权限校验、bizId 和数据写入流程不变。

## Non-Goals

- 不新增 outbox、MQ 重试表或定时补偿任务。
- 不新增好友申请审批模型或群通知模型。
- 不改变好友申请/通过的 HTTP API。
- 不改变 notification-service 的幂等规则或实时通知实现。

## Validation

- `openspec validate harden-friend-apply-push-degradation --strict`
- `mvn -pl :mallchat-chat-service -am test -Dtest=UserFriendApplyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
- `mvn -pl :mallchat-chat-service -am test`
- `openspec validate --all --strict`
