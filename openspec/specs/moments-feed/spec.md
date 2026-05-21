# moments-feed Specification

## Purpose
TBD - created by archiving change add-moments-feed-mvp. Update Purpose after archive.
## Requirements
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

### Requirement: 系统应支持作者删除自己的动态
The system SHALL allow a moment author to delete their own moment and reject deletion attempts from other users.

#### Scenario: 作者删除自己的动态
- **WHEN** 作者以 `DELETE /chat/moment/delete` 删除自己的动态
- **THEN** 系统软删除该动态
- **AND** 后续动态流不再返回该动态

#### Scenario: 非作者删除被拒绝
- **WHEN** 非作者尝试删除动态
- **THEN** 系统返回无权限错误

#### Scenario: 重复删除保持幂等
- **WHEN** 作者重复删除同一条动态
- **THEN** 系统返回成功
- **AND** 动态仍保持删除状态

#### Scenario: 已删除动态不向非作者泄露归属
- **WHEN** 非作者尝试删除一条已删除动态
- **THEN** 系统拒绝请求
- **AND** 响应不应让调用方确认该动态归属

### Requirement: 系统应支持动态点赞和取消点赞
The system SHALL allow a user to like or unlike a visible moment with idempotent behavior.

#### Scenario: 好友点赞动态
- **WHEN** 用户点赞一条自己可见且未删除的动态
- **THEN** 系统创建一条 `chat_moment_like` 正常记录
- **AND** 对应 `chat_moment.like_count` 增加 1
- **AND** 点赞事实以 `moment_id + user_id` 唯一约束保持幂等

#### Scenario: 重复点赞保持幂等
- **WHEN** 用户重复点赞同一条已点赞动态
- **THEN** 系统返回成功
- **AND** 不重复增加 `like_count`
- **AND** 不重复创建点赞通知

#### Scenario: 唯一键冲突下点赞保持幂等
- **WHEN** 并发请求导致点赞插入触发 `moment_id + user_id` 唯一键冲突
- **THEN** 系统按已点赞处理并返回成功
- **AND** 不重复增加 `like_count`
- **AND** 不重复创建点赞通知

#### Scenario: 取消点赞
- **WHEN** 用户取消一条已经点赞的动态
- **THEN** 系统软删除对应点赞记录
- **AND** 对应 `chat_moment.like_count` 减少 1 且不低于 0

#### Scenario: 重复取消点赞保持幂等
- **WHEN** 用户取消一条未点赞或已取消点赞的动态
- **THEN** 系统返回成功
- **AND** 不降低现有 `like_count`

#### Scenario: 已取消点赞后再次点赞
- **WHEN** 用户重新点赞一条自己已取消点赞的动态
- **THEN** 系统恢复原点赞事实
- **AND** 对应 `chat_moment.like_count` 增加 1

### Requirement: 系统应支持动态一级评论
The system SHALL allow a user to add and list first-level comments on a visible moment.

#### Scenario: 好友评论动态
- **WHEN** 用户对一条自己可见且未删除的动态提交合法评论
- **THEN** 系统创建一条 `chat_moment_comment` 正常记录
- **AND** 对应 `chat_moment.comment_count` 增加 1
- **AND** 返回评论 ID

#### Scenario: 空评论被拒绝
- **WHEN** 用户提交空白评论
- **THEN** 系统返回参数错误

#### Scenario: 超长评论被拒绝
- **WHEN** 用户提交 trim 后超过 500 个字符的评论
- **THEN** 系统返回参数错误

#### Scenario: 分页查询评论
- **WHEN** 用户查询一条自己可见动态的评论列表
- **THEN** 系统按创建时间升序和 ID 升序返回未删除评论
- **AND** 不返回已删除评论

### Requirement: 系统应支持动态互动通知
The system SHALL create notification records for moment authors when other users like or comment on their moments.

#### Scenario: 点赞通知
- **WHEN** 用户点赞他人动态且本次点赞状态从未点赞变为已点赞
- **THEN** 系统创建 `like` 类型通知给动态作者
- **AND** 通知 `relatedType` 为 `chat_moment`
- **AND** 通知 `relatedId` 为动态 ID

#### Scenario: 评论通知
- **WHEN** 用户评论他人动态
- **THEN** 系统创建 `comment` 类型通知给动态作者
- **AND** 通知 `relatedType` 为 `chat_moment`
- **AND** 通知 `relatedId` 为动态 ID

#### Scenario: 自己互动自己的动态不通知
- **WHEN** 用户点赞或评论自己的动态
- **THEN** 系统不创建互动通知

#### Scenario: 通知失败不回滚互动事实
- **WHEN** 点赞或评论事实已经保存但通知服务调用失败
- **THEN** 系统保留互动事实和计数变化
- **AND** 本次接口仍按互动成功返回

#### Scenario: 通知调用保持轻量降级
- **WHEN** 点赞或评论事实与计数更新成功
- **THEN** 系统尝试创建互动通知
- **AND** 通知创建失败不应回滚点赞或评论事实

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

### Requirement: 系统应支持公开动态轻量排序
The system SHALL order public moments by a simple, stable ranking that favors interaction and recent content after applying visibility and audit filters.

#### Scenario: 公开广场按轻量排序返回
- **WHEN** 用户查询公开动态广场
- **THEN** 系统先过滤公开、正常、审核通过且未删除的动态
- **AND** 再按点赞数、评论数、创建时间和 ID 倒序稳定排序

#### Scenario: 排序不绕过权限过滤
- **WHEN** 好友可见、已删除或审核未通过动态互动量更高
- **THEN** 公开广场仍不返回这些动态

### Requirement: 系统应提供动态内容审核状态边界
The system SHALL store a minimal audit status for moments so public discovery can exclude audit-failed content without depending on an external AI provider.

#### Scenario: 发布动态默认审核通过
- **WHEN** 用户发布合法动态
- **THEN** 系统保存动态审核状态为通过
- **AND** 不调用外部 AI 服务作为同步依赖

#### Scenario: 审核未通过动态不可公开发现或互动
- **WHEN** 一条动态审核状态不是通过
- **THEN** 公开广场不返回该动态
- **AND** 用户不能点赞或评论该动态

#### Scenario: 审核状态不复用生命周期状态
- **WHEN** 系统判断动态是否删除
- **THEN** 仍使用 `status` 与 `is_delete`
- **AND** 审核状态只用于内容治理和展示过滤
