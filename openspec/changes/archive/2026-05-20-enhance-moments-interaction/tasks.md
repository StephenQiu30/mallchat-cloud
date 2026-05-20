## 1. OpenSpec 与计划

- [x] 1.1 创建 `enhance-moments-interaction` change。
- [x] 1.2 明确本次只做动态互动闭环，不扩展公开广场或嵌套评论。
- [x] 1.3 运行 `openspec validate enhance-moments-interaction --strict`。
- [x] 1.4 使用 Hermes 只读复核范围、测试和实现一致性。

## 2. TDD 红灯

- [x] 2.1 增加可见动态点赞成功并递增计数测试。
- [x] 2.2 增加重复点赞幂等测试。
- [x] 2.3 增加取消点赞幂等并递减计数测试。
- [x] 2.4 增加唯一键冲突下点赞仍幂等成功、不重复计数和不重复通知测试。
- [x] 2.5 增加不可见动态点赞/评论/评论列表被拒绝测试。
- [x] 2.6 增加不存在或已删除动态点赞/评论/评论列表被拒绝测试。
- [x] 2.7 增加评论正文空白和超长被拒绝测试。
- [x] 2.8 增加评论成功、评论列表分页查询、已删除评论不返回测试。
- [x] 2.9 增加点赞/评论通知成功、自互动不通知与通知失败不回滚测试。
- [x] 2.10 运行目标测试，确认红灯来自缺失互动契约或业务断言。

## 3. 最小实现

- [x] 3.1 新增 `chat_moment_like` 和 `chat_moment_comment` 表结构。
- [x] 3.2 新增动态点赞/评论实体、mapper、DTO/VO。
- [x] 3.3 在 `ChatMomentService` 增加点赞、取消点赞、评论和评论列表接口。
- [x] 3.4 实现可见性校验：作者本人或互为好友才可互动。
- [x] 3.5 实现点赞幂等、取消点赞幂等和计数更新。
- [x] 3.6 实现一级评论创建、分页查询和计数更新。
- [x] 3.7 新增业务通知创建 DTO/Feign 契约，不复用管理员批量发布入口。
- [x] 3.8 复用通知 Feign 创建点赞/评论通知；通知失败降级吞掉。

## 4. 验证与归档

- [x] 4.1 运行 `ChatMomentServiceImplTest` 相关测试。
- [x] 4.2 运行 chat-service 模块回归。
- [x] 4.3 运行 `openspec validate --all --strict`。
- [x] 4.4 归档本次 OpenSpec change 并再次运行 `openspec validate --all --strict`。
- [x] 4.5 更新 `task_plan.md`、`findings.md`、`progress.md`，并按 test/impl 拆分提交。
