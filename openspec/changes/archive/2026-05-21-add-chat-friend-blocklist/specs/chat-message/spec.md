## ADDED Requirements

### Requirement: 拉黑关系阻断私聊发送
The system SHALL reject private chat message sends when the sender and peer have an active block relation in either direction.

#### Scenario: 私聊发送前发现拉黑关系
- **WHEN** 用户在私聊房间发送消息
- **AND** 私聊双方任一方向存在拉黑关系
- **THEN** 系统拒绝发送
- **AND** 不创建消息记录
- **AND** 不推送实时消息事件
