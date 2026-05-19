## ADDED Requirements

### Requirement: Room messages can be queried after a receive cursor
The system SHALL provide a room message query that returns persisted messages newer than a client's receive cursor.

#### Scenario: Query newer messages by message id
- **WHEN** a room member queries messages with `afterMessageId`
- **THEN** the system filters messages to the same room with ids greater than `afterMessageId`
- **AND** the system returns the messages in ascending message id order

#### Scenario: Limit reconnect compensation window
- **WHEN** a room member queries messages after a receive cursor
- **AND** the requested limit is missing, invalid, or above the supported maximum
- **THEN** the system applies a bounded default or maximum limit before querying messages

#### Scenario: Keep history paging direction unchanged
- **WHEN** a room member uses the existing history-message endpoint
- **THEN** the system continues to return older messages before `lastMessageId`
- **AND** the reconnect compensation query SHALL NOT change existing history paging semantics
