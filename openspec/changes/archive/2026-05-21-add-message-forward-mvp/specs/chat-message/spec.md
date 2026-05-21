## ADDED Requirements

### Requirement: 房间成员可以单条转发可见消息
The system SHALL allow a user to forward one visible normal message to a target room through the existing send-message flow.

#### Scenario: 单条消息转发成功
- **WHEN** a user forwards a normal message they can access
- **AND** the user can send messages to the target room
- **THEN** the system persists a new message in the target room
- **AND** the new message copies source `type`, `content`, and `extra`
- **AND** realtime push and session update behavior reuse the normal send-message path

#### Scenario: 来源房间无权限不可转发
- **WHEN** a user forwards a message from a room they cannot access
- **THEN** the system rejects the request
- **AND** no new target-room message is persisted or pushed

#### Scenario: 目标房间无发送权限不可转发
- **WHEN** a user forwards a visible message to a room they cannot send to
- **THEN** the system rejects the request
- **AND** no new target-room message is persisted or pushed

#### Scenario: 非私聊参与者不可转发到私聊房间
- **WHEN** a user forwards a visible message to a private room where they are not `userLow` or `userHigh`
- **THEN** the system rejects the request
- **AND** no new target-room message is persisted or pushed

#### Scenario: 已撤回或删除消息不可转发
- **WHEN** a user forwards a recalled or deleted message
- **THEN** the system rejects the request
- **AND** no new target-room message is persisted or pushed
