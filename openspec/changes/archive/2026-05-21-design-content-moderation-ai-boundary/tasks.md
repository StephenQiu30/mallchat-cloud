## 1. Tests First

- [x] 1.1 `ChatMomentServiceImplTest` 覆盖发布默认审核通过。
- [x] 1.2 `ChatMomentServiceImplTest` 覆盖审核未通过动态不可公开发现。
- [x] 1.3 `ChatMomentServiceImplTest` 覆盖审核未通过动态不可互动。
- [x] 1.4 `ChatReportServiceImplTest` 覆盖公开动态举报与拉黑边界。

## 2. Implementation / Design

- [x] 2.1 `chat_moment.audit_status` 支持动态发现最小审核边界。
- [x] 2.2 `design.md` 写清 AI 接入非目标与后续扩展点。
- [x] 2.3 保持消息和文件审核为后续 change，不提前改表。
- [x] 2.4 公开动态举报复用现有 `chat_report` 闭环，不新增审核平台。

## 3. Validation

- [x] 3.1 m9 focused RED/GREEN 测试通过。
- [x] 3.2 相关模块测试通过。
- [x] 3.3 `openspec validate --all --strict` 通过。
