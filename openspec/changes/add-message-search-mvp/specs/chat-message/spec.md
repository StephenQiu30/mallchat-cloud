## ADDED Requirements

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
