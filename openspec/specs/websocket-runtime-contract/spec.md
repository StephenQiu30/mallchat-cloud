# websocket-runtime-contract Specification

## Purpose
TBD - created by archiving change align-websocket-runtime-contract. Update Purpose after archive.
## Requirements
### Requirement: WebSocket runtime host is explicit
The system SHALL define which Spring service is allowed to host the Netty WebSocket listener for the default IM runtime.

#### Scenario: Notification service hosts the default listener
- **WHEN** the default MallChat IM runtime is started
- **THEN** `mallchat-notification-service` SHALL explicitly enable the WebSocket listener
- **AND** `mallchat-chat-service` SHALL NOT implicitly start another WebSocket listener

### Requirement: Common WebSocket module does not start implicitly
The system SHALL keep the reusable WebSocket common module disabled by default unless a runtime service explicitly opts in.

#### Scenario: A service depends on common WebSocket without opting in
- **WHEN** a Spring service imports `mallchat-common-websocket`
- **AND** `websocket.enabled` is absent or false
- **THEN** the Netty WebSocket server SHALL NOT start

#### Scenario: A runtime service explicitly opts in
- **WHEN** a Spring service imports `mallchat-common-websocket`
- **AND** `websocket.enabled=true`
- **THEN** the Netty WebSocket server MAY start using the configured port, path, boss thread count, and worker thread count

### Requirement: Client connection contract is documented
The system SHALL document the supported WebSocket connection address, token placement, heartbeat behavior, and current gateway limitations.

#### Scenario: A client connects in the default runtime
- **WHEN** a client needs real-time IM events
- **THEN** the documented default endpoint SHALL identify the notification-service WebSocket host, the configured path, and the supported token delivery methods
- **AND** the documentation SHALL state whether `/api/websocket/**` through gateway is an accepted runtime entry

