## ADDED Requirements

### Requirement: Reconnecting room members can compensate missed messages
The system SHALL allow a current room member to query messages created after the client's last received message id for reconnect compensation.

#### Scenario: Query missed messages after a received cursor
- **WHEN** an authenticated room member requests messages for a room with an `afterMessageId`
- **THEN** the system returns messages in that room with ids greater than `afterMessageId`
- **AND** the messages are returned in ascending message id order

#### Scenario: Reject compensation for non-members
- **WHEN** an authenticated user who is not a current room member requests reconnect compensation for that room
- **THEN** the system rejects the request and SHALL NOT return room messages

#### Scenario: Compensation does not rely on realtime caches
- **WHEN** a room member requests reconnect compensation
- **THEN** the system authorizes through the room membership fact source
- **AND** the system SHALL NOT require Redis online state or Redis room member cache to return persisted messages
