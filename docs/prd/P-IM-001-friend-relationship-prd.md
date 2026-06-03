---
layer: PRD
doc_no: "P-IM-001"
audience:
  - PM
  - Dev
  - QA
feature_area: friend-relationship
purpose: "定义好友关系功能的产品边界、核心用户故事和验收标准，支撑 IM 基础社交闭环。"
canonical_path: "docs/prd/P-IM-001-friend-relationship-prd.md"
status: active
version: "1.0.0"
owner: "StephenQiu30"
inputs:
  - "MallChat IM 产品规划"
outputs:
  - "好友关系功能 PRD，驱动 Plan、实现和验收"
triggers:
  - "好友关系功能需求变更或新阶段规划时"
downstream:
  - "docs/plans/PL-IM-001-friend-relationship-plan.md"
  - "docs/acceptance/AC-IM-001-friend-relationship-acceptance.md"
---

# 好友关系 PRD

## 1. 背景

MallChat IM 作为即时通讯系统，好友关系是社交功能的基础。用户需要能够发起好友申请、审批申请、管理好友列表，才能进行后续的私聊、群组和动态等功能。当前系统已有用户管理和基础聊天能力，但缺乏好友关系管理，无法支撑完整的社交场景。

## 2. 目标

```gherkin
Given 用户 A 和用户 B 均已注册
When A 向 B 发起好友申请，B 同意
Then 双方 user_friend 记录存在，好友列表可查
And 申请状态更新为已同意，通知发送给申请方
And 重复申请返回已有申请 ID（幂等）
```

### 2.1 核心用户故事

1. **发起好友申请**：用户通过目标用户 ID 发起好友申请，附带留言。
2. **审批好友申请**：目标用户同意或拒绝申请，同意后自动建立双向好友关系。
3. **查看好友列表**：用户查看所有好友，支持按分组筛选。
4. **查看申请列表**：用户查看收到的好友申请记录（分页）。
5. **删除好友**：用户移除好友关系（双向）。
6. **拉黑/解除拉黑**：用户拉黑其他用户，限制好友申请和消息。

### 2.2 幂等与安全

- 同一对用户同一方向只允许一条待处理申请，重复申请返回已有 ID。
- 反向存在待处理申请时，提示用户前往申请列表处理。
- 双方存在拉黑关系时，禁止发起申请和审批。
- 非申请目标用户无权审批。

## 3. 非目标

- 好友推荐算法（不在 MVP 范围）。
- 好友备注同步到聊天会话（后续迭代）。
- 批量导入/导出好友。
- 好友上限管理。

## 4. 核心内容

### 4.1 数据模型

| 表名 | 说明 | 关键字段 |
| --- | --- | --- |
| `user_friend` | 好友关系（双向） | `user_id`, `friend_user_id`, `remark_name`, `friend_group_name` |
| `user_friend_apply` | 好友申请 | `user_id`, `target_id`, `msg`, `status`（1=待处理, 2=已同意, 3=已拒绝） |
| `user_friend_block` | 拉黑关系 | `user_id`, `blocked_user_id` |

### 4.2 API 接口

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/chat/friend/apply/add` | POST | 发起好友申请 |
| `/chat/friend/apply/approve` | POST | 审批好友申请 |
| `/chat/friend/apply/list/page/vo` | POST | 获取申请列表 |
| `/chat/friend/list/vo` | GET | 获取好友列表 |
| `/chat/friend/search` | GET | 搜索候选用户 |
| `/chat/friend/delete` | DELETE | 删除好友 |
| `/chat/friend/block` | POST | 拉黑用户 |
| `/chat/friend/block` | DELETE | 解除拉黑 |
| `/chat/friend/profile/update` | POST | 更新好友备注/分组 |

### 4.3 错误码

| 场景 | ErrorCode | 说明 |
| --- | --- | --- |
| 申请自己 | PARAMS_ERROR | 不能添加自己为好友 |
| 已是好友 | OPERATION_ERROR | 已经是好友了 |
| 双向拉黑 | NO_AUTH_ERROR | 双方存在拉黑关系 |
| 重复申请 | 返回已有 ID | 幂等处理 |
| 反向申请存在 | OPERATION_ERROR | 对方已发起申请 |
| 审批无权限 | NO_AUTH_ERROR | 非申请目标用户 |
| 已处理申请 | PARAMS_ERROR | 已处理过该申请 |

### 4.4 实时通知

- 好友申请通过 RabbitMQ 推送 WebSocket 事件。
- 好友审批通过 RabbitMQ 推送 WebSocket 事件。
- 业务通知通过 NotificationFeignClient 发送，事务提交后执行。

## 5. 关联文档

### 5.1 输入文档

1. MallChat IM 产品规划

### 5.2 输出文档

1. `docs/plans/PL-IM-001-friend-relationship-plan.md`

### 5.3 下游文档

1. `docs/acceptance/AC-IM-001-friend-relationship-acceptance.md`

## 6. 验收门禁

- [ ] A 发起申请，B 同意后双方 `user_friend` 存在。
- [ ] 好友列表可查，包含用户信息和在线状态。
- [ ] 重复申请返回已有 ID，不创建新记录。
- [ ] 反向申请存在时提示处理。
- [ ] 双方拉黑时禁止申请和审批。
- [ ] 非目标用户审批返回无权限。
- [ ] 已处理申请重复审批返回参数错误。
- [ ] 通知和 WebSocket 推送正常。
- [ ] 全部测试通过（41+ 用例）。

## 7. 风险与边界

- 好友列表缓存与数据库一致性依赖缓存失效策略。
- 通知发送失败不影响核心流程，但可能导致用户感知延迟。
- FeignClient 调用用户服务，用户不存在时需优雅处理。

## 8. 待确认问题

- 无。

## 9. 变更记录

| 日期 | 作者 | 版本 | 变更说明 |
| --- | --- | --- | --- |
| 2026-06-03 | StephenQiu30 | 1.0.0 | 初始化 PRD，基于已实现代码整理 |
