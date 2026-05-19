## 1. OpenSpec 与自审

- [x] 1.1 创建 `enhance-friend-discovery-relationship` change。
- [x] 1.2 编写 proposal/design/spec delta，限定范围为好友发现与关系状态增强，不跨越备注/拉黑。
- [x] 1.3 运行 `openspec validate enhance-friend-discovery-relationship --strict`。
- [x] 1.4 向 Hermes 提交只读审阅（本地确认边界后继续）

## 2. TDD 红灯

- [x] 2.1 为 `UserFriendServiceImplTest` 增加关系状态查询失败/边界场景（自己、好友、待处理、互不认识、参数缺失）。
- [x] 2.2 为 `UserFriendServiceImplTest` 增加删除好友边界场景（空参数、自己、幂等）。
- [x] 2.3 为 `ChatFriendController` 对应服务能力添加关系状态映射与发现查询测试（若采用 service-first，需补齐失败预期的关键单测）。

## 3. 最小实现

- [x] 3.1 新增 `friendStatus` 字段到 `ChatFriendUserVO`。
- [x] 3.2 在 `ChatFeignClient` 与 `ChatFriendController` 中新增：
  - `GET /friend/search`
  - `DELETE /friend/delete`
- [x] 3.3 新增 `UserFriendService#getFriendshipStatus`：基于 `user_friend` 与 `user_friend_apply` 计算关系状态。
- [x] 3.4 `listFriends` 与 `search` 路径返回关系状态（列表/分页）。
- [x] 3.5 `/chat/friend/add` 维持禁用（非目标范围）。

## 4. 验证与归档

- [x] 4.1 运行新新增红绿测试，至少覆盖 chat-service 相关接口。
- [x] 4.2 运行 `openspec validate --all --strict`。
- [x] 4.3 归档本次 OpenSpec change 并再次运行 `openspec validate --all --strict`。
- [x] 4.4 更新 `progress.md`/`findings.md`。
