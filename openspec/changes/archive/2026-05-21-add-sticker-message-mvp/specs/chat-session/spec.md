## ADDED Requirements

### Requirement: 表情贴纸消息会话预览应稳定展示
The system SHALL show a stable placeholder preview for sticker messages in session lists.

#### Scenario: 表情贴纸消息进入会话列表
- **WHEN** a session's latest normal message is a sticker message
- **THEN** the session preview is `[表情]`
