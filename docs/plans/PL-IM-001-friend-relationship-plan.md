---
layer: Plan
doc_no: "PL-IM-001"
audience:
  - PM
  - Dev
  - QA
feature_area: friend-relationship
purpose: "定义好友关系功能的实现计划、任务拆解和交付顺序。"
canonical_path: "docs/plans/PL-IM-001-friend-relationship-plan.md"
status: active
version: "1.0.0"
owner: "StephenQiu30"
inputs:
  - "docs/prd/P-IM-001-friend-relationship-prd.md"
outputs:
  - "好友关系功能实现计划和验收清单"
triggers:
  - "好友关系功能开发前或计划变更时"
downstream:
  - "docs/acceptance/AC-IM-001-friend-relationship-acceptance.md"
---

# 好友关系实现计划

## 1. 背景

基于 PRD `docs/prd/P-IM-001-friend-relationship-prd.md`，本文档定义好友关系功能的实现计划。当前功能已全层实现，本文档用于记录实现结构、验证清单和交付边界。

## 2. 目标

完成好友关系基础闭环：申请、审批、好友列表、通知、拉黑，全部通过 TDD 验证。

## 3. 非目标

- 好友推荐、批量导入、好友上限管理。

## 4. 核心内容

### 4.1 实现结构

```
mallchat-api/mallchat-api-chat/
  └── src/main/java/com/stephen/cloud/api/chat/
      ├── model/dto/
      │   ├── ChatFriendApplyRequest.java        # 申请请求
      │   ├── ChatFriendApproveRequest.java       # 审批请求
      │   ├── ChatFriendApplyQueryRequest.java    # 申请列表查询
      │   ├── ChatFriendAddRequest.java           # 直接添加（MVP 禁用）
      │   ├── ChatFriendDeleteRequest.java        # 删除好友
      │   ├── ChatFriendListRequest.java          # 好友列表
      │   ├── ChatFriendQueryRequest.java         # 搜索
      │   ├── ChatFriendProfileUpdateRequest.java # 更新备注/分组
      │   ├── ChatFriendBlockRequest.java         # 拉黑
      │   └── ChatFriendUnblockRequest.java       # 解除拉黑
      ├── model/vo/
      │   ├── ChatFriendUserVO.java               # 好友用户视图
      │   ├── ChatFriendApplyVO.java              # 申请视图
      │   ├── ChatOperationResultVO.java          # 操作结果
      │   └── ChatIdVO.java                       # ID 视图
      └── client/
          └── ChatFeignClient.java                # Feign 客户端

mallchat-service/mallchat-chat-service/
  └── src/main/java/com/stephen/cloud/chat/
      ├── model/entity/
      │   ├── UserFriend.java                     # 好友实体
      │   ├── UserFriendApply.java                # 申请实体
      │   └── UserFriendBlock.java                # 拉黑实体
      ├── mapper/
      │   ├── UserFriendMapper.java
      │   ├── UserFriendApplyMapper.java
      │   └── UserFriendBlockMapper.java
      ├── service/
      │   ├── UserFriendService.java              # 好友服务接口
      │   ├── UserFriendApplyService.java         # 申请服务接口
      │   └── impl/
      │       ├── UserFriendServiceImpl.java      # 好友服务实现（516 行）
      │       └── UserFriendApplyServiceImpl.java # 申请服务实现（299 行）
      ├── controller/
      │   ├── ChatFriendController.java           # 好友接口
      │   └── ChatFriendApplyController.java      # 申请接口
      ├── convert/
      │   ├── ChatFriendConvert.java
      │   └── ChatFriendApplyConvert.java
      └── mq/producer/
          └── ChatMqProducer.java                 # MQ 推送

mallchat-service/mallchat-chat-service/
  └── src/test/java/com/stephen/cloud/chat/service/impl/
      ├── UserFriendServiceImplTest.java          # 好友服务测试（22 用例）
      ├── UserFriendApplyServiceImplTest.java     # 申请服务测试（19 用例）
      └── ChatApiContractConsistencyTest.java     # API 契约测试（4 用例）
```

### 4.2 任务拆解

| 阶段 | 任务 | 状态 | 说明 |
| --- | --- | --- | --- |
| 数据层 | 3 张表 DDL | ✅ 完成 | `sql/mallchat.sql` |
| 数据层 | 3 个实体类 | ✅ 完成 | MyBatis-Plus 注解 |
| 数据层 | 3 个 Mapper | ✅ 完成 | BaseMapper 无自定义 |
| 服务层 | 2 个服务接口 | ✅ 完成 | IService 扩展 |
| 服务层 | 2 个服务实现 | ✅ 完成 | 含缓存、MQ、通知 |
| 接口层 | 2 个 Controller | ✅ 完成 | Swagger 注解完整 |
| API 层 | 10 个 DTO + 4 个 VO | ✅ 完成 | 含校验注解 |
| API 层 | FeignClient | ✅ 完成 | 对外暴露接口 |
| 测试 | 边界测试补充 | ✅ 完成 | 7 个新增用例 |
| 文档 | PRD + Plan | ✅ 完成 | 本文档 |

### 4.3 TDD 流程

1. **RED**：补充 7 个边界测试（自申请、已是好友、无权限审批、重复审批等）。
2. **GREEN**：全部 41 用例通过，实现代码无需修改。
3. **Regression**：编译、仓库校验、API 契约测试通过。

## 5. 关联文档

### 5.1 输入文档

1. `docs/prd/P-IM-001-friend-relationship-prd.md`

### 5.2 输出文档

1. `docs/acceptance/AC-IM-001-friend-relationship-acceptance.md`

## 6. 验收门禁

- [ ] 全部测试通过（45 用例）。
- [ ] 编译通过。
- [ ] 仓库校验通过。
- [ ] PRD 验收标准全部满足。

## 7. 风险与边界

- 好友列表缓存一致性依赖 Redis TTL 策略。
- 通知发送失败不阻塞核心流程。

## 8. 待确认问题

- 无。

## 9. 变更记录

| 日期 | 作者 | 版本 | 变更说明 |
| --- | --- | --- | --- |
| 2026-06-03 | StephenQiu30 | 1.0.0 | 初始化 Plan，基于已实现代码整理 |
