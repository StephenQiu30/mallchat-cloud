## ADDED Requirements

### Requirement: 退群会话删除推送失败应降级

The system SHALL complete group quit business facts even when the leaving user's session-delete realtime push fails.

#### Scenario: 退群推送失败不回滚退群

- **WHEN** a group member quits a group room
- **AND** the session-delete realtime push fails
- **THEN** the system removes the member from the room membership fact source
- **AND** the system removes the user's chat session for that room
- **AND** the push failure is degraded instead of rolling back the quit transaction
