---
layer: Acceptance
doc_no: "A-026"
audience:
  - Dev
  - QA
  - Ops
feature_area: backend-engineering-consistency
purpose: "记录 m11 Chat 领域工程化一致性审查、Issue 消费、TDD/Code Review 验收和风险。"
canonical_path: "docs/acceptance/A-026-m11-chat-consistency-acceptance.md"
status: in_progress
version: "0.1.5"
owner: "StephenQiu30"
inputs:
  - "docs/design/D-003-backend-engineering-consistency-design.md"
  - "docs/plans/PL-008-backend-engineering-consistency-plan.md"
  - "GitHub Issue #52"
  - "GitHub Issue #53"
  - "GitHub Issue #54"
  - "GitHub Issue #55"
  - "GitHub Issue #56"
outputs:
  - "m11 Chat 领域工程化一致性审查清单"
  - "m11 验收证据"
triggers:
  - "消费 m11 后端工程化一致性治理 Issue"
downstream:
  - "m11 后端工程化一致性治理 PR"
---

# m11 Chat 工程化一致性验收

## 1. Issue 编排

| Issue | 标题 | 状态 | 说明 |
| --- | --- | --- | --- |
| [#52](https://github.com/StephenQiu30/mallchat-cloud/issues/52) | `[m11][epic][backend] 后端工程化一致性治理` | Open | m11 Epic，聚合本批治理任务 |
| [#53](https://github.com/StephenQiu30/mallchat-cloud/issues/53) | `[m11][backend][chat] Chat 接口与模型一致性审查清单` | Closed | 已完成事实审查和文档沉淀 |
| [#55](https://github.com/StephenQiu30/mallchat-cloud/issues/55) | `[m11][backend][chat] Chat P0/P1 一致性最小修正` | Closed | 已按审查清单完成 TDD 最小修正 |
| [#56](https://github.com/StephenQiu30/mallchat-cloud/issues/56) | `[m11][backend][chat] Chat DTO/VO 接口契约收敛` | Closed | 已完成 DTO Request / VO Response 契约收敛 |
| [#54](https://github.com/StephenQiu30/mallchat-cloud/issues/54) | `[m11][backend][qa] 工程化一致性验收与 Code Review` | Closed | 已完成验收证据沉淀和只读 Code Review |

## 2. 审查范围

| 范围 | 路径 | 当前事实 |
| --- | --- | --- |
| API DTO | `mallchat-api/mallchat-api-chat/src/main/java/com/stephen/cloud/api/chat/model/dto` | 26 个 DTO，命名基本保持 `Chat*Request` / `*QueryRequest` |
| API VO | `mallchat-api/mallchat-api-chat/src/main/java/com/stephen/cloud/api/chat/model/vo` | 13 个 VO，未发现 `isDelete` 暴露 |
| API Enum | `mallchat-api/mallchat-api-chat/src/main/java/com/stephen/cloud/api/chat/model/enums` | 5 个 Enum，整体保持 `code` / `desc` 风格 |
| Controller | `mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/controller` | 8 个 Controller，主流程返回 `BaseResponse` + `ResultUtils.success` |
| Convert | `mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/convert` | 8 个 Convert，均为静态字段转换，未访问数据库或远程服务 |
| Entity | `mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/model/entity` | 15 个 Entity，使用 MyBatis Plus `@TableName` / `@TableId` 风格 |
| Service | `mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/service` | Service/ServiceImpl 分层清楚，核心行为已有 focused tests |
| Tests | `mallchat-service/mallchat-chat-service/src/test/java/com/stephen/cloud/chat` | 已覆盖消息、会话、群、好友、动态、举报、MQ 和指标等核心服务 |

## 3. 正向结论

1. API 契约总体放在 `mallchat-api-chat`，Service 实现总体放在 `mallchat-chat-service`，没有发现明显跨层反向依赖。
2. Controller 基本保持“登录态读取、参数校验、Service 调用、`ResultUtils.success` 返回”的薄边界。
3. Convert 类保持静态工具风格，没有数据库、远程调用或权限判断。
4. Entity 未引用 DTO/VO，整体符合持久化模型边界。
5. 测试覆盖已经覆盖 IM 主链路，适合作为 #55 的 TDD 基线。

## 4. 审查清单

| 等级 | 类型 | 文件 | 问题 | 风险 | 建议 |
| --- | --- | --- | --- | --- | --- |
| P1 | DTO | `ChatPrivateRoomRequest.java` | `peerUserId` 在 `@Schema` 中标记 required，但缺少 `@NotNull` | Controller 目前手动校验可兜底，但 API 契约和 Bean Validation 不一致 | #55 中补 `@NotNull`，保持 Controller 兜底不变 |
| P1 | DTO | `ChatMessageReadRequest.java` | `roomId`、`lastReadMessageId` 标记 required，但缺少 `@NotNull` | Controller 已手动校验，但契约自描述不足 | #55 中补 `@NotNull`，并保持现有服务测试不变 |
| P1 | DTO | `ChatMomentCommentRequest.java` | `content` 标记 required，但缺少 `@NotBlank` | Service 已校验空白评论，Controller 层契约不完整 | #55 中补 `@NotBlank`，不改变 Service 兜底 |
| P1 | DTO | `ChatFriendQueryRequest.java` | `@Schema` 描述仍为英文 `Keywords (user nickname)` / `User ID` / `Friend User ID` | OpenAPI 展示风格不一致，不影响运行 | #55 中改为中文描述 |
| P1 | Controller | `ChatMessageController.java` | `markMessageRead` 使用 `new BusinessException(ErrorCode.PARAMS_ERROR)`，同类接口更多使用 `ThrowUtils.throwIf` | Controller 参数错误处理风格不一致 | #55 中改为 `ThrowUtils.throwIf` |
| P1 | Controller | `ChatMessageController.java`、`ChatSessionController.java` | 存在 `com.stephen.cloud.common.common.*` 通配导入 | 与多数 Controller 的显式 import 风格不一致 | #55 中改为显式 import |
| P1 | Controller | `ChatFriendController.java` | 多个接口保留未使用 `HttpServletRequest servletRequest` 参数 | 增加阅读噪声，和当前 `SecurityUtils.getLoginUserId()` 风格不一致 | #55 中删除未使用参数和对应 Javadoc |
| P1 | Controller | `ChatRoomController.java`、`ChatMessageController.java`、`ChatFriendApplyController.java`、`ChatSessionController.java` | 存在“参数校验 / 获取用户 / 调用 Service”等显而易见注释 | 影响可读性，不影响行为 | #55 中清理显而易见注释，保留解释边界或风险的注释 |
| P1 | API Contract | 多个 `chat` Controller | 部分接口仍使用多个 `@RequestParam`、通用 `DeleteRequest`、裸 `Boolean` 或裸 `Long` 作为面向前端的稳定契约 | 不利于前端接口生成、响应格式管控和后续字段扩展；直接全量修改存在兼容性风险 | 拆到 #56 单独按 TDD 收敛 DTO Request / VO Response，不并入 #55 小修 |
| P2 | Convert | `ChatRoomJoinApplyConvert.java` | 方法名使用 `getVOList` / `getVOPage`，其他 Convert 多为 `getChat*VO` 或 `objToVo` | 仅风格差异，修改会触发低收益调用点变更 | 记录，不在 m11 强制修复 |
| P2 | Convert | 多个 Convert | `objToVo`、`getChat*VO` 命名并存 | 历史风格差异，不影响行为 | 后续只在触碰对应文件时顺手收敛 |
| P2 | Service | `ChatMomentServiceImpl.java`、`ChatRoomJoinApplyServiceImpl.java`、`ChatReportServiceImpl.java` | 存在局部状态常量，例如 `STATUS_PENDING`、`STATUS_NORMAL`、`VISIBILITY_PUBLIC` | 当前局部使用清楚，抽成共享枚举可能过度设计 | 保持现状，除非出现跨模块复用或端侧契约需求 |
| P2 | VO | 多个 VO | `status`、`createTime`、`visibility` 对端侧可见 | 属于端侧展示或排序所需字段，未发现 `isDelete` 等内部字段暴露 | 保持现状 |

## 5. #53 结论

1. 本轮未发现 P0 问题。
2. P1 问题集中在请求契约自描述、Controller 导入/参数/异常风格和显而易见注释，适合进入 #55 做最小修正。
3. DTO Request / VO Response 是前端接口生成和响应管控的关键规范，已拆为 #56 独立处理，不能在 #55 中无迁移说明地直接全量改接口。
4. P2 问题只记录，不作为 m11 阻塞项，避免为了统一而做低收益重命名或抽象。
5. #53 不修改业务代码，不需要 RED 测试；后续 #55/#56 如改动行为边界，必须先写 RED。

## 6. #55 TDD 记录

| 阶段 | 命令 | 结果 | 说明 |
| --- | --- | --- | --- |
| RED | `mvn -B -pl mallchat-service/mallchat-chat-service -am -Dtest=ChatApiContractConsistencyTest -Dsurefire.failIfNoSpecifiedTests=false test` | 失败 | 2 个断言按预期失败：缺少 Bean Validation 注解；`ChatFriendQueryRequest` Schema 描述仍为英文 |
| GREEN | `mvn -B -pl mallchat-service/mallchat-chat-service -am -Dtest=ChatApiContractConsistencyTest -Dsurefire.failIfNoSpecifiedTests=false test` | 通过 | 新增契约一致性测试 2/2 通过 |
| Focused | `mvn -B -pl mallchat-service/mallchat-chat-service -am -Dsurefire.failIfNoSpecifiedTests=false test` | 通过 | 219 tests, 0 failures, 0 errors |

## 7. #55 修正内容

1. 为 `ChatPrivateRoomRequest.peerUserId`、`ChatMessageReadRequest.roomId`、`ChatMessageReadRequest.lastReadMessageId`、`ChatMomentCommentRequest.content` 补齐 Bean Validation 注解。
2. 将 `ChatFriendQueryRequest` 的 Schema 描述改为中文，保持 API 文档风格一致。
3. 将 `ChatMessageController.markMessageRead` 参数错误处理收敛为 `ThrowUtils.throwIf`。
4. 清理 chat Controller 的通配导入、未使用 `HttpServletRequest` 参数和显而易见注释。
5. 保持 P2 项不改，未进行 Convert 重命名、共享枚举抽象或目录重排。

## 8. #56 DTO/VO 契约补充

1. 所有面向前端或跨服务生成契约的接口，应使用业务 DTO Request / QueryRequest 表达入参，减少散落的 `@RequestParam` 和通用请求对象。
2. 所有面向前端或跨服务生成契约的接口，应使用业务 VO 或 `Page<*VO>` 表达响应；简单成功状态也应提供可扩展 VO，避免长期暴露裸 `Boolean` / `Long`。
3. 已新增 `ChatIdVO` 与 `ChatOperationResultVO`，避免每个简单操作都创建空壳响应类；业务请求仍按场景定义 `Chat*Request` / `Chat*QueryRequest`。
4. 已将 chat Controller 和 `ChatFeignClient` 中的 `@RequestParam`、通用 `DeleteRequest`、`BaseResponse<Boolean>`、`BaseResponse<Long>` 收敛为 DTO Request / VO Response。
5. 兼容性影响：部分响应 `data` 从裸 `true` / `123` 调整为 `{ "success": true }` / `{ "id": 123 }`；前端接口生成需要按新版 OpenAPI 契约同步。
6. 历史 query 参数调用保持兼容：原 query/form 参数接口改为 DTO 绑定但不强制 JSON body；动态 ID 参数继续使用 `id`，消息搜索默认 `pageSize=20` 不变。

## 9. #56 TDD 记录

| 阶段 | 命令 | 结果 | 说明 |
| --- | --- | --- | --- |
| RED | `mvn -B -pl mallchat-service/mallchat-chat-service -am -Dtest=ChatApiContractConsistencyTest -Dsurefire.failIfNoSpecifiedTests=false test` | 失败 | 契约护栏发现 `@RequestParam`、`DeleteRequest`、`BaseResponse<Boolean/Long>` 违例 |
| GREEN | `mvn -B -pl mallchat-service/mallchat-chat-service -am -Dtest=ChatApiContractConsistencyTest -Dsurefire.failIfNoSpecifiedTests=false test` | 通过 | 4 tests, 0 failures, 0 errors |
| Focused | `mvn -B -pl mallchat-service/mallchat-chat-service -am -Dsurefire.failIfNoSpecifiedTests=false test` | 通过 | 221 tests, 0 failures, 0 errors |

## 10. 验收记录

| 命令 | 结果 | 说明 |
| --- | --- | --- |
| `openspec validate --all --strict` | 通过 | 21 passed, 0 failed |
| `git diff --check` | 通过 | 无空白或补丁格式问题 |
| `bash scripts/validate-repository.sh` | 通过 | 仓库规范校验通过 |
| `mvn -B -DskipTests compile` | 通过 | 全仓 25 个 Maven 模块编译成功 |

## 11. Code Review 记录

| Reviewer | 结果 | 结论 |
| --- | --- | --- |
| 只读 reviewer | 已处理 | 首轮发现 Feign 契约未同步、query/body 兼容性、搜索默认页大小和测试覆盖缺口；已同步 `ChatFeignClient`，保留历史 query 绑定与 `pageSize=20`，并扩展契约测试扫描 Feign |

## 12. 变更记录

| 日期 | 作者 | 版本 | 变更说明 |
| --- | --- | --- | --- |
| 2026-05-21 | StephenQiu30 | 0.1.0 | 初始化 m11 Chat 工程化一致性审查清单 |
| 2026-05-21 | StephenQiu30 | 0.1.1 | 记录 #55 TDD 修正与验证结果 |
| 2026-05-21 | StephenQiu30 | 0.1.2 | 补充 DTO Request / VO Response 接口契约要求并拆分 #56 |
| 2026-05-21 | StephenQiu30 | 0.1.3 | 记录 #56 DTO/VO 契约收敛、兼容性影响与 TDD 证据 |
| 2026-05-21 | StephenQiu30 | 0.1.4 | 记录 Code Review 反馈处理和 Feign 契约同步 |
| 2026-05-21 | StephenQiu30 | 0.1.5 | 同步 #54/#56 GitHub Issue 关闭状态 |
