---
layer: Acceptance
doc_no: "A-029"
audience:
  - Dev
  - QA
  - Ops
feature_area: backend-engineering-consistency
purpose: "记录 m13 基础领域 user/ai/gateway/common 工程化一致性审查、TDD 修正、验收命令和残余风险。"
canonical_path: "docs/acceptance/A-029-m13-foundation-consistency-acceptance.md"
status: ready
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "docs/plans/PL-009-backend-engineering-consistency-followup-plan.md"
  - "GitHub Issue #64"
  - "GitHub Issue #65"
  - "GitHub Issue #66"
  - "GitHub Issue #67"
  - "GitHub Issue #68"
  - "GitHub Issue #69"
outputs:
  - "m13 基础领域工程化一致性修正"
  - "m13 TDD / Code Review / 验收证据"
triggers:
  - "消费 m13 Issue #64-#69"
downstream:
  - "GitHub Issue #70"
  - "GitHub Issue #78"
---

# m13 基础领域工程化一致性验收

## 1. Issue 范围

| Issue | 标题 | 状态 | 说明 |
| --- | --- | --- | --- |
| [#64](https://github.com/StephenQiu30/mallchat-cloud/issues/64) | `[m13][epic][backend] 基础领域工程化一致性治理` | 待 PR 关闭 | m13 Epic，聚合 user/ai/gateway/common 治理任务 |
| [#65](https://github.com/StephenQiu30/mallchat-cloud/issues/65) | `[m13][backend][user] 用户接口 DTO/VO 与权限契约收敛` | 已完成，待 PR 关闭 | 用户接口请求/响应契约收敛 |
| [#66](https://github.com/StephenQiu30/mallchat-cloud/issues/66) | `[m13][backend][ai] AI 接口 DTO/VO 与聊天记录契约收敛` | 已完成，待 PR 关闭 | AI 对话、记录删除和历史隔离边界 |
| [#67](https://github.com/StephenQiu30/mallchat-cloud/issues/67) | `[m13][backend][gateway] 网关鉴权与响应边界审查` | 已完成，待 PR 关闭 | 网关白名单与限流 key 边界 |
| [#68](https://github.com/StephenQiu30/mallchat-cloud/issues/68) | `[m13][backend][common] 公共契约边界与轻量护栏审查` | 已完成，待 PR 关闭 | RabbitMQ 事务发送公共语义恢复 |
| [#69](https://github.com/StephenQiu30/mallchat-cloud/issues/69) | `[m13][backend][qa] 基础领域验收与 Code Review` | 已完成，待 PR 关闭 | m13 收口验收与审查 |
| [#78](https://github.com/StephenQiu30/mallchat-cloud/issues/78) | `[m15][multi-end][contract] 同步 m13 后端 DTO/VO 契约到多端生成客户端` | 后续阻塞项 | Code Review 发现的多端生成客户端同步任务，正式上线前必须完成 |

## 2. 初始审查基线

以下内容是 m13 只读审查阶段发现的修复前基线，不代表当前代码最终状态。

| 子领域 | P0/P1 结论 | 本轮处理 |
| --- | --- | --- |
| user | 删除空 id 可能 NPE；多处返回裸 `Boolean/Long`；Feign 查询使用 `@RequestParam`；`list/page` 暴露实体 | 已修正核心契约：新增领域 DTO/VO，删除接口空 id 稳定参数错误，Feign 查询改 DTO，管理分页改 `Page<UserVO>` |
| ai | `getChatMemory` 只按 `sessionId` 查历史，可能跨用户加载上下文；删除接口复用 `DeleteRequest` 且返回裸 `Boolean`；消息必填约束缺失 | 已修正：历史查询追加当前 `userId` 过滤；删除接口改 `AiChatRecordDeleteRequest` + `AiOperationResultVO`；`message` 加 `@NotBlank` |
| gateway | 白名单包含 `/api/user/logout` 和不存在的 `/api/notification/page`；用户限流 key 只读 header，未优先使用认证属性 | 已修正：删除不合理白名单；`userKeyResolver` 优先读取 `GatewayConstant.ATTR_LOGIN_USER_ID` |
| common | `RabbitMqSender.sendTransactional` 语义降级为立即发送，和 README/方法名冲突 | 已修正：有事务同步时注册 `afterCommit`，无线程事务同步时保留直接发送兼容入口 |

## 3. 过度设计拦截

1. 本轮没有引入统一 `PageVO<T>`、统一接口生成器、全局响应重构或新的 AI 编排层。
2. user/ai 只新增小型 DTO/VO，遵循 chat/log/notification 中已有的 `IdRequest`、`IdVO`、`OperationResultVO` 风格。
3. gateway 只修白名单和限流 key，不在 m13 中重写限流响应体或错误 envelope。
4. common 只恢复 `sendTransactional` 的 afterCommit 语义，不改 RabbitMQ consumer 分发架构。
5. `BaseResponse`、`DeleteRequest`、`PageRequest` 公共语义保持兼容；具体业务入口通过领域 DTO 和局部校验补护栏。

## 4. TDD 记录

| 领域 | RED 证据 | GREEN 结果 |
| --- | --- | --- |
| user | 新增 `UserApiContractConsistencyTest`，暴露 `BaseResponse<Boolean/Long>`、通用 `DeleteRequest`、Feign `@RequestParam` | `mvn -B -pl mallchat-service/mallchat-user-service -am -Dtest=UserApiContractConsistencyTest -Dsurefire.failIfNoSpecifiedTests=false test` 通过，3 tests |
| ai | 新增 `AiApiContractConsistencyTest` 和 `AiChatServiceImplTest`，覆盖删除契约、消息必填、非法模型类型和会话历史 userId 过滤 | `mvn -B -pl mallchat-service/mallchat-ai-service -am -Dtest=AiApiContractConsistencyTest,AiChatServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` 通过，6 tests |
| gateway | 新增 `GatewayAuthWhitelistConfigTest` 和 `RateLimitConfigTest`，覆盖白名单和限流 key 来源 | `mvn -B -pl mallchat-gateway -am -Dtest=GatewayAuthWhitelistConfigTest,RateLimitConfigTest -Dsurefire.failIfNoSpecifiedTests=false test` 通过，2 tests |
| common | 扩展 `RabbitMqSenderTest`，先证明 `sendTransactional` 会立即发送 | `mvn -B -pl mallchat-common/mallchat-common-rabbitmq -Dtest=RabbitMqSenderTest -Dsurefire.failIfNoSpecifiedTests=false test` 通过，4 tests |

## 5. 修正内容

### 5.1 user

1. 新增 `UserIdRequest`、`UserIdsRequest`，让用户查询/删除接口具备明确领域 DTO。
2. 新增 `UserIdVO`、`UserOperationResultVO`、`UserAdminStatusVO`，替代用户变更接口中的裸 `Long/Boolean` 响应。
3. `UserFeignClient` 查询接口改为 `@SpringQueryMap` DTO，并保留默认方法兼容现有 Java 调用方。
4. `UserController` 的 add/delete/update/edit/logout/email-code/admin-status 返回领域 VO。
5. `UserController#get` 与 `list/page` 不再直接返回 `User` 实体，改为 `UserVO` / `Page<UserVO>`。

### 5.2 ai

1. 新增 `AiChatRecordDeleteRequest` 和 `AiOperationResultVO`，替代 AI 删除接口中的通用 `DeleteRequest` 和裸 `Boolean`。
2. `AiChatRequest.message` 增加 `@NotBlank`，Controller 参数增加 `@Validated`。
3. `AiChatServiceImpl#getChatMemory` 在 `sessionId` 基础上强制获取当前 `userId` 并追加查询条件，身份缺失时返回未登录错误，避免跨用户加载上下文。
4. `AiClientFactory` 对空 `modelType` 继续默认 DashScope，对非空非法值返回参数错误。
5. AI 记录分页补充 `current > 0`、`pageSize > 0 && pageSize <= 20` 护栏。

### 5.3 gateway

1. 网关认证白名单删除 `/api/user/logout` 和 `/api/notification/page`。
2. `userKeyResolver` 优先使用认证过滤器写入的 `GatewayConstant.ATTR_LOGIN_USER_ID`，再兼容读取 `SecurityConstant.USER_ID_HEADER`。
3. 新增配置级和 resolver 级测试，防止白名单和限流 key 回退。

### 5.4 common

1. `RabbitMqSender.sendTransactional` 在事务同步存在时注册 `TransactionSynchronization.afterCommit`。
2. 无事务同步时继续直接发送，兼容既有非事务调用方。
3. `RabbitMqSenderTest` 固化 afterCommit 行为，避免事务回滚前发出 MQ。

## 6. 保留风险与后续

| 风险 | 本轮处理 | 后续建议 |
| --- | --- | --- |
| `from-source: inner` 仍依赖明文内部头 | 已记录，不在 m13 内强行引入服务间签名，避免破坏 Feign 链路 | m14 或独立安全 Epic 中设计网关/Feign 内部凭证 |
| RabbitMQ consumer 去重和异常重试语义仍需进一步审查 | 本轮只恢复 producer transactional 语义 | 后续补 consumer dispatcher RED/GREEN，并评估 dedupe 写入时机 |
| gateway 429 响应体尚未统一 JSON | 本轮只修认证白名单和限流 key | m14 工程化守护或 gateway 安全任务中补响应 envelope 测试 |
| `UserVO` 仍包含 role/email 字段 | 本轮先不拆 `PublicUserVO/AdminUserVO`，避免影响大量 IM 调用方 | 后续独立评估 public/admin VO 分层和前端字段依赖 |
| 多端生成客户端仍是 m13 前旧契约 | 已创建 [#78](https://github.com/StephenQiu30/mallchat-cloud/issues/78)，明确作为后续多端同步阻塞项 | m13 是后端契约 PR；正式上线前必须完成 Taro/Admin/Flutter API client 同步 |

## 7. 验收命令

| 命令 | 结果 | 说明 |
| --- | --- | --- |
| `mvn -B -pl mallchat-service/mallchat-user-service -am -Dtest=UserApiContractConsistencyTest -Dsurefire.failIfNoSpecifiedTests=false test` | 通过 | user 契约护栏 3 tests |
| `mvn -B -pl mallchat-service/mallchat-ai-service -am -Dtest=AiApiContractConsistencyTest,AiChatServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` | 通过 | ai 契约与历史隔离 6 tests |
| `mvn -B -pl mallchat-gateway -am -Dtest=GatewayAuthWhitelistConfigTest,RateLimitConfigTest -Dsurefire.failIfNoSpecifiedTests=false test` | 通过 | gateway 白名单与限流 key 2 tests |
| `mvn -B -pl mallchat-common/mallchat-common-rabbitmq -Dtest=RabbitMqSenderTest -Dsurefire.failIfNoSpecifiedTests=false test` | 通过 | RabbitMQ sender 4 tests |
| `mvn -B -pl mallchat-service/mallchat-user-service -am -Dsurefire.failIfNoSpecifiedTests=false test` | 通过 | user-service 聚焦模块测试 3 tests |
| `mvn -B -pl mallchat-service/mallchat-ai-service -am -Dsurefire.failIfNoSpecifiedTests=false test` | 通过 | ai-service 聚焦模块测试 6 tests |
| `mvn -B -pl mallchat-gateway -am -Dsurefire.failIfNoSpecifiedTests=false test` | 通过 | gateway 聚焦模块测试 12 tests |
| `mvn -B -DskipTests compile` | 通过 | 全仓 25 个 Maven 模块编译通过 |
| `openspec validate --all --strict` | 通过 | 21 items passed |
| `git diff --check` | 通过 | 无空白或补丁格式问题 |
| `bash scripts/validate-repository.sh` | 通过 | 仓库规范校验通过 |

## 8. 变更记录

| 日期 | 作者 | 版本 | 变更说明 |
| --- | --- | --- | --- |
| 2026-05-21 | StephenQiu30 | 0.1.0 | 初始化 m13 基础领域工程化一致性验收记录 |
| 2026-05-21 | StephenQiu30 | 0.1.1 | 记录 Code Review 反馈处理：AI 历史强制 userId、非法模型测试、多端契约同步 issue #78 |
