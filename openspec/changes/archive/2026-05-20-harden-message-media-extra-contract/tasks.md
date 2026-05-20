## 1. OpenSpec 与范围

- [x] 1.1 创建 `harden-message-media-extra-contract` change。
- [x] 1.2 明确本次只加固现有图片/文件消息，不新增消息类型或媒体处理能力。
- [x] 1.3 运行 `openspec validate harden-message-media-extra-contract --strict`。

## 2. TDD 红灯

- [x] 2.1 补图片 `extra` 合法、空 URL、非正数宽高/大小、非数字宽高/大小测试。
- [x] 2.2 补文件 `extra` 合法、空 URL/名称/扩展名、非正数大小、非数字大小测试。
- [x] 2.3 补发送非法图片/文件消息不保存、不推送测试。
- [x] 2.4 补图片/文件会话预览仍为 `[图片]` / `[文件]` 回归测试。

## 3. 最小实现

- [x] 3.1 拆分图片和文件 extra 校验逻辑，使用结构化 JSON 读取。
- [x] 3.2 字符串字段只接受非空内容。
- [x] 3.3 数值字段只接受正数，兼容 JSON number 与数字字符串。
- [x] 3.4 保持文本、预览、撤回和引用回复逻辑不变。

## 4. 验证与归档

- [x] 4.1 运行 `ChatMessageHelperTest`。
- [x] 4.2 运行 `ChatMessageServiceImplTest` 与 `ChatSessionServiceImplTest`。
- [x] 4.3 运行 chat-service 回归。
- [x] 4.4 运行 `openspec validate --all --strict`。
- [x] 4.5 归档 change，完成归档后 OpenSpec 校验、计划回填、提交并推送。
