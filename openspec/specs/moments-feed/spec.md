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
The system SHALL return a paginated moments timeline that includes the viewer's own moments and mutual friends' moments only.

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
- **THEN** 用户 A 的动态流可以返回用户 B 的未删除动态

#### Scenario: 非好友不可见
- **WHEN** 用户 A 与用户 C 不是好友
- **THEN** 用户 A 的动态流不能返回用户 C 的动态

#### Scenario: 已删除动态不可见
- **WHEN** 一条动态已经被删除
- **THEN** 动态流不返回该动态

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

