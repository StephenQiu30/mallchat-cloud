## 1. Room Access Hardening

- [x] 1.1 收口 `/chat/room/join` 控制器入口，明确公开 join 在当前 MVP 下为不支持行为
- [x] 1.2 在 `ChatRoomService` / `ChatRoomServiceImpl` 中补齐房间类型与加入路径校验，禁止私聊房间手动加入
- [x] 1.3 确认群聊成员进入仅通过建群、邀请等受控路径，不再允许无授权直接写入房间成员
- [x] 1.4 为群聊越权加入、私聊第三人加入、越权读取房间后续数据补充回归测试

## 2. Read Boundary Consistency

- [x] 2.1 明确 `ChatRoomMember.lastReadMessageId` 与 `ChatSession.lastReadMessageId` 的边界推进语义
- [x] 2.2 调整 `ChatMessageServiceImpl.markMessageRead`，按已读边界更新会话未读数而不是直接清零
- [x] 2.3 确保旧读游标不会回退会话状态，也不会错误增加未读数
- [x] 2.4 为部分已读、全部已读、重复上报旧边界等场景补充测试

## 3. Presence Notification Hardening

- [x] 3.1 在好友域补充面向在线状态广播的好友 ID 查询能力
- [x] 3.2 复用现有好友缓存策略，实现缓存优先、数据库兜底、必要时回填缓存
- [x] 3.3 调整 `ChannelManager`，移除对好友缓存 key 的直接依赖，改为通过好友域解析通知对象
- [x] 3.4 为冷启动、缓存过期、空好友集合场景补充在线状态广播测试

## 4. Regression and Apply Readiness

- [x] 4.1 复查受影响的 controller / service / websocket 推送链路，确保客户端可见协议未发生破坏性变化
- [x] 4.2 汇总本次变更影响的 capability 与测试结果，确认 change 进入 apply-ready 状态
