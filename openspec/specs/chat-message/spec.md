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

### Requirement: 图片和文件消息 extra 应满足稳定元数据契约

The system SHALL validate existing image and file message `extra` metadata before persisting the message.

#### Scenario: 图片消息 extra 合法

- **WHEN** a room member sends an image message
- **AND** `extra.url` is not blank
- **AND** `extra.width`, `extra.height`, and `extra.size` are positive numbers
- **THEN** the system accepts the message
- **AND** the session preview remains `[图片]`

#### Scenario: 图片消息 extra 非法

- **WHEN** a room member sends an image message
- **AND** `extra.url` is blank or any of `extra.width`, `extra.height`, `extra.size` is missing, non-numeric, zero, or negative
- **THEN** the system rejects the message
- **AND** no message is persisted or pushed

#### Scenario: 文件消息 extra 合法

- **WHEN** a room member sends a file message
- **AND** `extra.url`, `extra.name`, and `extra.ext` are not blank
- **AND** `extra.size` is a positive number
- **THEN** the system accepts the message
- **AND** the session preview remains `[文件]`

#### Scenario: 文件消息 extra 非法

- **WHEN** a room member sends a file message
- **AND** `extra.url`, `extra.name`, or `extra.ext` is blank, or `extra.size` is missing, non-numeric, zero, or negative
- **THEN** the system rejects the message
- **AND** no message is persisted or pushed

### Requirement: 消息主链路实时推送失败应降级

The system SHALL keep persisted message-flow facts successful even when realtime push delivery fails after the fact is written.

#### Scenario: 消息发送推送失败不回滚消息事实

- **WHEN** a room member sends a valid chat message
- **AND** the message is persisted successfully
- **AND** the chat-message realtime push fails
- **THEN** the send-message operation still returns the persisted message
- **AND** the push failure is degraded instead of rolling back the message fact

#### Scenario: 已读事件推送失败不回滚已读事实

- **WHEN** a room member marks a message as read
- **AND** the member read boundary and session unread count are updated successfully
- **AND** the message-read realtime push fails
- **THEN** the read operation still succeeds
- **AND** the persisted read boundary and unread count remain updated
- **AND** the push failure is degraded instead of rolling back the read fact

#### Scenario: 消息撤回推送失败不回滚撤回事实

- **WHEN** a message sender recalls their message within the allowed recall window
- **AND** the message recall status is persisted successfully
- **AND** the message-recall realtime push fails
- **THEN** the recall operation still succeeds
- **AND** the persisted recall status remains updated
- **AND** the push failure is degraded instead of rolling back the recall fact

### Requirement: 房间成员可以搜索文本消息
The system SHALL allow an authenticated room member to search normal text messages in a room they can access using bounded pagination.

#### Scenario: 成员搜索房间文本消息
- **WHEN** a room member searches messages with a non-blank keyword
- **THEN** the system returns matching normal text messages from that room
- **AND** the results are ordered by message id descending

#### Scenario: 非成员不可搜索房间消息
- **WHEN** a user who is not a room member searches messages in that room
- **THEN** the system rejects the request
- **AND** no room messages are returned

#### Scenario: 空关键词被拒绝
- **WHEN** a room member searches with a blank keyword
- **THEN** the system rejects the request as invalid

#### Scenario: 撤回删除和非文本消息不进入搜索结果
- **WHEN** a room contains recalled, deleted, image, or file messages
- **THEN** the search result contains only normal text messages matching the keyword

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

### Requirement: 拉黑关系阻断私聊发送
The system SHALL reject private chat message sends when the sender and peer have an active block relation in either direction.

#### Scenario: 私聊发送前发现拉黑关系
- **WHEN** 用户在私聊房间发送消息
- **AND** 私聊双方任一方向存在拉黑关系
- **THEN** 系统拒绝发送
- **AND** 不创建消息记录
- **AND** 不推送实时消息事件

### Requirement: Message client idempotency survives duplicate delivery races
The system SHALL keep a single message fact for repeated sends with the same sender and client message id.

#### Scenario: Duplicate client message id is already persisted
- **WHEN** a user sends a message with a `clientMsgId` that already exists for that sender
- **THEN** the system SHALL return the existing message view
- **AND** the system SHALL NOT persist or push a second message fact

#### Scenario: Duplicate client message id wins the database race
- **WHEN** two requests with the same sender and `clientMsgId` pass the pre-insert lookup concurrently
- **AND** the database unique key rejects one insert
- **THEN** the rejected request SHALL reload and return the existing message view
- **AND** the rejected request SHALL NOT push another realtime message event

