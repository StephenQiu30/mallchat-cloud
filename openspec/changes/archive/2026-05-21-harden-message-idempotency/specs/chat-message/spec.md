## ADDED Requirements

### Requirement: Message client idempotency survives duplicate delivery races
The system SHALL keep a single message fact for repeated sends with the same sender and client message id.

#### Scenario: Duplicate client message id is already persisted
- **WHEN** a user sends a message with a `clientMsgId` that already exists for that sender
- **THEN** the system SHALL return the existing message view
- **AND** the system SHALL NOT persist or push a second message fact

#### Scenario: Duplicate client message id wins the database race
- **WHEN** two requests with the same sender and `clientMsgId` pass the pre-insert lookup concurrently
- **AND** the database unique key rejects one insert
- **THEN** the rejected request SHALL reload and return the existing message view
- **AND** the rejected request SHALL NOT push another realtime message event
