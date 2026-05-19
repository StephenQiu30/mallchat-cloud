# chat-message

## Purpose

Define room message read progress, unread clearing, and read-boundary behavior for the MallChat chat domain.
## Requirements
### Requirement: Room members can publish read progress
The system SHALL allow a room member to mark messages as read, SHALL persist the submitted read boundary for that room member, and SHALL publish a message-read event that other clients can use to update read state.

#### Scenario: Mark room messages as read
- **WHEN** a room member submits a read request with a target message in a room they belong to
- **THEN** the system updates the member's read boundary to that message and emits a room read event referencing the room and read boundary

#### Scenario: Clear unread state after reading the newest message
- **WHEN** a user marks a session's newest message as read
- **THEN** the system updates the session unread state so the unread count no longer includes messages up to that newest boundary

#### Scenario: Preserve unread state after partial read
- **WHEN** a user marks a message as read that is older than the session's newest message
- **THEN** the system updates the stored read boundary but SHALL preserve unread state for newer messages beyond that boundary

### Requirement: Room messages can be queried after a receive cursor
The system SHALL provide a room message query that returns persisted messages newer than a client's receive cursor.

#### Scenario: Query newer messages by message id
- **WHEN** a room member queries messages with `afterMessageId`
- **THEN** the system filters messages to the same room with ids greater than `afterMessageId`
- **AND** the system returns the messages in ascending message id order

#### Scenario: Limit reconnect compensation window
- **WHEN** a room member queries messages after a receive cursor
- **AND** the requested limit is missing, invalid, or above the supported maximum
- **THEN** the system applies a bounded default or maximum limit before querying messages

#### Scenario: Keep history paging direction unchanged
- **WHEN** a room member uses the existing history-message endpoint
- **THEN** the system continues to return older messages before `lastMessageId`
- **AND** the reconnect compensation query SHALL NOT change existing history paging semantics
