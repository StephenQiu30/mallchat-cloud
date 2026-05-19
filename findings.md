## 需求要点
- IM 后端要支撑 QQ-like 的好友发现与关系闭环。
- 在同一接口返回用户发现结果和关系状态，避免前端重复推导。
- 删除关系接口应具备幂等性，便于客户端重试和重复点击。

## 调研发现
- `mallchat-api-chat` 与 `mallchat-service/mallchat-chat-service` 已有 `UserFriend`、`UserFriendApply` 和 `ChatOnlineStatusService`，可复用为关系状态与在线状态来源。
- 现有 `UserFeignClient#listUserByPage` 提供分页搜索能力，可作为好友发现源，减少新 API 设计。
- `ChatFriendController` 当前仅有 `add`（禁用）与 `list`，缺少 discovery 及 delete 路径。
- `ChatCacheConstant` 在 chat-service 编译/测试中并非跨模块默认可用，需在 reactor 上下文（含 `mallchat-common-cache`）执行测试。

## 技术决策
| 决策 | 说明 |
|---|---|
| 复用现有用户服务分页搜索 | 最小化数据库/接口扩展，避免跨服务新边界 |
| 新增 `friendStatus` 而不改写申请模型 | 保持 `user_friend_apply` 审批语义不变 |
| 服务层先行测试（`UserFriendServiceImplTest`） | 符合仓库 TDD 与高可用优先覆盖约束 |
| 保持 `/chat/friend/add` 禁用 | 与现有审批链路保持一致，不引入直接加好友 |

## 风险与处理
- **关系状态语义一致性**：`friendStatus=1`（本人）不应出现在 search 结果，已通过 `notId` 过滤。
- **分页总数语义**：`searchFriends` 当前透传用户服务 `total`，避免改造 user-service 后的分页歧义。
- **高可用边界**：尚未新增 controller 集成测试；当前以 service 级别和 spec/openspec 约束保底。
- **测试覆盖边界**：未来可补充“缓存退化 + 删除后权限收敛”场景。

## 参考与证据
- `openspec/changes/enhance-friend-discovery-relationship/*`
- `mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/service/impl/UserFriendServiceImpl.java`
- `mallchat-service/mallchat-chat-service/src/test/java/com/stephen/cloud/chat/service/impl/UserFriendServiceImplTest.java`
- `docs/plans/PL-002-friend-discovery-and-relationship-phase-plan.md`

