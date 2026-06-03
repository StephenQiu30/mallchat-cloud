---
layer: PRD
doc_no: "P-IM-003"
audience:
  - PM
  - Dev
  - QA
feature_area: message-delivery
purpose: "定义消息投递可靠性和体验增强的产品边界、核心用户故事和验收标准。"
canonical_path: "docs/prd/P-IM-003-message-delivery-prd.md"
status: active
version: "1.0.0"
owner: "StephenQiu30"
inputs:
  - "MallChat IM 产品规划"
outputs:
  - "消息投递增强 PRD，驱动 Plan、实现和验收"
triggers:
  - "消息投递可靠性需求变更或新阶段规划时"
downstream:
  - "docs/plans/PL-IM-003-message-delivery-plan.md"
---

# 消息投递可靠性与体验增强 PRD

## 1. 背景

MallChat IM 已具备基础消息收发能力，但在生产场景中存在以下可靠性风险：
- 重复发送导致消息重复落库
- 推送失败后消息状态不一致
- 离线重连后消息丢失
- 撤回缺乏权限和时间窗口控制
- 富消息类型枚举不完整

## 2. 目标

```gherkin
Given 用户 A 向房间发送消息，clientMsgId 重复
When 服务端收到重复请求
Then 返回已有消息，不重复落库

Given 用户 A 在 2 分钟内撤回自己发送的消息
When 服务端收到撤回请求
Then 消息状态变为已撤回，通知房间成员

Given 用户 A 在 2 分钟后尝试撤回消息
When 服务端收到撤回请求
Then 返回撤回超时错误

Given 用户 A 尝试撤回用户 B 的消息
When 服务端收到撤回请求
Then 返回无权限错误

Given 推送失败
When 消息已落库
Then 消息事实不被破坏，投递状态标记为 FAILED
```

### 2.1 核心用户故事

1. **幂等发送**：相同 `clientMsgId` 不重复落库，返回已有消息。
2. **投递状态跟踪**：消息从 PENDING -> DELIVERED/FAILED 全链路可追踪。
3. **离线补偿**：重连后通过游标拉取未接收消息。
4. **消息撤回**：发送者可在 2 分钟内撤回自己的消息，超时或非本人返回错误。
5. **消息回复**：回复消息需引用原消息 ID，校验原消息存在性。
6. **消息转发**：转发消息携带新 `clientMsgId`，保持幂等。
7. **富消息**：图片/文件/语音/视频/表情类型完整覆盖。

### 2.2 幂等与安全

- `clientMsgId` 全局唯一索引，重复请求返回已有消息。
- 撤回权限：仅发送者可撤回，2 分钟时间窗口。
- 回复引用：原消息必须存在且属于同一房间。
- 投递失败不回滚消息事实。

## 3. 非目标

- 消息加密（端到端加密）。
- 消息已读回执增强（已有基础实现）。
- 消息撤回后管理员可见（后续迭代）。
- 批量撤回。

## 4. 核心内容

### 4.1 数据模型

| 表名 | 说明 | 关键字段 |
| --- | --- | --- |
| `chat_message` | 消息主表（已有） | `id`, `room_id`, `from_user_id`, `client_msg_id`, `content`, `type`, `status`, `reply_msg_id` |
| `chat_message_delivery` | 消息投递状态跟踪 | `id`, `message_id`, `user_id`, `status`（PENDING/DELIVERED/FAILED）, `retry_count`, `last_retry_at` |
| `chat_message_revoke` | 消息撤回记录 | `id`, `message_id`, `revoker_id`, `revoked_at`, `reason` |

### 4.2 API 接口

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/chat/message/send` | POST | 发送消息（已有，增强幂等） |
| `/chat/message/recall` | POST | 撤回消息（已有，增强权限/时间窗口） |
| `/chat/message/forward` | POST | 转发消息（已有） |
| `/chat/message/delivery/status` | GET | 查询投递状态（新增） |

### 4.3 错误码

| 错误码 | 说明 |
| --- | --- |
| `MESSAGE_NOT_FOUND` | 消息不存在 |
| `MESSAGE_DUPLICATE` | 重复消息（幂等） |
| `MESSAGE_REVOKE_TIMEOUT` | 撤回超时（超过 2 分钟） |
| `MESSAGE_REVOKE_NO_permission` | 无权限撤回 |
| `MESSAGE_REPLY_NOT_FOUND` | 被回复消息不存在 |
| `MESSAGE_DELIVERY_FAILED` | 消息投递失败 |

### 4.4 枚举

| 枚举 | 值 |
| --- | --- |
| `MessageDeliveryStatusEnum` | PENDING(0), DELIVERED(1), FAILED(2) |

## 5. 验收标准

1. 重复 `clientMsgId` 发送返回已有消息，数据库记录数不变。
2. 撤回请求在 2 分钟内成功，超时返回 `MESSAGE_REVOKE_TIMEOUT`。
3. 非发送者撤回返回 `MESSAGE_REVOKE_NO_permission`。
4. 回复不存在的消息返回 `MESSAGE_REPLY_NOT_FOUND`。
5. 转发消息使用新 `clientMsgId`，幂等。
6. 所有枚举值完整覆盖。
7. DTO/VO 均有 Swagger 注解和校验注解。
