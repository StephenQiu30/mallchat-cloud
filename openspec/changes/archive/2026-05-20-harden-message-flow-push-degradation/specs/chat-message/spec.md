## ADDED Requirements

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
