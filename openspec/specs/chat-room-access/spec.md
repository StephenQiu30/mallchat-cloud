# chat-room-access

## Purpose

Define controlled room membership entry paths for MallChat private rooms and group rooms.
## Requirements
### Requirement: Room membership entry follows controlled MVP paths
The system SHALL allow room membership to be created only through controlled MVP entry paths, including group creation, group invitation, and private room initialization between confirmed friends; direct group invitations SHALL create notification-center records for invited members.

#### Scenario: Create a group room with initial members
- **WHEN** an authenticated user creates a new group room and specifies valid invited members
- **THEN** the system creates the room, adds the creator as owner, and adds only the invited members admitted through the controlled creation flow
- **AND** the system creates a `user` notification for each invited member
- **AND** each notification `relatedType` is `chat_room`
- **AND** each notification `relatedId` is the group room ID

#### Scenario: Invite a friend into an existing group room
- **WHEN** a room member invites a confirmed friend into an existing group room
- **THEN** the system creates room membership for that invited friend through the invitation flow
- **AND** the system creates a `user` notification for the invited friend
- **AND** the notification `relatedType` is `chat_room`
- **AND** the notification `relatedId` is the group room ID

#### Scenario: Group invitation notification waits for local transaction commit
- **WHEN** a direct group invitation is handled inside a local transaction
- **THEN** the system SHALL send the notification-center creation request only after the local transaction commits
- **AND** a rolled-back room, membership, or session transaction SHALL NOT leave a notification-center record behind

#### Scenario: Group invitation notification failure is degraded
- **WHEN** a direct group invitation creates membership and session facts but notification-center creation fails
- **THEN** the invited member remains in the room
- **AND** the invited member keeps the created or updated chat session
- **AND** the existing session update event remains attempted

#### Scenario: Initialize a private room for confirmed friends
- **WHEN** a user requests a private chat room with a confirmed friend
- **THEN** the system returns the existing stable private room or creates one private room and membership for the two confirmed friends only

### Requirement: Uncontrolled room join is rejected
The system SHALL reject uncontrolled direct room-join attempts that are not backed by a controlled MVP entry path.

#### Scenario: Reject direct join for a group room
- **WHEN** a user attempts to join a group room through an uncontrolled public join request
- **THEN** the system rejects the request and SHALL NOT create room membership

#### Scenario: Reject direct join for a private room
- **WHEN** a user attempts to join a private room through a manual join request
- **THEN** the system rejects the request and SHALL NOT create room membership

### Requirement: 群主应能更新群资料

The system SHALL allow only the group owner to update group profile fields for an existing group room.

#### Scenario: 群主更新群名称头像和公告

- **WHEN** a group owner submits a group profile update with one or more of `name`, `avatar`, or `announcement`
- **THEN** the system updates the corresponding group profile fields
- **AND** the room base profile remains consistent with the group profile extension

#### Scenario: 非群聊拒绝群资料更新

- **WHEN** a user submits a group profile update for a private room
- **THEN** the system rejects the request
- **AND** no group profile record is created or updated

#### Scenario: 非群主拒绝群资料更新

- **WHEN** a non-owner room member submits a group profile update
- **THEN** the system rejects the request
- **AND** no group profile record is created or updated

#### Scenario: 空更新内容被拒绝

- **WHEN** a group owner submits a group profile update without `name`, `avatar`, or `announcement`
- **THEN** the system rejects the request as invalid

#### Scenario: 群扩展记录缺失时保留基础群资料

- **WHEN** a group owner updates only the announcement
- **AND** the group profile extension record does not exist
- **THEN** the system creates the group profile extension record
- **AND** the created extension record keeps the current room name and avatar as defaults
- **AND** the created extension record stores the new announcement

#### Scenario: 已存在群扩展记录字段为空时修复基础群资料

- **WHEN** a group owner updates only the announcement
- **AND** the group profile extension record exists but its name or avatar is blank
- **THEN** the system repairs the blank group profile fields from the current room base profile
- **AND** the group profile extension record stores the new announcement

#### Scenario: 空头像被拒绝

- **WHEN** a group owner submits a group profile update with a blank `avatar`
- **THEN** the system rejects the request as invalid

### Requirement: 群主应能移除普通群成员

The system SHALL allow only the group owner to remove an existing ordinary `MEMBER` role user from a group room.

#### Scenario: 群主移除普通成员

- **WHEN** a group owner removes an existing ordinary member from a group room
- **THEN** the system removes the target user's room membership from the membership fact source
- **AND** the system removes the target user from the room member cache

#### Scenario: 非群聊拒绝移除成员

- **WHEN** a user submits a member removal request for a private room
- **THEN** the system rejects the request
- **AND** no room membership is removed

#### Scenario: 非群主拒绝移除成员

- **WHEN** a non-owner submits a member removal request
- **THEN** the system rejects the request
- **AND** no room membership is removed

#### Scenario: 目标成员不存在时拒绝

- **WHEN** a group owner removes a user who is not a current room member
- **THEN** the system rejects the request
- **AND** no session delete event is emitted for that target user

#### Scenario: 群主不能移除自己或其他群主账号

- **WHEN** a group owner attempts to remove themselves or a room owner account
- **THEN** the system rejects the request
- **AND** the owner membership remains unchanged

#### Scenario: 管理员角色不在本次移除范围内

- **WHEN** a group owner attempts to remove an `ADMIN` role member
- **THEN** the system rejects the request
- **AND** no room membership is removed
- **AND** no session delete event is emitted

### Requirement: 受控入群幂等不得覆盖既有成员角色

The system SHALL treat controlled membership creation as idempotent when the room member already exists, and SHALL NOT overwrite the existing member role through the add-member path.

#### Scenario: 重复邀请不得降级管理员

- **WHEN** an existing `ADMIN` group member is added again through a controlled membership path with `MEMBER` role
- **THEN** the system keeps the existing `ADMIN` role
- **AND** no role update is persisted

#### Scenario: 重复邀请不得降级群主

- **WHEN** an existing `OWNER` group member is added again through a controlled membership path with `MEMBER` role
- **THEN** the system keeps the existing `OWNER` role
- **AND** no role update is persisted

#### Scenario: 重复加入普通成员保持幂等

- **WHEN** an existing `MEMBER` group member is added again through a controlled membership path with `MEMBER` role
- **THEN** the system keeps the existing `MEMBER` role
- **AND** no role update is persisted
