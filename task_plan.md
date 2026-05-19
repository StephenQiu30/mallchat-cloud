## 任务目标
完成 `enhance-friend-discovery-relationship` 的 IM 后端闭环：好友发现、关系状态返回、好友删除能力与 OpenSpec 归档验收同步落地，确保 TDD 与高可用边界先于实现落地。

## 当前阶段
Phase 5

## 阶段清单
### Phase 1：需求与现状自审
- [x] 梳理现有 `chat-friend` 能力与 OpenSpec/spec 约束。
- [x] 识别缺口（缺少发现入口、关系状态、删除接口返回一致性）。
- [x] 进行一次逻辑漏洞扫描：确认是否有未对齐的边界（分页、关系方向、缓存退化、权限）。

### Phase 2：计划与文档对齐
- [x] 使用 planning-with-files 建立可追溯文档（task_plan/findings/progress）。
- [x] 补齐 OpenSpec tasks 和 docs 计划/验收草稿。

### Phase 3：TDD 红绿实现
- [x] 按 OpenSpec 范围新增服务端能力：
  - `friendStatus` 状态映射（0/1/2/3/4）。
  - `/chat/friend/search` 与 `/chat/friend/delete`。
- [x] 为关键边界补齐/完善 `UserFriendServiceImplTest`。
- [x] 通过 TDD 验证关系状态与分页边界。

### Phase 4：验证与归档闭环
- [x] 运行 `mvn -pl :mallchat-chat-service -am ... UserFriendServiceImplTest` 通过。
- [x] 运行 `openspec validate` 全量通过。
- [x] 复核 Hermes 给出一致性结论与未决风险。
- [x] 补齐/确认本次的验收档案并归档 OpenSpec change。

### Phase 5：交付与提交
- [ ] 按 `test:/impl:` 顺序提交，确保提交范围只含本 change。
- [ ] 输出最终交付说明与残余风险。

## 关键问题与决策
- 决策：关系状态优先由后端统一计算，不在前端重复推导。
  - 原因：避免前端串改、保持 `chat-friend` 契约一致和状态幂等。
- 决策：`friendStatus` 值域保留为 0/1/2/3/4，兼容现有 `friend add` 禁用边界。
  - 原因：避免在本次 scope 引入备注/拉黑/分组等额外域模型。
- 决策：`searchFriends` 分页边界对 `current <= 0` 做最小归一化（转 1）并保留 `pageSize` 严格校验。
  - 原因：兼容前端可用性，避免 `0/负数` 页码导致 4xx 风险。

## 已遇到问题
| 问题 | 轮次 | 处理方式 |
|---|---:|---|
| `mvn -pl ...` 只在当前模块执行导致 `No tests matching pattern` | 1 | 使用 `-pl :mallchat-chat-service -am` + `-Dsurefire.failIfNoSpecifiedTests=false` |
| `ChatCacheConstant` 类加载失败 | 1 | 通过完整依赖链构建并在 reactor 中执行测试 |
| `friendStatus` 断言期望不一致（3 vs 12） | 1 | 修正预期为返回分页 `total`（12）以对齐 `user-service` 总量语义 |

## 待确认风险（需要你确认的下一步）
- 当前任务未补控制器级测试（当前仓库既有可复用 controller 测试基线较少），已通过 service-first + spec 化覆盖。
- 缓存退化场景（Redis 残留旧好友关系但 DB 无关系）未额外加测，需按你要求决定是否扩大下一阶段范围。
