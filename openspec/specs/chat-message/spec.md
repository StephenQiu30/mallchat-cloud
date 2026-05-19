# chat-message

## Purpose

Define room message read progress, unread clearing, and read-boundary behavior for the MallChat chat domain.
## Requirements
### Requirement: Room members can publish read progress
The system SHALL allow a room member to mark messages as read, SHALL persist the submitted read boundary for that room member, and SHALL publish a message-read event that other clients can use to update read state.

#### Scenario: Mark room messages as read
- **WHEN** a room member submits a read request with a target message in a room they belong to
- **THEN** the system updates the member's read boundary to that message and emits a room read event referencing the room and read boundary

#### Scenario: Clear unread state after reading the newest message
- **WHEN** a user marks a session's newest message as read
- **THEN** the system updates the session unread state so the unread count no longer includes messages up to that newest boundary

#### Scenario: Preserve unread state after partial read
- **WHEN** a user marks a message as read that is older than the session's newest message
- **THEN** the system updates the stored read boundary but SHALL preserve unread state for newer messages beyond that boundary

### Requirement: Room messages can be queried after a receive cursor
The system SHALL provide a room message query that returns persisted messages newer than a client's receive cursor.

#### Scenario: Query newer messages by message id
- **WHEN** a room member queries messages with `afterMessageId`
- **THEN** the system filters messages to the same room with ids greater than `afterMessageId`
- **AND** the system returns the messages in ascending message id order

#### Scenario: Limit reconnect compensation window
- **WHEN** a room member queries messages after a receive cursor
- **AND** the requested limit is missing, invalid, or above the supported maximum
- **THEN** the system applies a bounded default or maximum limit before querying messages

#### Scenario: Keep history paging direction unchanged
- **WHEN** a room member uses the existing history-message endpoint
- **THEN** the system continues to return older messages before `lastMessageId`
- **AND** the reconnect compensation query SHALL NOT change existing history paging semantics

### Requirement: 消息应支持同房间引用回复

The system SHALL allow a room member to send a message that references an existing message in the same room.

#### Scenario: 发送同房间引用回复

- **WHEN** a room member sends a message with `replyMsgId`
- **AND** the referenced message exists in the same room
- **THEN** the system persists the new message with the submitted `replyMsgId`
- **AND** the returned message view includes `replyMsg`

#### Scenario: 跨房间引用被拒绝

- **WHEN** a room member sends a message with `replyMsgId`
- **AND** the referenced message belongs to another room
- **THEN** the system rejects the request
- **AND** no new message is persisted

#### Scenario: 引用不存在消息被拒绝

- **WHEN** a room member sends a message with `replyMsgId`
- **AND** the referenced message does not exist
- **THEN** the system rejects the request
- **AND** no new message is persisted

#### Scenario: 无发送权限的引用消息被拒绝

- **WHEN** a user sends a message with `replyMsgId`
- **AND** the user does not satisfy the room send permission
- **THEN** the system rejects the request
- **AND** no new message is persisted

### Requirement: 引用回复预览应稳定脱敏和展示发送者

The system SHALL provide a stable reply preview for referenced messages.

#### Scenario: 引用正常消息返回预览和发送者

- **WHEN** a message view references a normal message
- **THEN** `replyMsg.content` contains the referenced message preview
- **AND** `replyMsg.userName` contains the referenced message sender name when available

#### Scenario: 引用已撤回消息返回脱敏预览

- **WHEN** a message view references a recalled message
- **THEN** `replyMsg.content` is `该消息已被撤回`
- **AND** the original recalled message content SHALL NOT be exposed through the reply preview

#### Scenario: 历史脏数据跨房间引用不展示预览

- **WHEN** a message view references a message from another room
- **THEN** the returned message view SHALL NOT expose that cross-room `replyMsg` preview

### Requirement: 消息发送者应能查询已读统计摘要

The system SHALL allow a message sender to query aggregate read status for a message in a room without exposing member-level read lists.

#### Scenario: 发送者查询已读统计摘要

- **WHEN** a room member queries read status for a message they sent
- **AND** the message belongs to the requested room
- **THEN** the system returns `messageId` and `roomId`
- **AND** the system returns `totalCount`, `readCount`, and `unreadCount`

#### Scenario: 发送者默认视为已读

- **WHEN** the system calculates read status for a message
- **THEN** the message sender is counted as read when they are a current room member

#### Scenario: 非发送者查询已读统计被拒绝

- **WHEN** a room member queries read status for a message sent by another user
- **THEN** the system rejects the request
- **AND** the system SHALL NOT expose read or unread member details

#### Scenario: 非房间成员查询已读统计被拒绝

- **WHEN** a user who is not a room member queries read status
- **THEN** the system rejects the request

#### Scenario: 消息不存在被拒绝

- **WHEN** a user queries read status for a missing message
- **THEN** the system rejects the request

#### Scenario: 跨房间消息不泄露存在性

- **WHEN** a room member queries read status for a message id
- **AND** that message id does not belong to the requested room
- **THEN** the system rejects the request as if the message is not found
- **AND** the system SHALL NOT reveal whether the message id exists in another room

