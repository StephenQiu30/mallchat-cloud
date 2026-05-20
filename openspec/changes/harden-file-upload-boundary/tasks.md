## 1. OpenSpec

- [x] 1.1 创建 `harden-file-upload-boundary` change。
- [x] 1.2 明确文件上传安全边界 MVP。
- [x] 1.3 运行 `openspec validate harden-file-upload-boundary --strict`。

## 2. TDD

- [x] 2.1 先补 `FileUploadValidatorTest`。
- [x] 2.2 运行 file-service 目标测试确认 RED。

## 3. Implementation

- [x] 3.1 增加轻量 `FileUploadValidator`。
- [x] 3.2 在文件上传进入 COS 前执行校验，并保持失败审计。
- [x] 3.3 保持 COS key 不拼接原始文件名。

## 4. Validation

- [x] 4.1 运行 file-service 目标测试。
- [x] 4.2 运行 OpenSpec strict 校验。
- [ ] 4.3 同步 GitHub Issue #19。
