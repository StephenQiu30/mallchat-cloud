# chat-room-access Delta

## ADDED Requirements

### Requirement: 群主应能移除普通群成员

The system SHALL allow only the group owner to remove an existing ordinary `MEMBER` role user from a group room.

#### Scenario: 群主移除普通成员

- **WHEN** a group owner removes an existing ordinary member from a group room
- **THEN** the system removes the target user's room membership from the membership fact source
- **AND** the system removes the target user from the room member cache

#### Scenario: 非群聊拒绝移除成员

- **WHEN** a user submits a member removal request for a private room
- **THEN** the system rejects the request
- **AND** no room membership is removed

#### Scenario: 非群主拒绝移除成员

- **WHEN** a non-owner submits a member removal request
- **THEN** the system rejects the request
- **AND** no room membership is removed

#### Scenario: 目标成员不存在时拒绝

- **WHEN** a group owner removes a user who is not a current room member
- **THEN** the system rejects the request
- **AND** no session delete event is emitted for that target user

#### Scenario: 群主不能移除自己或其他群主账号

- **WHEN** a group owner attempts to remove themselves or a room owner account
- **THEN** the system rejects the request
- **AND** the owner membership remains unchanged

#### Scenario: 管理员角色不在本次移除范围内

- **WHEN** a group owner attempts to remove an `ADMIN` role member
- **THEN** the system rejects the request
- **AND** no room membership is removed
- **AND** no session delete event is emitted
