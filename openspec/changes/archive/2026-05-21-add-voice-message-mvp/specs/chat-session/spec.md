## ADDED Requirements

### Requirement: 语音消息会话预览应稳定展示
The system SHALL show a stable placeholder preview for voice messages in session lists.

#### Scenario: 语音消息进入会话列表
- **WHEN** a session's latest normal message is a voice message
- **THEN** the session preview is `[语音]`
