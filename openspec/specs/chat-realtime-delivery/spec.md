# chat-realtime-delivery Specification

## Purpose
Define reliable room realtime delivery behavior when Redis room member cache is cold, missing, or empty.
## Requirements
### Requirement: Room realtime delivery survives room member cache miss
The system SHALL NOT skip a room realtime event solely because the Redis room member cache is empty or missing.

#### Scenario: Redis room member cache is present
- **WHEN** notification-service handles a room realtime event
- **AND** the Redis room member set for the room is non-empty
- **THEN** the system SHALL push the event to the cached room members

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
