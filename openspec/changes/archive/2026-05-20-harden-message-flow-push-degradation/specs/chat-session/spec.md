## ADDED Requirements

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
