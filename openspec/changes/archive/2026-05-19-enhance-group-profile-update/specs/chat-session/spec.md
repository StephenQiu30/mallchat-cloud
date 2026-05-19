# chat-session Delta

## ADDED Requirements

### Requirement: 群资料更新后成员会话应刷新

The system SHALL notify current room members to refresh session metadata after a group profile update succeeds.

#### Scenario: 群资料更新后推送会话刷新

- **WHEN** a group owner successfully updates group profile fields
- **THEN** the system sends a session update event to current room members
- **AND** the session update data reflects the persisted group profile state when a session exists for the member

#### Scenario: 会话刷新失败不破坏群资料事实数据

- **WHEN** a group profile update is persisted successfully
- **AND** session refresh push fails for a room member
- **THEN** the group profile update remains successful
- **AND** the push failure SHALL NOT roll back the persisted group profile data
