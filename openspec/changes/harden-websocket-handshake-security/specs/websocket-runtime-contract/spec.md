## MODIFIED Requirements

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
