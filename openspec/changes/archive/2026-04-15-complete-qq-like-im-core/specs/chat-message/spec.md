## ADDED Requirements

### Requirement: User can send private and group chat messages
The system SHALL allow an authenticated room member to send messages to a private chat room or group chat room, persist the message, and publish a real-time message event to the relevant recipients.

#### Scenario: Send a private chat message
- **WHEN** a user sends a valid message to a private room they belong to
- **THEN** the system persists the message and pushes a real-time chat message event to the sender and the peer user

#### Scenario: Send a group chat message
- **WHEN** a user sends a valid message to a group room they belong to
- **THEN** the system persists the message and pushes a real-time chat message event to online room members

### Requirement: User can query chat history in room order
The system SHALL provide paginated room history ordered by descending message identifier and SHALL return enough message metadata for clients to render message previews and reply relationships.

#### Scenario: Query latest messages
- **WHEN** a user requests history for a room without a cursor
- **THEN** the system returns the latest messages for that room in stable order

#### Scenario: Query older messages with cursor
- **WHEN** a user requests history for a room with a last message cursor
- **THEN** the system returns messages older than the cursor and SHALL NOT include newer messages in the result

### Requirement: User can recall a previously sent message
The system SHALL allow the original sender to recall an eligible message, update the stored message status, and publish a real-time recall event to room participants.

#### Scenario: Recall own message
- **WHEN** the original sender recalls an eligible message in a room
- **THEN** the system marks the message as recalled and pushes a recall event for the room

#### Scenario: Reject recall by non-sender
- **WHEN** a different user attempts to recall a message they did not send
- **THEN** the system rejects the request and leaves the message status unchanged

### Requirement: Room members can publish read progress
The system SHALL allow a room member to mark messages as read and SHALL publish a message-read event that other clients can use to update read state.

#### Scenario: Mark room messages as read
- **WHEN** a room member submits a read request with a target message in a room they belong to
- **THEN** the system updates the member's read progress and emits a room read event referencing the room and read boundary

#### Scenario: Clear unread state after read
- **WHEN** a user marks a session's newest message as read
- **THEN** the system updates the session unread state so the unread count no longer includes messages up to that boundary
