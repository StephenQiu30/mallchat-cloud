## 1. Tests First

- [x] 1.1 `ChatMomentServiceImplTest` 覆盖默认好友可见与公开动态发布。
- [x] 1.2 `ChatMomentServiceImplTest` 覆盖公开广场只返回公开、正常、审核通过动态。
- [x] 1.3 `ChatMomentServiceImplTest` 覆盖公开动态非好友互动边界。

## 2. Implementation

- [x] 2.1 扩展 `ChatMomentPublishRequest`、`ChatMoment`、`ChatMomentVO`。
- [x] 2.2 新增 `ChatMomentService#listPublicMoments` 与 Controller 入口。
- [x] 2.3 同步 `chat_moment` DDL 与 m9 migration。

## 3. Validation

- [x] 3.1 m9 focused RED/GREEN 测试通过。
- [x] 3.2 相关模块测试通过。
- [x] 3.3 `openspec validate --all --strict` 通过。
