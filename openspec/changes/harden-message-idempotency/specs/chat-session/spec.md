## ADDED Requirements

### Requirement: Session unread updates are idempotent for duplicate message events
The system SHALL NOT increment session unread counts more than once for the same persisted message id.

#### Scenario: Duplicate message event is applied to existing sessions
- **WHEN** session batch update receives a message id that is equal to or older than a session's current `lastMessageId`
- **THEN** the system SHALL keep that session's `lastMessageId`
- **AND** the system SHALL NOT increment that session's unread count again
