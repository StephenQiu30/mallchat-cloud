# chat-message Delta

## ADDED Requirements

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
