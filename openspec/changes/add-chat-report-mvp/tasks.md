## 1. OpenSpec

- [x] 1.1 创建 `add-chat-report-mvp` change。
- [x] 1.2 明确举报对象、幂等策略和非目标。
- [x] 1.3 运行 `openspec validate add-chat-report-mvp --strict`。

## 2. TDD

- [x] 2.1 先补 `ChatReportServiceImplTest`，覆盖消息举报成功。
- [x] 2.2 先补重复举报幂等返回既有 ID。
- [x] 2.3 先补非房间成员举报消息被拒绝。
- [x] 2.4 运行目标测试确认 RED。

## 3. Implementation

- [x] 3.1 新增 `ChatReport`、Mapper、Service 和 Controller。
- [x] 3.2 新增 `ChatReportSubmitRequest` 和举报对象类型枚举。
- [x] 3.3 复用用户、消息、动态和房间成员服务做对象校验。

## 4. Validation

- [x] 4.1 运行 m6 聚焦 Maven 测试。
- [x] 4.2 运行 OpenSpec strict 校验。
- [x] 4.3 同步 GitHub Issue #28。
