## 1. OpenSpec 与范围

- [x] 1.1 创建 `integrate-friend-notification-center` change。
- [x] 1.2 明确本次只接入好友申请和好友通过通知，不扩展群邀请或系统广播。
- [x] 1.3 运行 `openspec validate integrate-friend-notification-center --strict`。

## 2. TDD 红灯

- [x] 2.1 增加好友申请成功时创建通知中心记录测试。
- [x] 2.2 增加好友申请通知失败不回滚申请事实测试。
- [x] 2.3 增加好友通过成功时创建通知中心记录测试。
- [x] 2.4 增加好友通过通知失败不回滚好友关系、私聊房间和 WebSocket 事件测试。
- [x] 2.5 运行目标测试，确认红灯来自缺失通知中心接入。

## 3. 最小实现

- [x] 3.1 在 `UserFriendApplyServiceImpl` 注入 `NotificationFeignClient`。
- [x] 3.2 申请成功后创建接收人为目标用户、`relatedType=user_friend_apply`、`relatedId=applyId` 的 `user` 类型通知。
- [x] 3.3 通过成功后创建接收人为申请人、`relatedType=user_friend_apply`、`relatedId=applyId` 的 `user` 类型通知。
- [x] 3.4 通知调用使用直接 try/catch 降级，不影响好友业务事实。

## 4. 验证与归档

- [x] 4.1 运行 `UserFriendApplyServiceImplTest`。
- [x] 4.2 运行 chat + notification 组合回归。
- [x] 4.3 运行 `openspec validate --all --strict`。
- [x] 4.4 更新验收文档和 planning files。
- [x] 4.5 归档本次 OpenSpec change 并再次运行 `openspec validate --all --strict`。
