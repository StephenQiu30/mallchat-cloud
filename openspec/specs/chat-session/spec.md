# chat-session

## Purpose

Define how MallChat session state follows message creation, unread count updates, and read-boundary changes.
## Requirements
### Requirement: Session state updates follow message lifecycle
The system SHALL update session last message, unread count, last read boundary, and activity time whenever room messages are produced or read.

#### Scenario: Increment unread for receivers
- **WHEN** a new room message is sent
- **THEN** the system updates receiver sessions with the latest message and increments unread count for users other than the sender

#### Scenario: Preserve sender session without unread growth
- **WHEN** a new room message is sent by a user
- **THEN** the sender's session is updated with the latest message and activity time without increasing unread count

#### Scenario: Reduce unread according to read boundary
- **WHEN** a user marks a room message as read
- **THEN** the system updates that user's session last-read boundary and recalculates unread state so only messages newer than the submitted boundary remain unread

#### Scenario: Ignore stale read boundary updates
- **WHEN** a user submits a read boundary that is older than or equal to the currently stored boundary
- **THEN** the system SHALL NOT increase unread count or move the stored last-read boundary backward

### Requirement: Session list exposes message cursor state
The system SHALL expose session message cursor fields so clients can distinguish the latest persisted message from the user's read boundary.

#### Scenario: Session item includes cursor fields
- **WHEN** an authenticated user queries their chat session list
- **THEN** each session item includes `lastMessageId`
- **AND** each session item includes `lastReadMessageId`

#### Scenario: Receive cursor and read cursor remain separate
- **WHEN** a client decides whether reconnect compensation is needed
- **THEN** it can compare its last received message id with the session `lastMessageId`
- **AND** it SHALL NOT need to treat `lastReadMessageId` as the reconnect compensation cursor

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

