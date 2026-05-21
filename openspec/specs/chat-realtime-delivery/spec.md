# chat-realtime-delivery Specification

## Purpose
Define reliable room realtime delivery behavior when Redis room member cache is cold, missing, or empty.
## Requirements
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

### Requirement: Room realtime events carry a database member snapshot
The system SHALL attach a database-backed room member snapshot to room realtime events produced by chat-service.

#### Scenario: Chat service enqueues a room event
- **WHEN** chat-service enqueues a chat message, recall, or read room event
- **THEN** the WebSocket message SHALL include the room id
- **AND** the WebSocket message SHALL include the room member user ids resolved from the chat room membership fact source

### Requirement: Existing realtime event model is reused
The system SHALL reuse existing RabbitMQ and WebSocket message models for the room cache-miss fallback.

#### Scenario: A room event is produced with member snapshot
- **WHEN** chat-service sends a room realtime event
- **THEN** the event SHALL still use `CHAT_MESSAGE_PUSH`
- **AND** the event SHALL still wrap payloads with `ImWebSocketEvent`
- **AND** the event SHALL NOT introduce a parallel MQ type or custom WebSocket envelope

### Requirement: Reconnecting room members can compensate missed messages
The system SHALL allow a current room member to query messages created after the client's last received message id for reconnect compensation, using persisted message facts rather than realtime connection state.

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

#### Scenario: Missing cursor falls back to bounded latest messages
- **WHEN** a room member requests reconnect compensation without a valid receive cursor
- **THEN** the system returns a bounded latest message window
- **AND** the returned messages are still ordered by ascending message id
