## 1. OpenSpec

- [x] 1.1 创建 `add-friend-remark-group` change。
- [x] 1.2 明确好友备注和轻量分组的最小边界。
- [x] 1.3 运行 `openspec validate add-friend-remark-group --strict`。

## 2. TDD

- [x] 2.1 先补 `UserFriendServiceImplTest`，覆盖备注和分组更新。
- [x] 2.2 先补好友列表缺省分组返回 `默认分组`。
- [x] 2.3 运行目标测试确认 RED。

## 3. Implementation

- [x] 3.1 扩展 `UserFriend`、`ChatFriendUserVO` 和请求 DTO。
- [x] 3.2 更新好友资料写回当前用户单向 `user_friend` 记录。
- [x] 3.3 好友列表按分组可选过滤。

## 4. Validation

- [x] 4.1 运行 m6 聚焦 Maven 测试。
- [x] 4.2 运行 OpenSpec strict 校验。
- [ ] 4.3 同步 GitHub Issue #29。
