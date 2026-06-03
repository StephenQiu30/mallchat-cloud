---
layer: Plan
doc_no: "PL-IM-003"
audience:
  - PM
  - Dev
  - QA
feature_area: message-delivery
purpose: "定义消息投递可靠性增强的实现计划、任务拆解和交付顺序。"
canonical_path: "docs/plans/PL-IM-003-message-delivery-plan.md"
status: active
version: "1.0.0"
owner: "StephenQiu30"
inputs:
  - "docs/prd/P-IM-003-message-delivery-prd.md"
outputs:
  - "消息投递增强实现计划和验收清单"
triggers:
  - "消息投递增强开发前或计划变更时"
---

# 消息投递可靠性增强实现计划

## 1. 背景

基于 PRD `docs/prd/P-IM-003-message-delivery-prd.md`，本文档定义消息投递可靠性增强的实现计划。当前工作区仅含 API 合约层（`mallchat-api`）和公共模块（`mallchat-common`），本次聚焦 API 合约增强。

## 2. 目标

补齐消息投递相关的 API 合约：错误码、枚举、DTO、VO 和 Feign 客户端接口，全部通过 TDD 验证。

## 3. 非目标

- Service 实现层（不在当前工作区范围）。
- 数据库迁移脚本执行（仅定义表结构）。
- 推送失败重试的运行时逻辑。

## 4. 核心内容

### 4.1 实现结构

```
mallchat-api/mallchat-api-chat/
  └── src/main/java/com/stephen/cloud/api/chat/
      ├── model/dto/
      │   ├── ChatMessageSendRequest.java        # 已有，含 clientMsgId
      │   ├── ChatMessageRecallRequest.java       # 已有，增强撤回
      │   ├── ChatMessageForwardRequest.java      # 已有，含 clientMsgId
      │   └── ChatMessageDeliveryStatusRequest.java # 新增
      ├── model/vo/
      │   ├── ChatMessageVO.java                  # 已有，增加 deliveryStatus
      │   ├── ChatMessageDeliveryVO.java          # 新增
      │   └── ChatMessageRevokeVO.java            # 新增
      ├── model/enums/
      │   ├── MessageStatusEnum.java              # 已有
      │   ├── ChatMessageTypeEnum.java            # 已有
      │   └── MessageDeliveryStatusEnum.java      # 新增
      └── client/
          └── ChatFeignClient.java                # 已有，增加撤回/投递接口

mallchat-common/mallchat-common-core/
  └── src/main/java/com/stephen/cloud/common/
      └── common/
          └── ErrorCode.java                      # 已有，增加消息投递错误码

mallchat-api/mallchat-api-chat/
  └── src/test/java/com/stephen/cloud/api/chat/
      ├── model/dto/
      │   ├── ChatMessageSendRequestTest.java     # 新增
      │   ├── ChatMessageRecallRequestTest.java   # 新增
      │   ├── ChatMessageForwardRequestTest.java  # 新增
      │   └── ChatMessageDeliveryStatusRequestTest.java # 新增
      ├── model/vo/
      │   ├── ChatMessageVOTest.java              # 新增
      │   ├── ChatMessageDeliveryVOTest.java      # 新增
      │   └── ChatMessageRevokeVOTest.java        # 新增
      └── model/enums/
          ├── MessageDeliveryStatusEnumTest.java  # 新增
          ├── MessageStatusEnumTest.java          # 新增
          └── ChatMessageTypeEnumTest.java        # 新增
```

### 4.2 数据库表结构定义

#### chat_message_delivery

```sql
CREATE TABLE chat_message_delivery (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id BIGINT NOT NULL COMMENT '消息ID',
    user_id BIGINT NOT NULL COMMENT '接收用户ID',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '投递状态：0-PENDING, 1-DELIVERED, 2-FAILED',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    last_retry_at DATETIME COMMENT '最后重试时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_message_id (message_id),
    INDEX idx_user_id (user_id),
    UNIQUE INDEX uk_message_user (message_id, user_id)
) COMMENT '消息投递状态跟踪表';
```

#### chat_message_revoke

```sql
CREATE TABLE chat_message_revoke (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id BIGINT NOT NULL COMMENT '消息ID',
    revoker_id BIGINT NOT NULL COMMENT '撤回者ID',
    revoked_at DATETIME NOT NULL COMMENT '撤回时间',
    reason VARCHAR(255) COMMENT '撤回原因',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_message_id (message_id)
) COMMENT '消息撤回记录表';
```

### 4.3 TDD 交付顺序

1. `docs:` PRD 和 Plan 文档
2. `test:` 失败测试（RED）
3. `impl:` 最小 API 合约实现（GREEN）
4. `refactor:` 命名和结构优化
5. `docs:` 文档索引更新

### 4.4 验证清单

- [ ] Maven 编译通过
- [ ] 所有测试通过
- [ ] DTO 校验注解正确
- [ ] 枚举值覆盖完整
- [ ] 错误码定义完整
- [ ] Swagger 注解完整
