# chat-friend

## Purpose

Define the MVP friendship flow for the chat domain, including friend application, approval or ignore handling, friend list exposure, and confirmed-friend private chat entry.
## Requirements
### Requirement: User can initiate a friend application
The system SHALL allow an authenticated user to submit a friend application to another user with a validation message, SHALL persist the application in a pending state until it is handled, and SHALL create a notification-center record for the target user when a new application is created.

#### Scenario: Submit a new friend application
- **WHEN** a user sends a friend application to another valid user who is not already a friend
- **THEN** the system creates a pending friend application record and returns success
- **AND** the system creates a `user` notification for the target user
- **AND** the notification `relatedType` is `user_friend_apply`
- **AND** the notification `relatedId` is the friend application ID

#### Scenario: Reject duplicate friendship creation
- **WHEN** a user sends a friend application to a target user who is already in the user's friend list
- **THEN** the system rejects the request and SHALL NOT create a duplicate friendship or application
- **AND** the system SHALL NOT create a notification-center record

#### Scenario: Return existing pending application without duplicate notification
- **WHEN** a user sends a friend application that matches an existing same-direction pending application
- **THEN** the system returns the existing application ID
- **AND** the system SHALL NOT create another notification-center record

#### Scenario: Friend application notification failure is degraded
- **WHEN** a new friend application is persisted successfully but notification-center creation fails
- **THEN** the friend application remains pending
- **AND** the existing friend-application WebSocket event remains attempted
- **AND** the application API still returns the application ID

#### Scenario: Friend application WebSocket push failure is degraded
- **WHEN** a new friend application is persisted successfully but the friend-application WebSocket/MQ push fails
- **THEN** the friend application remains pending
- **AND** the application API still returns the application ID
- **AND** the system still attempts to create the notification-center record

#### Scenario: Friend application notification is created after transaction commit
- **WHEN** a new friend application is persisted while a transaction synchronization is active
- **THEN** the system SHALL NOT create the notification-center record before commit
- **AND** the system creates the notification-center record after commit

### Requirement: Target user can process a friend application
The system SHALL allow the target user of a pending friend application to approve or ignore the request, SHALL update the application status exactly once, and SHALL create a notification-center record for the applicant when the application is approved.

#### Scenario: Approve a friend application
- **WHEN** the target user approves a pending friend application
- **THEN** the system marks the application as approved and creates mutual friendship records for both users
- **AND** the system creates a `user` notification for the applicant
- **AND** the notification `relatedType` is `user_friend_apply`
- **AND** the notification `relatedId` is the friend application ID

#### Scenario: Ignore a friend application
- **WHEN** the target user ignores a pending friend application
- **THEN** the system marks the application as ignored
- **AND** the system SHALL NOT create friendship records
- **AND** the system SHALL NOT create an approval notification

#### Scenario: Friend approval notification failure is degraded
- **WHEN** a pending friend application is approved and notification-center creation fails
- **THEN** the mutual friendship records and private room creation remain successful
- **AND** the existing friend-approval WebSocket event remains attempted
- **AND** the approval API still follows the friend approval result

#### Scenario: Friend approval WebSocket push failure is degraded
- **WHEN** a pending friend application is approved but the friend-approval WebSocket/MQ push fails
- **THEN** the mutual friendship records and private room creation remain successful
- **AND** the approval API still follows the friend approval result
- **AND** the system still attempts to create the notification-center record

#### Scenario: Friend approval notification is created after transaction commit
- **WHEN** a pending friend application is approved while a transaction synchronization is active
- **THEN** the system SHALL NOT create the notification-center record before commit
- **AND** the system creates the notification-center record after commit

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

### Requirement: 用户可以维护好友备注和轻量分组
The system SHALL allow an authenticated user to update their own friend remark and lightweight group name for a confirmed friend.

#### Scenario: 更新好友备注和分组
- **WHEN** 用户对一个已确认好友提交备注和分组名称
- **THEN** 系统只更新当前用户视角的好友关系记录
- **AND** 不修改对方用户资料或对方视角好友关系

#### Scenario: 非好友不能设置联系人资料
- **WHEN** 用户尝试为非好友设置备注或分组
- **THEN** 系统返回无权限错误
- **AND** 不创建好友资料记录

#### Scenario: 好友列表返回备注和分组
- **WHEN** 用户查询好友列表
- **THEN** 每个好友项返回 `remarkName`
- **AND** 每个好友项返回 `friendGroupName`
- **AND** 缺少分组名称时返回 `默认分组`

#### Scenario: 按轻量分组过滤好友列表
- **WHEN** 用户带 `friendGroupName` 查询好友列表
- **THEN** 系统只返回该分组下的好友

#### Scenario: 超长联系人资料被拒绝
- **WHEN** 用户提交超过长度限制的备注或分组名称
- **THEN** 系统返回参数错误
- **AND** 不更新好友资料

### Requirement: 用户可以拉黑和解除拉黑其他用户
The system SHALL allow an authenticated user to block or unblock another existing user with idempotent behavior.

#### Scenario: 拉黑有效用户
- **WHEN** 用户拉黑一个存在且不是自己的用户
- **THEN** 系统创建 `user_friend_block` 记录
- **AND** 重复拉黑同一用户保持成功且不创建重复记录

#### Scenario: 解除拉黑保持幂等
- **WHEN** 用户解除对一个用户的拉黑
- **THEN** 系统删除该方向的拉黑关系
- **AND** 重复解除拉黑保持成功

#### Scenario: 拉黑自己被拒绝
- **WHEN** 用户尝试拉黑自己
- **THEN** 系统返回参数错误

### Requirement: 拉黑关系阻断好友申请
The system SHALL reject friend applications and approvals when either direction has an active block relation.

#### Scenario: 拉黑后不能申请好友
- **WHEN** 用户 A 和用户 B 任一方向存在拉黑关系
- **AND** 用户 A 尝试向用户 B 发起好友申请
- **THEN** 系统拒绝申请
- **AND** 不创建新的好友申请记录

#### Scenario: 审批前出现拉黑关系
- **WHEN** 好友申请仍待处理
- **AND** 审批前双方任一方向出现拉黑关系
- **THEN** 系统拒绝通过该好友申请
- **AND** 不创建好友关系

