# chat-session Delta

## ADDED Requirements

### Requirement: 成员被移除后会话应删除

The system SHALL remove the kicked member's chat session and notify that member after a successful owner-initiated group member removal.

#### Scenario: 移除成员后删除目标成员会话

- **WHEN** a group owner successfully removes a member from a group room
- **THEN** the system removes the target member's session for that room
- **AND** the system sends a session delete event to the target member

#### Scenario: 会话删除推送失败不破坏成员移除事实

- **WHEN** a group owner successfully removes a member from a group room
- **AND** session delete push fails
- **THEN** the room membership removal remains successful
- **AND** the push failure SHALL NOT restore the removed membership

#### Scenario: 会话事实删除与成员移除保持事务一致

- **WHEN** chat session deletion throws a persistence exception during member removal
- **THEN** the member removal transaction fails
- **AND** the membership removal SHALL NOT be reported as successful
