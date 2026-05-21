## ADDED Requirements

### Requirement: 表情贴纸消息应使用稳定 extra 契约
The system SHALL allow room members to send sticker messages using lightweight structured metadata.

#### Scenario: 表情贴纸消息 extra 合法
- **WHEN** a room member sends a sticker message
- **AND** `extra.stickerId`, `extra.name`, and `extra.url` are not blank
- **THEN** the system accepts the message
- **AND** the stored message content can use the `[表情]` preview placeholder

#### Scenario: 表情贴纸消息 extra 非法
- **WHEN** a room member sends a sticker message
- **AND** `extra.stickerId`, `extra.name`, or `extra.url` is blank or missing
- **THEN** the system rejects the message
- **AND** no message is persisted or pushed
