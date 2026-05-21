## ADDED Requirements

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
