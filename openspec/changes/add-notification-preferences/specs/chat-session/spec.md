## ADDED Requirements

### Requirement: 会话应支持免打扰状态
The system SHALL allow a room member to update a session-level mute status and SHALL expose that status in session list data.

#### Scenario: 开启或关闭会话免打扰
- **WHEN** a room member updates `muteStatus` to `0` or `1`
- **THEN** the system persists the status on the user's chat session
- **AND** returns the status in `ChatSessionVO`

#### Scenario: 免打扰更新推送失败降级
- **WHEN** the mute status is persisted successfully
- **AND** the session update realtime push fails
- **THEN** the persisted mute status remains successful
- **AND** the failure is degraded

#### Scenario: 非成员不可设置免打扰
- **WHEN** a user who is not a room member updates mute status for that room
- **THEN** the system rejects the request

#### Scenario: 免打扰不影响未读事实
- **WHEN** a muted receiver receives a new room message
- **THEN** the receiver session unread count still increases according to the normal message lifecycle
