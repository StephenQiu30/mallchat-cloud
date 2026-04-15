## ADDED Requirements

### Requirement: User can view a unified session list
The system SHALL provide a session list for the current user that combines private chats and group chats and includes ordering, unread count, last message preview, and display metadata.

#### Scenario: List sessions by activity
- **WHEN** a user requests their session list
- **THEN** the system returns sessions ordered by top status first and then by latest activity time

#### Scenario: Build private session display
- **WHEN** a listed session represents a private chat
- **THEN** the system returns the peer user's name, avatar, and online status for that session

### Requirement: Session state updates follow message lifecycle
The system SHALL update session last message, unread count, and activity time whenever room messages are produced or read.

#### Scenario: Increment unread for receivers
- **WHEN** a new room message is sent
- **THEN** the system updates receiver sessions with the latest message and increments unread count for users other than the sender

#### Scenario: Preserve sender session without unread growth
- **WHEN** a new room message is sent by a user
- **THEN** the sender's session is updated with the latest message and activity time without increasing unread count

### Requirement: User can manage session visibility
The system SHALL allow a user to top or delete a session from their own session list and SHALL push a session update event to the user's connected clients.

#### Scenario: Top a session
- **WHEN** a user marks one of their sessions as top
- **THEN** the system persists the top status and pushes a session update event for that user

#### Scenario: Delete a session
- **WHEN** a user deletes one of their sessions
- **THEN** the system removes that session from the user's list and pushes a session deletion update for that user
