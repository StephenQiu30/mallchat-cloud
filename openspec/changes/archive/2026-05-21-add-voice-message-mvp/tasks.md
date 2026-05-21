## 1. Tests First

- [x] 1.1 `ChatMessageHelperTest` 覆盖合法/非法语音 extra。
- [x] 1.2 `ChatSessionServiceImplTest` 覆盖语音会话预览。
- [x] 1.3 `FileUploadValidatorTest` 覆盖 `chat_voice` 上传边界。

## 2. Implementation

- [x] 2.1 扩展 `ChatMessageTypeEnum` 与 `ChatMessageHelper`。
- [x] 2.2 扩展文件上传 biz 和 validator。
- [x] 2.3 同步 Swagger/DTO 文案。

## 3. Validation

- [x] 3.1 focused Maven tests 通过。
- [x] 3.2 `openspec validate --all --strict` 通过。
