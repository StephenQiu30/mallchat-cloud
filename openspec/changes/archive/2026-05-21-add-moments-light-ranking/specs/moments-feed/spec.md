## ADDED Requirements

### Requirement: 系统应支持公开动态轻量排序
The system SHALL order public moments by a simple, stable ranking that favors interaction and recent content after applying visibility and audit filters.

#### Scenario: 公开广场按轻量排序返回
- **WHEN** 用户查询公开动态广场
- **THEN** 系统先过滤公开、正常、审核通过且未删除的动态
- **AND** 再按点赞数、评论数、创建时间和 ID 倒序稳定排序

#### Scenario: 排序不绕过权限过滤
- **WHEN** 好友可见、已删除或审核未通过动态互动量更高
- **THEN** 公开广场仍不返回这些动态
