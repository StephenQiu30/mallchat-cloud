## 1. Tests First

- [x] 1.1 `ChatMessageServiceImplTest` 覆盖成功转发。
- [x] 1.2 `ChatMessageServiceImplTest` 覆盖来源房间无权限、目标房间无权限和撤回消息。

## 2. Implementation

- [x] 2.1 新增转发请求 DTO 和 Controller endpoint。
- [x] 2.2 Service 复用现有发送链路，复制 type/content/extra。
- [x] 2.3 保持 clientMsgId 幂等语义。

## 3. Validation

- [x] 3.1 focused Maven tests 通过。
- [x] 3.2 `openspec validate --all --strict` 通过。
