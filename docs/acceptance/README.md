# Acceptance 文档

本目录存放验收与验证类文档。

## 适合放入

1. 验收标准。
2. 测试记录。
3. 验证报告。
4. 回归证据和残余风险说明。

## 文档清单

| 文档 | 说明 |
| --- | --- |
| [A-001-agents-ci-report.md](./A-001-agents-ci-report.md) | AGENTS 迁移与后端 CI 验收结论 |
| [A-011-session-operation-push-degradation-acceptance.md](./A-011-session-operation-push-degradation-acceptance.md) | 退群、会话置顶、会话删除推送失败降级验收结论 |
| [A-012-message-flow-push-degradation-acceptance.md](./A-012-message-flow-push-degradation-acceptance.md) | 消息发送、已读上报、消息撤回推送失败降级验收结论 |
| [A-013-friend-apply-push-degradation-acceptance.md](./A-013-friend-apply-push-degradation-acceptance.md) | 好友申请、好友通过推送失败降级验收结论 |
| [A-014-friend-notification-after-commit-acceptance.md](./A-014-friend-notification-after-commit-acceptance.md) | 好友申请、好友通过通知 afterCommit 验收结论 |
| [A-015-im-backend-long-task-acceptance-summary.md](./A-015-im-backend-long-task-acceptance-summary.md) | IM 后端长任务最终验收摘要 |
| [A-016-im-production-readiness-issue-generation-acceptance.md](./A-016-im-production-readiness-issue-generation-acceptance.md) | IM 生产可用 P0 Issue 编排验收 |
| [A-017-m3-message-reliability-acceptance.md](./A-017-m3-message-reliability-acceptance.md) | m3 消息可靠性与可恢复 Epic 验收 |
| [A-018-m4-ops-observability-acceptance.md](./A-018-m4-ops-observability-acceptance.md) | m4 可观测性与运维门禁 Epic 验收 |

## 不适合放入

1. 产品需求或执行计划。
2. 架构设计草案。
3. 发布流程说明。
4. 没有验收结论或长期复用价值的临时测试过程记录。

## 命名建议

使用 `序号-主题-验收.md`，例如 `001-agent规范验收.md`。
