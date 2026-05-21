## ADDED Requirements

### Requirement: 用户可以拉黑和解除拉黑其他用户
The system SHALL allow an authenticated user to block or unblock another existing user with idempotent behavior.

#### Scenario: 拉黑有效用户
- **WHEN** 用户拉黑一个存在且不是自己的用户
- **THEN** 系统创建 `user_friend_block` 记录
- **AND** 重复拉黑同一用户保持成功且不创建重复记录

#### Scenario: 解除拉黑保持幂等
- **WHEN** 用户解除对一个用户的拉黑
- **THEN** 系统删除该方向的拉黑关系
- **AND** 重复解除拉黑保持成功

#### Scenario: 拉黑自己被拒绝
- **WHEN** 用户尝试拉黑自己
- **THEN** 系统返回参数错误

### Requirement: 拉黑关系阻断好友申请
The system SHALL reject friend applications and approvals when either direction has an active block relation.

#### Scenario: 拉黑后不能申请好友
- **WHEN** 用户 A 和用户 B 任一方向存在拉黑关系
- **AND** 用户 A 尝试向用户 B 发起好友申请
- **THEN** 系统拒绝申请
- **AND** 不创建新的好友申请记录

#### Scenario: 审批前出现拉黑关系
- **WHEN** 好友申请仍待处理
- **AND** 审批前双方任一方向出现拉黑关系
- **THEN** 系统拒绝通过该好友申请
- **AND** 不创建好友关系
