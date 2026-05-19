## Context

`chat-friend` 已具备申请/审批/列表/私聊建房能力，但未对外提供发现入口。用户无法在 IM 端便捷找到目标用户，也无法把待处理申请状态回传到用户列表与候选列表。当前 `/chat/friend/add` 仍直接抛出业务禁用错误。

新增功能需复用现有模型与服务边界：

- 候选用户来源于 `mallchat-user-service`（`/user/list/page/vo`，内部 Feign `UserFeignClient#listUserByPage`）。
- 关系事实来源为 `user_friend` + `user_friend_apply`。
- 私聊权限、消息发送权限仍走现有 `isMutualFriend` 与 `chat-room` 约束。

## Decision

采用“复用 + 显式关系映射”策略：

1. 不新增数据库表，不改动现有好友事实模型。
2. 在 `chat-friend` 领域内新增“发现 + 关系状态”能力：
   - `friendStatus` 表示候选用户与当前用户关系（0-陌生人，1-自己，2-已是好友，3-待处理，4-对方待处理）。
   - `/chat/friend/search` 复用 `UserQueryRequest`（`searchText`）
     + 在查询前排除当前用户。
     + 对结果按 `friendStatus` 注入关系快照。
   - `/chat/friend/delete` 删除接口直接调用 `UserFriendService#removeFriend`，并返回幂等成功。
3. 限制范围为 MVP：
   - 仅添加 `friendStatus` 与删除关系。
   - 不引入黑名单、备注、分组。
4. 接口定义只改动 `chat-friend` 领域：
   - API DTO/VO 和 Feign 补齐新接口。

## Alternatives Considered

### 新增独立 `friend-discovery-service`
- 优势：域拆分清晰。
- 缺点：与当前 `chat` 现有好友链路重复，且新增服务治理与部署成本高。
- 结论：不采用，保持现有服务内聚。

### 前端自行拼接 user-service 接口构建关系状态
- 优势：后端变更少。
- 缺点：关系状态易被前端篡改或重复计算，违反统一契约。
- 结论：不采用。

### 继续保留 `/chat/friend/add` 并开放直接加好友
- 优势：兼容现有接口。
- 缺点：绕过申请流程，导致状态与审批链条不一致。
- 结论：本次不开放，保留禁用边界。

## API and Data Changes

### 1. API

- `ChatFriendUserVO` 新增 `friendStatus`。
   - 新增 `GET /chat/friend/search`（鉴权后的用户发现）与 `DELETE /chat/friend/delete`。
- `ChatFeignClient` 补齐上述接口。
- `UserFeignClient#listUserByPage` 已存在，本次不变。

### 2. Service

- `UserFriendService` 新增关系状态查询方法 `getFriendshipStatus(Long userId, Long targetUserId)`。
- `UserFriendServiceImpl` 增加查询逻辑：
  - `userId == target` => `1`
  - 任一方向待处理申请 => `3/4`
  - 已互为好友 => `2`
  - 其他 => `0`
- `ChatFriendController` 新增发现与删除接口，列表/搜索返回关系状态。

### 3. 测试

- 先补失败用例，再做最小实现：
  - 发现查询：关系状态返回正确、分页上限、排他当前用户。
  - 删除关系：未登录/未传 ID/删除自己均失败；不存在关系也幂等成功。
  - 关系状态：好友、待处理、空关系。

## Non-functional

- 保持 `chat-friend` 已有鉴权和日志方式；保留现有返回体风格。
- 保持 API 命名为 `chat-*` 风格和现有 VO/DTO 结构。
- 变更后运行 `openspec validate` 与 `mvn` 模块测试。
