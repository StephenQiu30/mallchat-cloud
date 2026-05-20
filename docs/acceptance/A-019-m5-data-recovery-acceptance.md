---
layer: Acceptance
doc_no: "A-019"
audience:
  - Dev
  - QA
  - Ops
feature_area: im-data-recovery
purpose: "记录 m5 数据安全与备份恢复 Epic 的测试先行、实现范围和验收命令。"
canonical_path: "docs/acceptance/A-019-m5-data-recovery-acceptance.md"
status: review
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "GitHub Issue #17"
  - "GitHub Issue #18"
  - "GitHub Issue #19"
  - "openspec/changes/verify-im-core-data-recovery"
  - "openspec/changes/verify-redis-cache-recovery"
  - "openspec/changes/harden-file-upload-boundary"
outputs:
  - "m5-backend-data-recovery-epic"
  - "核心 IM 表备份恢复 smoke"
  - "Redis 失效恢复验收"
  - "文件上传安全边界"
triggers:
  - "创建或更新 m5 PR"
  - "回归数据安全与备份恢复 Epic #5"
downstream:
  - "GitHub Epic #5"
  - "GitHub PR #24"
---

# m5 数据安全与备份恢复验收

## 1. 验收范围

本次 m5 聚合消费 Epic #5 下的 #17、#18、#19。实现保持最小生产可用闭环：不建设完整灾备系统、不让旧登录态自动恢复、不引入文件内容审核或杀毒扫描。

交付 PR：[https://github.com/StephenQiu30/mallchat-cloud/pull/24](https://github.com/StephenQiu30/mallchat-cloud/pull/24)

## 2. 结论

1. #17：新增核心 IM 表固定清单、备份脚本和恢复 smoke dry-run；真实演练可恢复到临时库并检查消息、会话、房间、好友和动态关联。
2. #18：好友缓存和房间成员缓存冷缓存 DB 回源已有测试保护；WebSocket 心跳在 Redis 连接态丢失时会从本地连接重建。
3. #19：文件上传在进入 COS 前校验空文件、大小、危险文件名、后缀、Content-Type 和图片魔数。

## 3. RED 证据

1. `bash scripts/verify-im-core-data-recovery.sh --dry-run` 初次失败：脚本不存在。
2. `ChannelManagerTest#shouldRebuildRedisConnectionStateWhenHeartbeatFindsCacheMissing` 初次失败：Redis 连接集合清空后心跳没有重建在线态。
3. `FileUploadValidatorTest` 初次编译失败：缺少 `FileUploadValidator`。
4. 好友和房间成员冷缓存测试新增后直接通过，说明现有 DB 回源逻辑已经满足 Redis flush/key missing 恢复边界。

## 4. GREEN 命令

```bash
bash scripts/verify-im-core-data-recovery.sh --dry-run
mvn -pl mallchat-common/mallchat-common-websocket -am -Dtest=ChannelManagerTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl mallchat-service/mallchat-chat-service -am -Dtest=ChatRoomMemberServiceImplTest,UserFriendServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl mallchat-service/mallchat-file-service -am -Dtest=FileUploadValidatorTest,FileUploadRecordRecorderTest,FileServiceApplicationTest -Dsurefire.failIfNoSpecifiedTests=false test
openspec validate --all --strict
```

## 5. 残余风险

1. 数据恢复真实演练依赖可访问 MySQL 和 `mysqldump`/`mysql` 客户端；当前 CI 只执行 dry-run 和脚本静态路径。
2. Redis 脏缓存存在但内容错误不在本次范围；本次只覆盖 key missing / flush 后的恢复。
3. 文件上传只做 MVP 白名单和图片魔数校验，不替代内容审核、病毒扫描或私有下载鉴权。
