## ADDED Requirements

### Requirement: 视频消息应使用稳定 extra 契约
The system SHALL allow room members to send video messages with bounded structured metadata.

#### Scenario: 视频消息 extra 合法
- **WHEN** a room member sends a video message
- **AND** `extra.url` and `extra.format` are not blank
- **AND** `extra.duration`, `extra.size`, `extra.width`, and `extra.height` are positive numbers
- **THEN** the system accepts the message
- **AND** the stored message content can use the `[视频]` preview placeholder

#### Scenario: 视频消息 extra 非法
- **WHEN** a room member sends a video message
- **AND** required text or numeric metadata is blank, missing, non-numeric, zero, or negative
- **THEN** the system rejects the message
- **AND** no message is persisted or pushed
