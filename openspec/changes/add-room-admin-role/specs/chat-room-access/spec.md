## ADDED Requirements

### Requirement: 群主应能任免群管理员
The system SHALL allow a group owner to grant and revoke the `ADMIN` role for existing group members by reusing `chat_room_member.role`.

#### Scenario: 群主任命普通成员为管理员
- **WHEN** a group owner grants admin role to an existing `MEMBER`
- **THEN** the target member role becomes `ADMIN`

#### Scenario: 群主取消管理员
- **WHEN** a group owner revokes admin role from an existing `ADMIN`
- **THEN** the target member role becomes `MEMBER`

#### Scenario: 非群主拒绝管理员任免
- **WHEN** a non-owner attempts to grant or revoke admin role
- **THEN** the system rejects the request
- **AND** no member role is changed

#### Scenario: 群主角色不可被任免为管理员
- **WHEN** a group owner attempts to grant or revoke admin role for an `OWNER` member
- **THEN** the system rejects the request
- **AND** the owner role remains unchanged
