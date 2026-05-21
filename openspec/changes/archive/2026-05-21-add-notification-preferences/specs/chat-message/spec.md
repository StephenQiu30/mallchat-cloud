## ADDED Requirements

### Requirement: 免打扰不影响消息事实和重连补偿
The system SHALL keep message persistence, session updates, and reconnect compensation independent from session mute status.

#### Scenario: 免打扰成员仍可通过历史消息获取消息
- **WHEN** a muted room member queries history or reconnect compensation
- **THEN** the system authorizes through room membership
- **AND** returns persisted messages normally

#### Scenario: 群消息推送过滤免打扰接收者
- **WHEN** a group member sends a message
- **AND** another receiver has muted the session
- **THEN** the realtime `CHAT_MESSAGE` push target list excludes the muted receiver
- **AND** the sent message remains persisted successfully
