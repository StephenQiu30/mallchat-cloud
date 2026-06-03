---
layer: Plan
doc_no: "PL-IM-009"
audience:
  - PM
  - Dev
  - QA
feature_area: security-permission
purpose: "定义 P-IM-009 安全权限功能的阶段计划、任务拆解、依赖和交付顺序。"
canonical_path: "docs/plans/PL-IM-009-security-permission-plan.md"
status: draft
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "docs/prd/P-IM-009-security-permission-prd.md"
outputs:
  - "安全权限功能的分阶段实施计划"
triggers:
  - "安全权限功能开发启动时"
  - "阶段验收或复盘时"
downstream:
  - "各子 Issue 的实现计划"
---

# P-IM-009 安全权限 实施计划

## 1. 阶段概览

```
P0 (核心安全基线)  →  P1 (群权限增强)  →  P2 (频控与审计)
     ↓                    ↓                    ↓
  好友权限            群成员权限            频控保护
  撤回权限            动态可见性            审计日志
  举报基础            群主转让              举报审核增强
```

## 2. P0：核心安全基线（第 1-2 周）

### 2.1 好友权限

| 任务 | 描述 | 依赖 | 验收标准 |
| --- | --- | --- | --- |
| P0-F1 | 好友申请验证消息 | 无 | 申请可附带消息，对方收到通知 |
| P0-F2 | 好友申请通过/拒绝 | P0-F1 | 通过后双方成为好友 |
| P0-F3 | 拉黑/解除拉黑 | 无 | 拉黑后无法发消息，解除后恢复 |
| P0-F4 | 被拉黑发消息拦截 | P0-F3 | 返回 BLOCKED_BY_TARGET 错误码 |

**代码影响范围：**
- `UserFriendService` / `UserFriendServiceImpl`：增加拉黑校验
- `ChatMessageService` / `ChatMessageServiceImpl`：发消息前检查好友关系和拉黑状态
- `UserFriendController`：增加拉黑/解除拉黑接口

### 2.2 撤回权限

| 任务 | 描述 | 依赖 | 验收标准 |
| --- | --- | --- | --- |
| P0-R1 | 撤回时间窗口校验（2 分钟） | 无 | 超时返回 RECALL_TIMEOUT |
| P0-R2 | 撤回人校验（仅限自己或管理员） | 无 | 非本人非管理员返回 NO_PERMISSION |
| P0-R3 | 管理员/群主撤回任意消息 | P0-R2 | 群主/管理员可撤回群内任何消息 |

**代码影响范围：**
- `ChatMessageServiceImpl`：`recallMessage()` 方法增加时间窗口和权限校验
- `ChatRoomMemberService`：复用 `isOwner()` / `isAdmin()` 方法

### 2.3 举报基础

| 任务 | 描述 | 依赖 | 验收标准 |
| --- | --- | --- | --- |
| P0-RP1 | 举报消息/用户/动态 | 无 | 举报创建成功，管理员收到通知 |
| P0-RP2 | 举报列表查询 | P0-RP1 | 支持按状态、类型筛选 |

**代码影响范围：**
- `ChatReportService` / `ChatReportServiceImpl`：已有基础，需扩展 targetType 和审核流程
- `ChatReportController`：增加举报接口

## 3. P1：群权限增强（第 3-4 周）

### 3.1 群成员权限

| 任务 | 描述 | 依赖 | 验收标准 |
| --- | --- | --- | --- |
| P1-G1 | 群主设置/撤销管理员 | 无 | 仅群主可操作 |
| P1-G2 | 管理员踢出成员 | P1-G1 | 管理员可踢普通成员，不可踢其他管理员 |
| P1-G3 | 群主/管理员禁言成员 | P1-G1 | 禁言后无法发消息 |
| P1-G4 | 群主转让 | 无 | 转让后角色变更正确 |
| P1-G5 | 仅管理员可邀请设置 | P1-G1 | 普通成员邀请需审批 |

**代码影响范围：**
- `ChatRoomServiceImpl`：增加权限校验逻辑
- `ChatRoomMemberService`：增加 `isAdmin()`、`canInvite()` 等方法
- `ChatMessageServiceImpl`：发消息前检查禁言状态
- `ChatGroupInfo`：增加 `invitePermission` 字段

### 3.2 动态可见性

| 任务 | 描述 | 依赖 | 验收标准 |
| --- | --- | --- | --- |
| P1-M1 | 动态发布时选择可见范围 | 无 | 支持公开/好友/私密 |
| P1-M2 | 好友可见动态过滤 | P1-M1 | 非好友无法查看好友可见动态 |
| P1-M3 | 被拉黑用户动态拦截 | P0-F3 | 被拉黑后无法查看对方动态 |

**代码影响范围：**
- `ChatMoment`：已有 `visibility` 字段，需校验逻辑
- `ChatMomentService` / `ChatMomentServiceImpl`：查询时增加可见性过滤
- `ChatMomentController`：查询接口增加权限校验

## 4. P2：频控与审计（第 5-6 周）

### 4.1 频控保护

| 任务 | 描述 | 依赖 | 验收标准 |
| --- | --- | --- | --- |
| P2-FR1 | 消息发送限速（30 条/分钟） | 无 | 超频返回 429 |
| P2-FR2 | 好友申请限速（20 条/小时） | 无 | 超频返回 429 |
| P2-FR3 | 举报提交限速（10 条/小时） | 无 | 超频返回 429 |
| P2-FR4 | Gateway 频控 Filter | 无 | 统一限流入口 |

**代码影响范围：**
- `mallchat-gateway`：新增 `RateLimitFilter`，使用 Redis 滑动窗口
- `ChatMessageController`、`UserFriendController`、`ChatReportController`：无需改动，频控在 Gateway 层

### 4.2 审计日志

| 任务 | 描述 | 依赖 | 验收标准 |
| --- | --- | --- | --- |
| P2-A1 | 审计日志数据模型 | 无 | `user_audit_log` 表创建 |
| P2-A2 | 关键操作审计记录 | P2-A1 | 踢人、禁言、转让、封禁等操作记录 |
| P2-A3 | 审计日志查询接口 | P2-A2 | 管理员可查询，支持按时间/操作人/类型筛选 |

**代码影响范围：**
- `mallchat-log-service`：新增 `AuditLog` Entity、Mapper、Service
- `mallchat-chat-service`：在权限变更操作中发送审计事件
- `mallchat-api`：新增审计日志 DTO/VO

### 4.3 举报审核增强

| 任务 | 描述 | 依赖 | 验收标准 |
| --- | --- | --- | --- |
| P2-RP1 | 举报审核通过/拒绝 | P0-RP1 | 审核后状态更新，处置动作生效 |
| P2-RP2 | 举报处置动作（警告/禁言/封禁） | P2-RP1 | 处置后用户状态变更 |

## 5. 依赖关系图

```
P0-F1 (好友申请) ──→ P0-F2 (通过/拒绝)
P0-F3 (拉黑) ──→ P0-F4 (拦截) ──→ P1-M3 (动态拦截)
P0-R1 (撤回时间) ──→ P0-R2 (撤回人校验) ──→ P0-R3 (管理员撤回)
P0-RP1 (举报基础) ──→ P2-RP1 (举报审核)

P1-G1 (群主设管理员) ──→ P1-G2 (踢人)
                    ──→ P1-G3 (禁言)
                    ──→ P1-G5 (邀请权限)
P1-M1 (动态可见性) ──→ P1-M2 (好友过滤)

P2-FR1~FR4 (频控) 无外部依赖
P2-A1 (审计模型) ──→ P2-A2 (审计记录) ──→ P2-A3 (审计查询)
```

## 6. 风险与边界

| 风险 | 影响 | 缓解措施 |
| --- | --- | --- |
| 好友关系查询性能 | 高频发消息时查询好友关系和拉黑状态可能成为瓶颈 | Redis 缓存好友关系和拉黑列表 |
| 禁言状态同步 | 禁言后在线用户需实时生效 | WebSocket 推送禁言事件 |
| 频控误杀 | 正常用户高频使用可能被误限 | 阈值可配置，支持白名单 |
| 审计日志存储 | 90 天日志量可能较大 | 分表或使用 ES 存储 |
| 动态可见性查询复杂度 | 好友可见需要联合查询好友关系 | 查询时先过滤再分页 |

## 7. TDD 执行要求

每个子 Issue 实现时必须遵循：

1. **红灯**：先写失败测试，覆盖核心权限校验逻辑和边界条件。
2. **绿灯**：实现最小代码使测试通过。
3. **重构**：在测试保护下清理命名和结构。

测试覆盖优先级：
1. 权限校验核心逻辑（拉黑拦截、撤回窗口、角色校验）。
2. 边界条件（超时、超频、空值）。
3. 错误码返回正确性。

## 8. 变更记录

| 日期 | 作者 | 版本 | 变更说明 |
| --- | --- | --- | --- |
| 2026-06-03 | StephenQiu30 | 0.1.0 | 初始化文档 |
