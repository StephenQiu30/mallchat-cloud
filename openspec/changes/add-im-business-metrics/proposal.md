## Why

MallChat 生产运行需要回答关键 IM 行为的业务量和失败情况，例如消息发送、好友申请、动态互动。现有 m3 已覆盖 MQ 和推送结果，但业务入口本身缺少统一的最小计数。

## What Changes

- 在 chat-service 增加轻量业务指标记录器。
- 记录消息发送、好友申请、动态点赞和动态评论的结果计数。
- 使用 Micrometer `MeterRegistry`，不绑定具体监控平台。
- 增加测试覆盖指标记录和关键服务入口调用。

## Non-Goals

- 不新增指标中台或复杂埋点框架。
- 不把 userId、roomId、momentId 等高基数字段作为 metric tag。
- 不改变现有业务成功/失败语义。
