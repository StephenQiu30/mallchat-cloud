# moments-feed Specification

## Purpose
TBD - created by archiving change add-moments-feed-mvp. Update Purpose after archive.
## Requirements
### Requirement: 系统应支持发布文字或图片动态
The system SHALL allow an authenticated user to publish a moment with text content, image media, or both.

#### Scenario: 发布文字动态
- **WHEN** 用户以 `POST /chat/moment/publish` 提交合法正文
- **THEN** 系统创建一条 `chat_moment` 记录
- **AND** 返回动态 ID
- **AND** 不创建 `chat_message`
- **AND** 不更新 `chat_session` 未读数

#### Scenario: 发布图片动态
- **WHEN** 用户提交包含图片 URL 的动态
- **THEN** 系统保存动态主体
- **AND** 系统按提交顺序保存 `chat_moment_media`

#### Scenario: 空动态被拒绝
- **WHEN** 用户提交空正文且无媒体的动态
- **THEN** 系统返回参数错误

#### Scenario: 超长正文被拒绝
- **WHEN** 用户提交 trim 后超过 1000 个字符的正文
- **THEN** 系统返回参数错误

#### Scenario: 非法媒体被拒绝
- **WHEN** 用户提交空 URL、超过 1024 个字符 URL、或超过 9 张图片的媒体列表
- **THEN** 系统返回参数错误

#### Scenario: 媒体保存失败回滚主体
- **WHEN** 动态主体保存成功但媒体保存失败
- **THEN** 系统回滚本次发布
- **AND** 不保留只有主体没有媒体的脏数据

### Requirement: 系统应支持好友可见动态流
The system SHALL return a paginated moments timeline that includes the viewer's own moments and mutual friends' moments only, excluding blocked friend relationships.

#### Scenario: 动态列表先按可见作者集合分页
- **WHEN** 用户查看动态流
- **THEN** 系统先确定可见作者集合
- **AND** 再按可见作者集合、未删除状态和发布时间倒序分页
- **AND** 分页总数不应包含不可见动态

#### Scenario: 作者查看自己的动态
- **WHEN** 用户查看动态流
- **THEN** 系统返回该用户自己的未删除动态

#### Scenario: 好友查看动态
- **WHEN** 用户 A 与用户 B 互为好友
- **AND** 用户 A 与用户 B 不存在任一方向拉黑关系
- **THEN** 用户 A 的动态流可以返回用户 B 的未删除动态

#### Scenario: 非好友不可见
- **WHEN** 用户 A 与用户 C 不是好友
- **THEN** 用户 A 的动态流不能返回用户 C 的动态

#### Scenario: 已拉黑好友不可见
- **WHEN** 用户 A 与用户 B 互为好友
- **AND** 用户 A 与用户 B 任一方向存在拉黑关系
- **THEN** 用户 A 的动态流不能返回用户 B 的动态

#### Scenario: 已删除动态不可见
- **WHEN** 一条动态已经被删除
- **THEN** 动态流不返回该动态

#### Scenario: 不可见动态不能互动
- **WHEN** 用户尝试点赞或评论非本人且非互为好友作者的动态
- **THEN** 系统返回无权限错误
- **AND** 不创建点赞、评论或通知事实

#### Scenario: 不可见动态不能查询评论
- **WHEN** 用户尝试查询非本人且非互为好友作者的动态评论列表
- **THEN** 系统返回无权限错误
- **AND** 不返回评论内容

#### Scenario: 不存在或已删除动态不能互动
- **WHEN** 用户尝试点赞、评论或查询一条不存在或已删除的动态
- **THEN** 系统返回错误
- **AND** 不创建互动事实
- **AND** 不创建通知

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
