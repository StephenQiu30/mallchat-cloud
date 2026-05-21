## MODIFIED Requirements

### Requirement: Room realtime delivery survives room member cache miss
The system SHALL NOT skip a room realtime event solely because the Redis room member cache is empty or missing, and SHALL respect a non-empty `userIds` allowlist when the event carries one.

#### Scenario: Redis room member cache is present
- **WHEN** notification-service handles a room realtime event
- **AND** the Redis room member set for the room is non-empty
- **AND** the event does not carry a `userIds` allowlist
- **THEN** the system SHALL push the event to the cached room members

#### Scenario: Redis room member cache is present and event carries an allowlist
- **WHEN** notification-service handles a room realtime event
- **AND** the Redis room member set for the room is non-empty
- **AND** the WebSocket message carries a non-empty `userIds` allowlist
- **THEN** the system SHALL push only to cached room members that also appear in the allowlist

#### Scenario: Redis room member cache is missing but the event carries a member snapshot
- **WHEN** notification-service handles a room realtime event
- **AND** the Redis room member set for the room is empty or missing
- **AND** the WebSocket message carries a non-empty `userIds` member snapshot
- **THEN** the system SHALL push the event to the member snapshot
- **AND** the system SHALL NOT drop the event only because Redis is cold
