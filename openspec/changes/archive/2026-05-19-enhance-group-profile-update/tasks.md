## 1. OpenSpec 与自审

- [x] 1.1 创建 `enhance-group-profile-update` change。
- [x] 1.2 明确本次只做群主更新群名称、头像和公告，不提前实现管理员/踢人/入群审核。
- [x] 1.3 运行 `openspec validate enhance-group-profile-update --strict`。
- [x] 1.4 使用 Hermes 只读复核范围、测试和实现一致性。

## 2. TDD 红灯

- [x] 2.1 为 `ChatRoomServiceImplTest` 增加私聊拒绝、非群主拒绝、空 payload 拒绝。
- [x] 2.2 为群主更新成功增加群资料落库和成员会话刷新测试。
- [x] 2.3 为 `chat_group_info` 缺失时的公告单独更新增加默认群名/头像保护测试，并确认实现前失败。
- [x] 2.4 按 Hermes 复核补充已有脏 `chat_group_info` 字段、空头像和推送失败高可用红灯测试。

## 3. 最小实现

- [x] 3.1 新增 `ChatRoomUpdateRequest`。
- [x] 3.2 新增 `POST /chat/room/update` Controller 入口。
- [x] 3.3 新增 `ChatRoomService#updateGroupProfile` 并实现参数、房间类型、群主权限和 payload 校验。
- [x] 3.4 更新 `chat_room` 与 `chat_group_info`，并在扩展记录缺失时继承当前群名和头像。
- [x] 3.5 更新成功后向房间成员发送会话刷新事件。
- [x] 3.6 按 Hermes 复核修复已有扩展记录空字段兜底，并确保会话刷新失败不破坏群资料事实更新。

## 4. 验证与归档

- [x] 4.1 运行 `ChatRoomServiceImplTest` 相关测试。
- [x] 4.2 运行 chat-service 模块回归。
- [x] 4.3 运行 `openspec validate --all --strict`。
- [x] 4.4 归档本次 OpenSpec change 并再次运行 `openspec validate --all --strict`。
- [x] 4.5 更新 `task_plan.md`、`findings.md`、`progress.md` 并提交。
