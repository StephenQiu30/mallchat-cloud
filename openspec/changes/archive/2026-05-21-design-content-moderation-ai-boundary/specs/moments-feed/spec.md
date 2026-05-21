## ADDED Requirements

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
