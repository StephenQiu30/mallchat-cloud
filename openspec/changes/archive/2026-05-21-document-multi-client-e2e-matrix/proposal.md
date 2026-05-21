## Why

MallChat 的上线验收需要覆盖 Taro、UniApp、Flutter 和 Admin，但 P2 当前仍是“先后端，后多端”。在端侧自动化实现前，需要先沉淀一份可执行的多端 E2E 验收矩阵，明确登录、好友、群聊、消息、动态、通知和后台审计的最小验收边界。

## What Changes

- 新增多端 E2E 矩阵规格，定义各端必须覆盖的核心 IM 流程和证据类型。
- 新增长期 docs 验收文档，作为后续 Taro、UniApp、Flutter、Admin 自动化的共同基线。
- 不在本 change 中实现端侧测试脚本或页面。

## Non-Goals

- 不实现 Taro、UniApp、Flutter 或 Admin 自动化脚本。
- 不引入新的 E2E 框架选型结论。
- 不把多端页面开发并入 m10 后端 PR。
