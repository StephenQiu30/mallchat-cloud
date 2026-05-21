## 1. Tests First

- [x] 1.1 `ChatMomentServiceImplTest` 覆盖公开广场轻量排序。
- [x] 1.2 测试覆盖同互动分下按时间与 ID 稳定排序。

## 2. Implementation

- [x] 2.1 `pagePublicMoments` 按点赞数、评论数、创建时间和 ID 排序。
- [x] 2.2 为公开广场查询补充 `idx_public_rank` 索引。

## 3. Validation

- [x] 3.1 m9 focused RED/GREEN 测试通过。
- [x] 3.2 相关模块测试通过。
- [x] 3.3 `openspec validate --all --strict` 通过。
