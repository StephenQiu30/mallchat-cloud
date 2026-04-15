## MODIFIED Requirements

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
