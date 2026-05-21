## ADDED Requirements

### Requirement: 语音消息应使用稳定 extra 契约
The system SHALL allow room members to send voice messages with bounded structured metadata.

#### Scenario: 语音消息 extra 合法
- **WHEN** a room member sends a voice message
- **AND** `extra.url` and `extra.format` are not blank
- **AND** `extra.duration` and `extra.size` are positive numbers
- **THEN** the system accepts the message
- **AND** the stored message content can use the `[语音]` preview placeholder

#### Scenario: 语音消息 extra 非法
- **WHEN** a room member sends a voice message
- **AND** `extra.url` or `extra.format` is blank, or `extra.duration` or `extra.size` is missing, non-numeric, zero, or negative
- **THEN** the system rejects the message
- **AND** no message is persisted or pushed
