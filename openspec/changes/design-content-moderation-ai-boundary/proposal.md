# design-content-moderation-ai-boundary

## Why

P2 需要为消息、动态和文件内容治理预留清晰边界，但当前阶段不应绑定外部 AI 厂商或引入独立审核平台。先把动态发现所需的审核状态契约落稳，后续再按风险逐步扩展。

## What Changes

- 为 `chat_moment` 增加最小审核状态字段。
- 明确业务写入不依赖外部 AI 调用，默认走通过状态。
- 公开广场与互动入口统一过滤审核未通过动态。
- 记录后续消息/文件审核扩展边界。

## Non-Goals

- 不接入具体 AI 厂商。
- 不实现异步审核队列、人工审核后台或敏感词引擎。
- 不改变 `chat_message` 和 `file_upload_record` 表结构。

## Linked Issues

- #42
- #45
