## MODIFIED Requirements

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
