# chat-message Delta

## ADDED Requirements

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
