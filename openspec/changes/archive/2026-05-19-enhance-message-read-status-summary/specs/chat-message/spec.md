# chat-message Delta

## ADDED Requirements

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
