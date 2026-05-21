# chat-session

## Purpose

Define how MallChat session state follows message creation, unread count updates, and read-boundary changes.
## Requirements
### Requirement: Session state updates follow message lifecycle
The system SHALL update session last message, unread count, last read boundary, and activity time whenever room messages are produced or read.

#### Scenario: Increment unread for receivers
- **WHEN** a new room message is sent
- **THEN** the system updates receiver sessions with the latest message and increments unread count for users other than the sender

#### Scenario: Preserve sender session without unread growth
- **WHEN** a new room message is sent by a user
- **THEN** the sender's session is updated with the latest message and activity time without increasing unread count

#### Scenario: Reduce unread according to read boundary
- **WHEN** a user marks a room message as read
- **THEN** the system updates that user's session last-read boundary and recalculates unread state so only messages newer than the submitted boundary remain unread

#### Scenario: Ignore stale read boundary updates
- **WHEN** a user submits a read boundary that is older than or equal to the currently stored boundary
- **THEN** the system SHALL NOT increase unread count or move the stored last-read boundary backward

### Requirement: Session list exposes message cursor state
The system SHALL expose session message cursor fields so clients can distinguish the latest persisted message from the user's read boundary.

#### Scenario: Session item includes cursor fields
- **WHEN** an authenticated user queries their chat session list
- **THEN** each session item includes `lastMessageId`
- **AND** each session item includes `lastReadMessageId`

#### Scenario: Receive cursor and read cursor remain separate
- **WHEN** a client decides whether reconnect compensation is needed
- **THEN** it can compare its last received message id with the session `lastMessageId`
- **AND** it SHALL NOT need to treat `lastReadMessageId` as the reconnect compensation cursor

### Requirement: 群资料更新后成员会话应刷新

The system SHALL notify current room members to refresh session metadata after a group profile update succeeds.

#### Scenario: 群资料更新后推送会话刷新

- **WHEN** a group owner successfully updates group profile fields
- **THEN** the system sends a session update event to current room members
- **AND** the session update data reflects the persisted group profile state when a session exists for the member

#### Scenario: 会话刷新失败不破坏群资料事实数据

- **WHEN** a group profile update is persisted successfully
- **AND** session refresh push fails for a room member
- **THEN** the group profile update remains successful
- **AND** the push failure SHALL NOT roll back the persisted group profile data

### Requirement: 成员被移除后会话应删除

The system SHALL remove the kicked member's chat session and notify that member after a successful owner-initiated group member removal.

#### Scenario: 移除成员后删除目标成员会话

- **WHEN** a group owner successfully removes a member from a group room
- **THEN** the system removes the target member's session for that room
- **AND** the system sends a session delete event to the target member

#### Scenario: 会话删除推送失败不破坏成员移除事实

- **WHEN** a group owner successfully removes a member from a group room
- **AND** session delete push fails
- **THEN** the room membership removal remains successful
- **AND** the push failure SHALL NOT restore the removed membership

#### Scenario: 会话事实删除与成员移除保持事务一致

- **WHEN** chat session deletion throws a persistence exception during member removal
- **THEN** the member removal transaction fails
- **AND** the membership removal SHALL NOT be reported as successful

### Requirement: 会话操作推送失败应降级

The system SHALL keep persisted session operation facts successful even when realtime session push delivery fails.

#### Scenario: 置顶推送失败不回滚置顶状态

- **WHEN** a user updates a chat session top status
- **AND** the session-update realtime push fails
- **THEN** the persisted top status remains successful
- **AND** the push failure is degraded instead of failing the session operation

#### Scenario: 删除会话推送失败不回滚删除事实

- **WHEN** a user deletes a chat session
- **AND** the session-delete realtime push fails
- **THEN** the session delete result remains successful
- **AND** the push failure is degraded instead of failing the session operation

### Requirement: 消息主链路会话刷新推送失败应降级

The system SHALL keep message-flow business facts successful even when session refresh push delivery fails after a message read or recall operation.

#### Scenario: 已读后会话刷新失败不回滚已读事实

- **WHEN** a room member marks a message as read
- **AND** the member read boundary and session unread count are updated successfully
- **AND** the session-update realtime push fails
- **THEN** the read operation still succeeds
- **AND** the persisted read boundary and unread count remain updated
- **AND** the push failure is degraded instead of rolling back the read fact

#### Scenario: 撤回后成员会话刷新失败不回滚撤回事实

- **WHEN** a message sender recalls their message within the allowed recall window
- **AND** the message recall status is persisted successfully
- **AND** one or more member session-update realtime pushes fail
- **THEN** the recall operation still succeeds
- **AND** the persisted recall status remains updated
- **AND** the system continues attempting session refresh pushes for remaining members

#### Scenario: 消息发送后会话刷新失败不回滚消息事实

- **WHEN** a room member sends a valid chat message
- **AND** the message is persisted successfully
- **AND** session facts are updated for room members
- **AND** one or more session-update realtime pushes fail
- **THEN** the message send flow still succeeds
- **AND** the persisted message and session facts remain updated
- **AND** the system continues attempting session refresh pushes for remaining members

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

### Requirement: Session unread updates are idempotent for duplicate message events
The system SHALL NOT increment session unread counts more than once for the same persisted message id.

#### Scenario: Duplicate message event is applied to existing sessions
- **WHEN** session batch update receives a message id that is equal to or older than a session's current `lastMessageId`
- **THEN** the system SHALL keep that session's `lastMessageId`
- **AND** the system SHALL NOT increment that session's unread count again

### Requirement: 语音消息会话预览应稳定展示
The system SHALL show a stable placeholder preview for voice messages in session lists.

#### Scenario: 语音消息进入会话列表
- **WHEN** a session's latest normal message is a voice message
- **THEN** the session preview is `[语音]`

### Requirement: 视频消息会话预览应稳定展示
The system SHALL show a stable placeholder preview for video messages in session lists.

#### Scenario: 视频消息进入会话列表
- **WHEN** a session's latest normal message is a video message
- **THEN** the session preview is `[视频]`

### Requirement: 表情贴纸消息会话预览应稳定展示
The system SHALL show a stable placeholder preview for sticker messages in session lists.

#### Scenario: 表情贴纸消息进入会话列表
- **WHEN** a session's latest normal message is a sticker message
- **THEN** the session preview is `[表情]`

