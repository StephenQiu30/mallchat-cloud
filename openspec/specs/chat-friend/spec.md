# chat-friend

## Purpose

Define the MVP friendship flow for the chat domain, including friend application, approval or ignore handling, friend list exposure, and confirmed-friend private chat entry.
## Requirements
### Requirement: User can initiate a friend application
The system SHALL allow an authenticated user to submit a friend application to another user with a validation message, and SHALL persist the application in a pending state until it is handled.

#### Scenario: Submit a new friend application
- **WHEN** a user sends a friend application to another valid user who is not already a friend
- **THEN** the system creates a pending friend application record and returns success

#### Scenario: Reject duplicate friendship creation
- **WHEN** a user sends a friend application to a target user who is already in the user's friend list
- **THEN** the system rejects the request and SHALL NOT create a duplicate friendship or application

### Requirement: Target user can process a friend application
The system SHALL allow the target user of a pending friend application to approve or ignore the request, and SHALL update the application status exactly once.

#### Scenario: Approve a friend application
- **WHEN** the target user approves a pending friend application
- **THEN** the system marks the application as approved and creates mutual friendship records for both users

#### Scenario: Ignore a friend application
- **WHEN** the target user ignores a pending friend application
- **THEN** the system marks the application as ignored and SHALL NOT create friendship records

### Requirement: Friend relationship enables private chat discovery
The system SHALL expose the friend list of the current user and SHALL allow a private chat entry to be created or retrieved for a selected friend.

#### Scenario: Query friend list
- **WHEN** a user requests their friend list
- **THEN** the system returns the user's confirmed friends with profile information required for chat display

#### Scenario: Enter a private chat with a friend
- **WHEN** a user requests a private chat room for a confirmed friend
- **THEN** the system returns an existing private room if present or creates one stable private room for the user pair

### Requirement: 系统应支持用户发现候选并返回关系状态
The system SHALL allow authenticated users to search candidate users by keywords, return paginated results, and include a `friendStatus` relation snapshot for each candidate.

#### Scenario: 发现候选用户时返回关系状态
- **WHEN** 用户以 GET `/chat/friend/search` 进行关键词检索
- **THEN** 系统返回分页后的用户列表
- **AND** 每个结果项均包含 `friendStatus`
- **AND** `friendStatus` 只能为 0（陌生人）、2（已是好友）、3（我已发起待处理）、4（对方已发起待处理）
- **AND** 系统在搜索结果中排除当前登录用户，故结果不会出现 `friendStatus=1`

### Requirement: 系统应拒绝非法分页请求
The system SHALL reject invalid page parameters when searching candidates.

#### Scenario: 非法分页参数拒绝
- **WHEN** 用户以 `pageSize=0` 或 `pageSize=21` 调用 `/chat/friend/search`
- **THEN** 系统返回参数错误

### Requirement: 系统应在好友列表中稳定返回好友关系状态
The system SHALL return `friendStatus` on friend list entries so frontends can render consistent action states.

#### Scenario: 好友列表返回已是好友关系
- **WHEN** 当前用户查看好友列表
- **THEN** 列表中的每个好友项 `friendStatus` 为 2

#### Scenario: 关系状态应区分待处理方向
- **WHEN** 存在待处理申请
- **THEN** 系统区分申请方向并在双方可见视图返回不同状态值
- **AND** 发起方在列表/搜索中看到 `friendStatus=3`
- **AND** 被发起方在列表/搜索中看到 `friendStatus=4`

### Requirement: 系统应支持删除好友且保持幂等
The system SHALL provide deletion by API and SHALL be idempotent for repeated deletes against the same pair.

#### Scenario: 删除已存在好友关系
- **WHEN** 当前用户请求删除一个有效好友
- **THEN** 系统移除双向好友关系并返回成功

#### Scenario: 重复删除保持幂等
- **WHEN** 当前用户重复对同一对用户执行删除
- **THEN** 系统保持幂等语义（重复请求不会报错且结果保持一致）

