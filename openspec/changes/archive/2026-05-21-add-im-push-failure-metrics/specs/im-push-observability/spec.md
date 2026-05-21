## ADDED Requirements

### Requirement: IM realtime push outcomes are measurable
The system SHALL record realtime push outcomes by message business type and event type.

#### Scenario: Target user is offline on the local node
- **WHEN** notification-service handles a targeted realtime push
- **AND** no local WebSocket connection receives the message
- **THEN** the system SHALL record a push outcome tagged with result `offline`
- **AND** the outcome SHALL include the message `bizType` and `eventType`

#### Scenario: Channel write throws during push
- **WHEN** notification-service handles a realtime push
- **AND** writing to a local WebSocket connection throws
- **THEN** the system SHALL record a push outcome tagged with result `failure`
- **AND** the handler SHALL preserve the existing exception behavior for MQ retry or caller degradation

#### Scenario: Push succeeds
- **WHEN** one or more local WebSocket connections receive the message
- **THEN** the system SHALL record a push outcome tagged with result `success`
