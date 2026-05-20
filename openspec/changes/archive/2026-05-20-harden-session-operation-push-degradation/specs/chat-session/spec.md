## ADDED Requirements

### Requirement: 会话操作推送失败应降级

The system SHALL keep persisted session operation facts successful even when realtime session push delivery fails.

#### Scenario: 置顶推送失败不回滚置顶状态

- **WHEN** a user updates a chat session top status
- **AND** the session-update realtime push fails
- **THEN** the persisted top status remains successful
- **AND** the push failure is degraded instead of failing the session operation

#### Scenario: 删除会话推送失败不回滚删除事实

- **WHEN** a user deletes a chat session
- **AND** the session-delete realtime push fails
- **THEN** the session delete result remains successful
- **AND** the push failure is degraded instead of failing the session operation
