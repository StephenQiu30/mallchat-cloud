# Tasks

## 1. Spec and Review
- [x] 1.1 用 Hermes 只读审阅补偿游标、权限和测试边界
- [x] 1.2 编写 `chat-realtime-delivery`、`chat-message` 与 `chat-session` spec delta
- [x] 1.3 运行 `openspec validate harden-reconnect-message-compensation --strict`

## 2. TDD
- [x] 2.1 先写 `ChatMessageServiceImplTest` 红灯测试，覆盖 `afterMessageId` 补偿查询
- [x] 2.2 先写 `ChatSessionConvertTest` 红灯测试，覆盖会话游标映射
- [x] 2.3 记录红灯失败原因

## 3. Implementation
- [x] 3.1 在 `ChatMessageService` / `ChatMessageServiceImpl` 增加补偿查询方法
- [x] 3.2 在 `ChatMessageController` 和 `ChatFeignClient` 暴露补偿查询入口
- [x] 3.3 在 `ChatSessionVO` 暴露 `lastMessageId` 和 `lastReadMessageId`

## 4. Validation and Archive
- [x] 4.1 运行 chat-service 相关 Maven 测试
- [x] 4.2 运行 `openspec validate --all --strict`
- [x] 4.3 归档 OpenSpec change，并再次运行全量 strict 校验
- [x] 4.4 更新 planning 文件并提交本轮变更
