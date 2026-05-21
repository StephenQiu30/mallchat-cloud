## 1. OpenSpec

- [x] 1.1 创建 `add-message-search-mvp` change。
- [x] 1.2 明确 DB LIKE MVP 和非目标。

## 2. TDD

- [x] 2.1 先补 `ChatMessageServiceImplTest` 搜索成功、非成员拒绝、空关键词拒绝。

## 3. Implementation

- [x] 3.1 `ChatMessageService` 新增 `searchMessages`。
- [x] 3.2 `ChatMessageController` 新增 `/chat/message/search/vo`。
- [x] 3.3 查询限制文本、正常状态、指定房间和有界分页。

## 4. Validation

- [x] 4.1 运行 m7 聚焦 Maven 测试。
- [x] 4.2 运行 chat/notification service 全量测试。
- [x] 4.3 同步 GitHub Issue #32。
