## Why

现有好友能力已支持“申请-审批-好友列表-私聊建房”闭环，但缺少一个高频的 QQ-like 入口：

- 通过用户发现入口（昵称/简介/手机号）找到候选用户。
- 申请侧与列表侧能感知关系状态（自己/自己已发起/已是好友/对方已发起待处理）。
- 以 API 形式支撑好友关系删除与关系查询的最小闭环。

这些能力是日常 IM 导航的基础，当前 `ChatFriendController` 仅有 `add`（禁用）和 `list`，`ChatFriendAddRequest` 也未被业务使用，导致“前端可发起申请但无法安全查找与撤销关系”形成体验断层。

## What Changes

- 在 `chat-friend` 范围内新增好友发现查询与关系增强：
  - 新增 `GET /chat/friend/search`：按关键词分页返回候选用户，并返回与当前用户的关系快照。
  - 新增 `DELETE /chat/friend/delete`：删除双向好友关系。
  - 复用 `GET /chat/friend/list/vo` 为好友列表；补充返回关系字段。
  - 补充 `/chat/friend` 侧关系状态字段（最小可执行）
- 采用用户服务分页查询能力作为关系发现事实来源（`UserFeignClient#listUserByPage`），保持现有数据模型。
- 通过 OpenSpec change、TDD、最小实现和回归测试闭环，避免一次性扩展到黑名单/推荐/分组。
- 本次不新增新表，不修改消息系统、不增加新事件模型。

## Capabilities

### New Capabilities

- `chat-friend`: 好友发现接口与关系状态返回。

### Modified Capabilities

- `chat-friend`: `listFriends` 与 `friend` 操作补齐关系状态字段。

## Impact

- 代码：
  - API DTO/VO 与 `ChatFeignClient`
  - `ChatFriendController` 与 `UserFriendService`
  - 关系查询与关系状态构建逻辑。
- 测试：
  - `UserFriendServiceImplTest`（关系状态、权限、删除幂等）
  - `ChatFriendApplyServiceImplTest` 保持现有申请行为边界
  - 视实现增加 `ChatFriendService`/`UserFriendService` 单测覆盖 `list/search/delete` 的关键路径。
- OpenSpec：新增本次变更到 `chat-friend` 规格。

## Non-Goals

- 拉黑、备注、分组、共同好友推荐、好友来源标记。
- 修改好友申请审批状态机制（`user_friend_apply` 仍为 pending/approved/ignored）。
- 新建“关注/粉丝”关系域。
