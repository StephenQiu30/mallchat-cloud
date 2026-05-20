## 1. OpenSpec 与计划

- [x] 1.1 创建 `add-moments-feed-mvp` change。
- [x] 1.2 编写 proposal/design/spec delta，限定范围为动态发布、好友可见列表和作者删除。
- [x] 1.3 更新 P-001/P-007，把基础动态 feed 提升为 MVP 必选能力。
- [x] 1.4 编写 Superpowers TDD 执行计划。
- [x] 1.5 运行 `openspec validate add-moments-feed-mvp --strict`。

## 2. TDD 红灯：发布动态

- [x] 2.1 创建 `ChatMomentServiceImplTest`，覆盖空动态拒绝、超长正文拒绝、媒体数量上限、媒体 URL 为空拒绝、文字动态发布成功、图片动态发布保存媒体顺序。
- [x] 2.2 运行 `mvn -pl :mallchat-chat-service -am test -Dtest=ChatMomentServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`，确认红灯失败。
- [x] 2.3 创建最小空类型/空方法后再次运行测试，确认失败来自业务断言而不是单纯编译错误。

## 3. 最小实现：发布动态

- [x] 3.1 新增 `ChatMomentPublishRequest`、`ChatMomentMediaVO`、`ChatMomentVO`。
- [x] 3.2 新增 `ChatMoment`、`ChatMomentMedia`、mapper、service、controller。
- [x] 3.3 新增 `POST /chat/moment/publish`，保存主体和媒体，并保证主体和媒体保存同事务。
- [x] 3.4 更新 `sql/mallchat.sql`，新增 `chat_moment` 与 `chat_moment_media`。
- [x] 3.5 测试发布动态不会创建 `chat_message`、不会刷新 `chat_session` 未读。
- [x] 3.6 运行发布相关测试至绿灯。

## 4. TDD 红灯：好友可见列表

- [x] 4.1 覆盖作者本人动态可见。
- [x] 4.2 覆盖好友动态可见。
- [x] 4.3 覆盖非好友动态不可见。
- [x] 4.4 覆盖已删除动态不可见。

## 5. 最小实现：好友可见列表

- [x] 5.1 新增 `ChatMomentQueryRequest`。
- [x] 5.2 新增 `GET /chat/moment/list`。
- [x] 5.3 先获取可见作者集合（本人 + 好友 ID），再按 `user_id IN (...)` 分页查询，避免先分页后过滤。
- [x] 5.4 运行列表相关测试至绿灯。

## 6. TDD 红灯：删除自己的动态

- [x] 6.1 覆盖作者删除成功。
- [x] 6.2 覆盖非作者删除拒绝。
- [x] 6.3 覆盖不存在动态拒绝。
- [x] 6.4 覆盖重复删除幂等。
- [x] 6.5 覆盖已删除但非作者删除不泄露资源归属。

## 7. 最小实现：删除自己的动态

- [x] 7.1 新增 `DELETE /chat/moment/delete`。
- [x] 7.2 服务层校验作者身份并软删除；作者重复删除已删除动态返回成功。
- [x] 7.3 运行删除相关测试至绿灯。

## 8. 验证与归档

- [x] 8.1 运行 `mvn -pl :mallchat-chat-service -am test`。
- [x] 8.2 运行 `openspec validate --all --strict`。
- [x] 8.3 更新验收文档和 planning files。
- [x] 8.4 归档 `add-moments-feed-mvp` 并再次运行 OpenSpec 全量校验。
