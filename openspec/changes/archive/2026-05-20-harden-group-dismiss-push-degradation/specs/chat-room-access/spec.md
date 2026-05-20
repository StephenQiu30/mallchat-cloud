## ADDED Requirements

### Requirement: 群解散会话删除推送失败应降级

The system SHALL complete group dismissal business facts even when a member session-delete realtime push fails.

#### Scenario: 推送失败不回滚群解散

- **WHEN** a group owner dismisses a group room
- **AND** one or more member session-delete realtime pushes fail
- **THEN** the system removes room memberships from the membership fact source
- **AND** the system removes chat sessions for the dismissed room
- **AND** the system removes the group profile extension
- **AND** the system removes the group room
- **AND** the push failure is degraded instead of rolling back the dismissal transaction
