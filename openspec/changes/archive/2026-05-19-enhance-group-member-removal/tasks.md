## 1. OpenSpec 与自审

- [x] 1.1 创建 `enhance-group-member-removal` change。
- [x] 1.2 明确范围只做群主移除普通成员，不实现管理员/禁言/入群审核/群主转让。
- [x] 1.3 运行 `openspec validate enhance-group-member-removal --strict`。
- [x] 1.4 使用 Hermes 只读复核范围、测试和实现一致性。

## 2. TDD 红灯

- [x] 2.1 为 `ChatRoomServiceImplTest` 增加非群聊拒绝、非群主拒绝。
- [x] 2.2 增加目标成员不存在拒绝、不能移除自己、不能移除群主测试。
- [x] 2.3 增加群主移除普通成员成功测试，断言调用 `leaveRoom`、删除会话并发送会话删除事件。
- [x] 2.4 增加会话删除推送失败不影响成员事实移除的高可用测试。
- [x] 2.5 按 Hermes 复核补管理员角色拒绝、拒绝路径无副作用和会话 DB 删除失败事务策略测试。

## 3. 最小实现

- [x] 3.1 新增群成员移除请求 DTO。
- [x] 3.2 新增 `POST /chat/room/member/remove` Controller 入口。
- [x] 3.3 新增 `ChatRoomService#removeMember` 并实现参数、房间类型、群主权限和目标成员校验。
- [x] 3.4 成功后复用 `ChatRoomMemberService#leaveRoom` 移除成员并删除目标成员会话。
- [x] 3.5 成功后发送会话删除事件；推送失败不得破坏成员事实移除。
- [x] 3.6 收紧角色边界：本次只允许移除 `MEMBER`，`OWNER` 与 `ADMIN` 均拒绝。

## 4. 验证与归档

- [x] 4.1 运行 `ChatRoomServiceImplTest` 相关测试。
- [x] 4.2 运行 chat-service 模块回归。
- [x] 4.3 运行 `openspec validate --all --strict`。
- [x] 4.4 归档本次 OpenSpec change 并再次运行 `openspec validate --all --strict`。
- [x] 4.5 更新 `task_plan.md`、`findings.md`、`progress.md` 并按 test/impl 拆分提交。
