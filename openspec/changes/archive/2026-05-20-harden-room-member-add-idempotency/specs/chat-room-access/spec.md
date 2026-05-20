## ADDED Requirements

### Requirement: 受控入群幂等不得覆盖既有成员角色

The system SHALL treat controlled membership creation as idempotent when the room member already exists, and SHALL NOT overwrite the existing member role through the add-member path.

#### Scenario: 重复邀请不得降级管理员

- **WHEN** an existing `ADMIN` group member is added again through a controlled membership path with `MEMBER` role
- **THEN** the system keeps the existing `ADMIN` role
- **AND** no role update is persisted

#### Scenario: 重复邀请不得降级群主

- **WHEN** an existing `OWNER` group member is added again through a controlled membership path with `MEMBER` role
- **THEN** the system keeps the existing `OWNER` role
- **AND** no role update is persisted

#### Scenario: 重复加入普通成员保持幂等

- **WHEN** an existing `MEMBER` group member is added again through a controlled membership path with `MEMBER` role
- **THEN** the system keeps the existing `MEMBER` role
- **AND** no role update is persisted
