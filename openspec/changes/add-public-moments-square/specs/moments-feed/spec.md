## MODIFIED Requirements

### Requirement: 系统应支持发布文字或图片动态
The system SHALL allow an authenticated user to publish a moment with text content, image media, or both, and MAY mark the moment as friend-visible or public.

#### Scenario: 发布文字动态
- **WHEN** 用户以 `POST /chat/moment/publish` 提交合法正文
- **THEN** 系统创建一条 `chat_moment` 记录
- **AND** 返回动态 ID
- **AND** 不创建 `chat_message`
- **AND** 不更新 `chat_session` 未读数

#### Scenario: 发布公开动态
- **WHEN** 用户以 `visibility=1` 发布合法动态
- **THEN** 系统保存该动态为公开可见
- **AND** 审核状态默认为通过

#### Scenario: 非法可见范围被拒绝
- **WHEN** 用户提交非 0 或 1 的 `visibility`
- **THEN** 系统返回参数错误

### Requirement: 系统应支持好友可见动态流
The system SHALL return a paginated moments timeline that includes the viewer's own moments and mutual friends' moments only, excluding blocked friend relationships and audit-failed moments.

#### Scenario: 审核未通过动态不可进入好友动态流
- **WHEN** 一条动态审核状态不是通过
- **THEN** 好友动态流不返回该动态

## ADDED Requirements

### Requirement: 系统应支持动态公开广场 MVP
The system SHALL provide a public moments list that returns public, active, audit-passed moments.

#### Scenario: 查询公开动态广场
- **WHEN** 登录用户查询 `GET /chat/moment/public/list`
- **THEN** 系统返回公开、正常、审核通过且未删除的动态
- **AND** 不要求动态作者与查看者互为好友

#### Scenario: 好友可见动态不进入公开广场
- **WHEN** 一条动态为好友可见
- **THEN** 公开广场不返回该动态

#### Scenario: 审核未通过动态不进入公开广场
- **WHEN** 一条公开动态审核状态不是通过
- **THEN** 公开广场不返回该动态

#### Scenario: 公开动态允许非好友互动
- **WHEN** 用户点赞或评论一条公开、正常、审核通过的动态
- **AND** 用户与作者不是好友
- **THEN** 系统按可见动态处理本次互动

#### Scenario: 拉黑关系下公开动态不可见
- **WHEN** 用户与公开动态作者存在任一方向拉黑
- **THEN** 公开广场不返回该动态
- **AND** 用户不能点赞或评论该动态
