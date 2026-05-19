# chat-room-access Delta

## ADDED Requirements

### Requirement: 群主应能更新群资料

The system SHALL allow only the group owner to update group profile fields for an existing group room.

#### Scenario: 群主更新群名称头像和公告

- **WHEN** a group owner submits a group profile update with one or more of `name`, `avatar`, or `announcement`
- **THEN** the system updates the corresponding group profile fields
- **AND** the room base profile remains consistent with the group profile extension

#### Scenario: 非群聊拒绝群资料更新

- **WHEN** a user submits a group profile update for a private room
- **THEN** the system rejects the request
- **AND** no group profile record is created or updated

#### Scenario: 非群主拒绝群资料更新

- **WHEN** a non-owner room member submits a group profile update
- **THEN** the system rejects the request
- **AND** no group profile record is created or updated

#### Scenario: 空更新内容被拒绝

- **WHEN** a group owner submits a group profile update without `name`, `avatar`, or `announcement`
- **THEN** the system rejects the request as invalid

#### Scenario: 群扩展记录缺失时保留基础群资料

- **WHEN** a group owner updates only the announcement
- **AND** the group profile extension record does not exist
- **THEN** the system creates the group profile extension record
- **AND** the created extension record keeps the current room name and avatar as defaults
- **AND** the created extension record stores the new announcement

#### Scenario: 已存在群扩展记录字段为空时修复基础群资料

- **WHEN** a group owner updates only the announcement
- **AND** the group profile extension record exists but its name or avatar is blank
- **THEN** the system repairs the blank group profile fields from the current room base profile
- **AND** the group profile extension record stores the new announcement

#### Scenario: 空头像被拒绝

- **WHEN** a group owner submits a group profile update with a blank `avatar`
- **THEN** the system rejects the request as invalid
