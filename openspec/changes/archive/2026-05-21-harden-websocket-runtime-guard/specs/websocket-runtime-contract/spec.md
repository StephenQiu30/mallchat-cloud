## ADDED Requirements

### Requirement: WebSocket runtime connection guard is configurable
The system SHALL support lightweight local WebSocket runtime guards for connection count, repeated connection attempts, and abnormal disconnect visibility.

#### Scenario: Same user exceeds local connection limit
- **WHEN** a user already has the configured maximum number of active local WebSocket connections
- **AND** the same user completes another WebSocket handshake on the same server instance
- **THEN** the server SHALL reject the new connection registration
- **AND** the server SHALL close the new channel without removing the existing active connections

#### Scenario: Same user reconnects too quickly
- **WHEN** a user connects again before the configured minimum reconnect interval has elapsed
- **THEN** the server SHALL reject the new connection registration
- **AND** the server SHALL close the new channel before persisting the new connection metadata

#### Scenario: Unknown channel disconnect is audited
- **WHEN** the WebSocket runtime observes a channel removal for a channel that was not registered to a user
- **THEN** the runtime SHALL record a lightweight abnormal disconnect count
- **AND** the runtime SHALL log the channel id for diagnosis
