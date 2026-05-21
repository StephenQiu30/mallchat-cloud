## ADDED Requirements

### Requirement: 用户可以提交最小举报
The system SHALL allow authenticated users to submit reports for users, messages, and moments.

#### Scenario: 举报有效用户
- **WHEN** 用户举报另一个存在的用户
- **THEN** 系统创建一条待处理举报记录
- **AND** 返回举报 ID

#### Scenario: 举报自己被拒绝
- **WHEN** 用户尝试举报自己
- **THEN** 系统返回参数错误
- **AND** 不创建举报记录

#### Scenario: 举报房间内消息
- **WHEN** 房间成员举报房间内存在的消息
- **THEN** 系统创建一条待处理举报记录
- **AND** 举报记录保存消息发送者作为对象归属用户

#### Scenario: 非房间成员举报消息被拒绝
- **WHEN** 非房间成员尝试举报某条消息
- **THEN** 系统返回无权限错误
- **AND** 不创建举报记录

#### Scenario: 举报可见动态
- **WHEN** 用户举报一条自己可见且未删除的动态
- **THEN** 系统创建一条待处理举报记录
- **AND** 举报记录保存动态作者作为对象归属用户

#### Scenario: 重复举报保持幂等
- **WHEN** 同一用户重复举报同一对象
- **THEN** 系统返回既有举报 ID
- **AND** 不创建重复举报记录
