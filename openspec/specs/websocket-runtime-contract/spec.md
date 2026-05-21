# websocket-runtime-contract Specification

## Purpose
定义 MallChat 默认 IM 实时链路的 WebSocket 运行承载、启动开关、连接入口、握手鉴权、心跳和网关边界。
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
The system SHALL document and enforce the supported WebSocket connection address, token placement, heartbeat behavior, current gateway limitations, and handshake rejection boundaries.

#### Scenario: A client connects in the default runtime
- **WHEN** a client needs real-time IM events
- **THEN** the documented default endpoint SHALL identify the notification-service WebSocket host, the configured path, and the supported token delivery methods
- **AND** the documentation SHALL state whether `/api/websocket/**` through gateway is an accepted runtime entry

#### Scenario: Missing token is rejected before protocol upgrade
- **WHEN** a client opens the default WebSocket endpoint without `Authorization` header and without `token` query parameter
- **THEN** the server SHALL return `401 Unauthorized`
- **AND** the server SHALL close the channel before binding a WebSocket user id

#### Scenario: Invalid token is rejected before protocol upgrade
- **WHEN** a client opens the default WebSocket endpoint with an invalid token
- **THEN** the server SHALL return `401 Unauthorized`
- **AND** the server SHALL close the channel before binding a WebSocket user id

#### Scenario: Allowed token is bound to the channel
- **WHEN** a client opens the default WebSocket endpoint with a valid token in `Authorization: Bearer <token>` or `token` query parameter
- **THEN** the server SHALL bind the resolved login id to the WebSocket channel
- **AND** the request SHALL continue to the WebSocket protocol upgrade handler

#### Scenario: Disallowed Origin is rejected when allowlist is configured
- **WHEN** `websocket.allowed-origins` contains one or more origins
- **AND** a client opens the default WebSocket endpoint with an `Origin` header not present in the allowlist
- **THEN** the server SHALL return `403 Forbidden`
- **AND** the server SHALL close the channel before binding a WebSocket user id

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
