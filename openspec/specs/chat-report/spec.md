# chat-report Specification

## Purpose
TBD - created by archiving change add-chat-report-mvp. Update Purpose after archive.
## Requirements
### Requirement: 用户可以提交最小举报
The system SHALL allow authenticated users to submit reports for users, messages, and visible moments.

#### Scenario: 举报可见动态
- **WHEN** 用户举报一条自己可见且未删除的动态
- **THEN** 系统创建一条待处理举报记录
- **AND** 举报记录保存动态作者作为对象归属用户

#### Scenario: 举报公开动态
- **WHEN** 用户举报一条公开、审核通过且未删除的动态
- **AND** 用户与动态作者不存在任一方向拉黑
- **THEN** 系统创建一条待处理举报记录
- **AND** 举报记录保存动态作者作为对象归属用户

#### Scenario: 拉黑关系下举报公开动态被拒绝
- **WHEN** 用户与公开动态作者存在任一方向拉黑
- **THEN** 系统返回无权限错误
- **AND** 不创建举报记录

