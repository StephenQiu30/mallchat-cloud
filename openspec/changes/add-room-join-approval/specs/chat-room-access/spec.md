## ADDED Requirements

### Requirement: 入群申请审批是受控群成员入口
The system SHALL allow a non-member to apply for group room membership and SHALL create membership only after an owner or admin approves the pending application.

#### Scenario: 用户申请入群
- **WHEN** a non-member applies to join an existing group room
- **THEN** the system creates a pending `chat_room_join_apply` record
- **AND** the system SHALL NOT create room membership before approval

#### Scenario: 重复待处理申请幂等
- **WHEN** the same user applies to the same group room while a pending application exists
- **THEN** the system returns the existing application ID
- **AND** no duplicate pending application is created

#### Scenario: 群主或管理员同意入群申请
- **WHEN** an owner or admin approves a pending join application
- **THEN** the system marks the application as approved
- **AND** creates group membership with `MEMBER` role
- **AND** creates or updates the applicant's chat session

#### Scenario: 群主或管理员拒绝入群申请
- **WHEN** an owner or admin rejects a pending join application
- **THEN** the system marks the application as rejected
- **AND** does not create group membership

#### Scenario: 普通成员不可审核
- **WHEN** an ordinary member attempts to approve or reject a join application
- **THEN** the system rejects the request
- **AND** the application and membership facts remain unchanged

#### Scenario: 通知失败不回滚申请和审核事实
- **WHEN** notification-center creation fails after a join application or review is persisted
- **THEN** the application or review fact remains successful
- **AND** the failure is degraded instead of rolling back the local transaction
