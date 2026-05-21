## ADDED Requirements

### Requirement: 视频消息会话预览应稳定展示
The system SHALL show a stable placeholder preview for video messages in session lists.

#### Scenario: 视频消息进入会话列表
- **WHEN** a session's latest normal message is a video message
- **THEN** the session preview is `[视频]`
