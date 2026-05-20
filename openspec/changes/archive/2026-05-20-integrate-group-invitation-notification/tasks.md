## 1. OpenSpec 与范围

- [x] 1.1 创建 `integrate-group-invitation-notification` change。
- [x] 1.2 明确本次只接入直接群邀请通知，不扩展审批式群邀请或群通知页。
- [x] 1.3 运行 `openspec validate integrate-group-invitation-notification --strict`。

## 2. TDD 红灯

- [x] 2.1 增加创建群时初始受邀成员创建通知中心记录测试。
- [x] 2.2 增加已有群邀请成员创建通知中心记录测试。
- [x] 2.3 增加群邀请通知失败不回滚成员、会话和 session update 事实测试。
- [x] 2.4 运行目标测试，确认红灯来自缺失通知中心接入。

## 3. 最小实现

- [x] 3.1 在 `ChatRoomServiceImpl` 注入 `NotificationFeignClient`。
- [x] 3.2 创建群初始邀请和已有群邀请成功后创建接收人为被邀请人、`relatedType=chat_room`、`relatedId=roomId` 的 `user` 类型通知。
- [x] 3.3 通知调用使用直接 try/catch 降级，不影响群成员和会话事实。

## 4. 验证与归档

- [x] 4.1 运行 `ChatRoomServiceImplTest`。
- [x] 4.2 运行 chat + notification 组合回归。
- [x] 4.3 运行 `openspec validate --all --strict`。
- [x] 4.4 更新验收文档和 planning files。
- [x] 4.5 归档本次 OpenSpec change 并再次运行 `openspec validate --all --strict`。
