## MODIFIED Requirements

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
