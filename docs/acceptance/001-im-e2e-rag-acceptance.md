---
layer: Acceptance
doc_no: "001"
audience:
  - PM
  - Dev
  - QA
  - Ops
feature_area: im-system
purpose: "定义 MallChat IM 生产化增强的 TDD、E2E、RAG 验收门禁和长期回归边界。"
canonical_path: "docs/acceptance/001-im-e2e-rag-acceptance.md"
status: draft
version: "0.2.0"
owner: "StephenQiu30"
inputs:
  - "docs/prd/001-im-system-srd.md"
  - "docs/design/D-001-im-production-architecture.md"
  - "docs/plans/001-im-production-task-orchestration-plan.md"
outputs:
  - "GitHub Issue 验收说明"
  - "Pull Request Test-first Evidence"
  - "CI focused tests"
triggers:
  - "IM feature 进入开发、评审、合并或发布前"
downstream:
  - "GitHub Issue"
  - "Pull Request"
  - ".github/workflows/ci.yml"
---

# IM E2E 与 RAG 验收

## 1. 背景

IM 生产化不能只用编译通过作为验收。消息、好友、群聊、朋友圈和实时投递都需要先用测试定义行为，再用 E2E 或等价可执行脚本验证真实业务链路。

本文档是 IM RAG 验收看板，用于维护 feature RAG 状态、红绿测试、E2E 场景和残余风险。每个 P0 feature 必须关联至少一个 E2E 或等价验收场景。

## 2. RAG 定义

| 状态 | 含义 | 处理要求 |
| --- | --- | --- |
| Red | 核心链路失败、无测试、无 E2E 或存在生产阻塞风险 | 不得进入验收，必须先修复或拆分 |
| Amber | 核心链路可用，但失败补偿、观测、缓存退化或边界覆盖不足 | 可延期，但必须记录风险、补偿和后续 Issue |
| Green | TDD、E2E、focused tests、仓库门禁均有证据 | 可进入评审或生产化验收 |

## 3. TDD 门禁

每个 feature 必须记录：

1. 红灯测试名称和命令。
2. 红灯失败原因。
3. 绿灯实现范围。
4. 绿灯验证命令。
5. 是否发生重构。
6. 重构后验证命令。
7. 无法测试优先时的原因和替代可执行验证。

## 4. E2E 主场景

### 4.1 私聊消息可靠性

1. 准备用户 A 和用户 B。
2. A 发起好友申请。
3. B 通过好友申请。
4. A/B 创建或获取私聊房间。
5. A 使用 `clientMsgId` 发送文本消息。
6. 查询消息历史，验证只生成一条 `chat_message`。
7. A 使用同一 `clientMsgId` 重复发送。
8. 验证不重复落库、不重复增加 B 未读。
9. 验证 B 的 `chat_session.last_message_id` 正确。
10. 模拟 MQ/WebSocket 失败，验证消息事实不回滚。
11. B 离线后重新拉取历史，验证消息不丢。

### 4.2 好友权限

1. 非好友发送私聊应失败。
2. 好友关系建立后发送应成功。
3. 拉黑后发送应失败。
4. 缓存清空后权限判断仍以数据库事实为准。

### 4.3 群聊权限

1. 非成员发送群消息应失败。
2. 成员发送群消息应成功。
3. 退出或被踢后发送应失败。
4. 群成员变化不得破坏历史消息事实。

### 4.4 朋友圈

1. 用户发布公开动态后，其他用户可见。
2. 用户发布好友可见动态后，非好友不可见。
3. 点赞重复请求只产生一条事实。
4. 评论写入独立事实表，不复用 `chat_message`。

## 5. Feature RAG 表

每个 P0 feature 必须关联至少一个 E2E 场景（见第 4 节）。

| Feature | 当前状态 | 目标状态 | 必需证据 | E2E 场景引用 |
| --- | --- | --- | --- | --- |
| `im-e2e-test-harness` | Red | Green | E2E smoke 可执行 | N/A（基础设施） |
| `message-send-idempotency` | Red | Green | 重复发送红绿测试 + 私聊 E2E | 4.1 私聊消息可靠性 |
| `message-delivery-reliability` | Red | Green | MQ/WebSocket 失败测试 + 历史可查 E2E | 4.1 私聊消息可靠性 |
| `session-consistency` | Red | Green | 未读/lastMessage 重复乱序测试 + 会话 E2E | 4.1 私聊消息可靠性 |
| `message-recovery-observability` | Amber | Green | 恢复脚本 + focused tests + 日志指标 | 4.1 私聊消息可靠性 |
| `friend-apply-lifecycle` | Amber | Green | 好友申请生命周期 E2E | 4.2 好友权限 |
| `friend-message-permission` | Amber | Green | 非好友/拉黑权限测试 | 4.2 好友权限 |
| `group-message-permission` | Amber | Green | 非成员/退出后权限测试 | 4.3 群聊权限 |
| `moment-like-comment-idempotency` | Amber | Green | 点赞评论幂等测试 | 4.4 朋友圈 |

## 6. 通用验证命令

每个 PR 至少选择与改动范围匹配的命令：

```bash
bash scripts/validate-repository.sh
mvn -pl mallchat-service/mallchat-chat-service -Dtest=<FocusedTest> test
mvn -pl mallchat-common/mallchat-common-websocket -Dtest=<FocusedTest> test
mvn -pl mallchat-common/mallchat-common-rabbitmq -Dtest=<FocusedTest> test
mvn -B -DskipTests compile
```

如果新增 E2E 脚本，应在本文档和 PR 中补充具体命令。

## 7. 代码风格一致性验收

每个 feature PR 必须回答：

1. 是否复用现有 Controller、Service、Mapper、Entity、Convert、DTO/VO 分层。
2. 是否避免新增平行架构。
3. 是否保持 `chat-*` / `Chat*` 命名。
4. 是否使用现有异常和响应风格。
5. 是否将跨服务契约放入 `mallchat-api-*`。
6. 是否通过对应契约守护测试或 focused tests。
7. 是否只维护 `sql/mallchat.sql` 作为数据库结构事实源。

## 8. 发布前验收门禁

1. 所有 P0 feature 达到 Green，或 Amber 项有明确延期 Issue。
2. Red 项不得合并到生产化目标分支。
3. PR 描述包含 Test-first Evidence、Commands run、Result、RAG 状态和残余风险。
4. `bash scripts/validate-repository.sh` 通过。
5. 相关 focused tests 通过。
6. E2E smoke 通过或记录不可执行原因和替代证据。

## 9. 风险与边界

1. 首批 E2E 可能先覆盖后端服务和替身投递，不强制端侧 UI。
2. MQ 自动重投如果本阶段不做，必须保持 Amber 并创建后续 Issue。
3. WebSocket 真实连接 E2E 如果不稳定，先用契约测试守住投递边界。

## 10. 变更记录

| 日期 | 作者 | 版本 | 变更说明 |
| --- | --- | --- | --- |
| 2026-06-05 | StephenQiu30 | 0.1.0 | 初始化 IM E2E 与 RAG 验收文档 |
| 2026-06-06 | StephenQiu30 | 0.2.0 | Rework: RAG 表增加 E2E 场景引用列，明确每个 feature 关联的 E2E 场景 |
