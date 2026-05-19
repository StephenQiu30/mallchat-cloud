## ADDED Requirements

### Requirement: Session list exposes message cursor state
The system SHALL expose session message cursor fields so clients can distinguish the latest persisted message from the user's read boundary.

#### Scenario: Session item includes cursor fields
- **WHEN** an authenticated user queries their chat session list
- **THEN** each session item includes `lastMessageId`
- **AND** each session item includes `lastReadMessageId`

#### Scenario: Receive cursor and read cursor remain separate
- **WHEN** a client decides whether reconnect compensation is needed
- **THEN** it can compare its last received message id with the session `lastMessageId`
- **AND** it SHALL NOT need to treat `lastReadMessageId` as the reconnect compensation cursor
