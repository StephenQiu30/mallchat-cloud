---
layer: Acceptance
doc_no: "A-028"
audience:
  - Dev
  - QA
  - Ops
feature_area: backend-engineering-consistency
purpose: "记录 m12 支撑领域 log/file/notification 工程化一致性事实审查、P0/P1/P2 分级和后续 Issue 消费建议。"
canonical_path: "docs/acceptance/A-028-m12-support-consistency-audit.md"
status: ready
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "docs/plans/PL-009-backend-engineering-consistency-followup-plan.md"
  - "docs/acceptance/A-027-backend-engineering-followup-issue-acceptance.md"
  - "GitHub Issue #58"
  - "GitHub Issue #59"
outputs:
  - "m12 支撑领域工程化一致性审查清单"
  - "m12 #60/#61/#62 TDD 修正与 #63 验收输入"
triggers:
  - "消费 m12 Issue #59"
downstream:
  - "GitHub Issue #60"
  - "GitHub Issue #61"
  - "GitHub Issue #62"
  - "GitHub Issue #63"
---

# m12 支撑领域工程化一致性审查

## 1. Issue 范围

| Issue | 标题 | 状态 | 说明 |
| --- | --- | --- | --- |
| [#58](https://github.com/StephenQiu30/mallchat-cloud/issues/58) | `[m12][epic][backend] 支撑领域工程化一致性治理` | 待 PR 关闭 | m12 Epic，聚合 log/file/notification 治理任务 |
| [#59](https://github.com/StephenQiu30/mallchat-cloud/issues/59) | `[m12][backend][support] log/file/notification 一致性审查清单` | 已完成，待 PR 关闭 | 本文档对应的只读审查任务 |
| [#60](https://github.com/StephenQiu30/mallchat-cloud/issues/60) | `[m12][backend][log] 日志接口 DTO/VO 契约收敛` | 已完成，待 PR 关闭 | 承接 log P1/P2 修正 |
| [#61](https://github.com/StephenQiu30/mallchat-cloud/issues/61) | `[m12][backend][file] 文件上传接口契约与记录边界审查` | 已完成，待 PR 关闭 | 承接 file P1/P2 修正 |
| [#62](https://github.com/StephenQiu30/mallchat-cloud/issues/62) | `[m12][backend][notification] 通知接口 DTO/VO 与 Feign 契约收敛` | 已完成，待 PR 关闭 | 承接 notification P1/P2 修正 |
| [#63](https://github.com/StephenQiu30/mallchat-cloud/issues/63) | `[m12][backend][qa] 支撑领域验收与 Code Review` | 已完成，待 PR 关闭 | m12 收口验收和只读 Code Review |

## 2. 初始审查基线

以下内容是 #59 只读审查阶段发现的修复前基线，不代表当前代码最终状态。已修正结果见第 7-14 节。

| 子领域 | API 契约 | Service 实现 | 初始结论 |
| --- | --- | --- | --- |
| log | `mallchat-api/mallchat-api-log` | `mallchat-service/mallchat-log-service` | 有 DTO/VO 基础，但 add/delete 仍暴露裸 `Boolean`，删除和列表空请求存在 NPE 风险 |
| file | `mallchat-api/mallchat-api-file` | `mallchat-service/mallchat-file-service` | `multipart + bizType` 应保留，响应已是 `FileVO`；全局 multipart 10MB 与业务枚举上限不一致 |
| notification | `mallchat-api/mallchat-api-notification` | `mallchat-service/mallchat-notification-service` | 契约问题集中：裸 ID、裸操作结果、批量数量、`DeleteRequest`、`@RequestParam("id")` 和业务通知必填边界 |

## 3. 初始 P0/P1/P2 清单

以下清单记录 #59 审查阶段的待修正项；完成状态见第 7-14 节。

### 3.1 P0

本轮未发现 P0。没有证据显示当前支撑领域存在会直接导致服务不可启动、Feign 路径硬断裂或核心生产链路完全不可用的问题。

### 3.2 P1

| 领域 | 类型 | 文件 | 问题 | 风险 | 后续 Issue |
| --- | --- | --- | --- | --- | --- |
| log | 参数契约 | `UserLoginLogController.java`、`OperationLogController.java`、`ApiAccessLogController.java`、`FileUploadRecordController.java` | 4 个 delete 接口使用 `deleteRequest.getId() <= 0`，未覆盖 `id == null` | 空 JSON 可能触发 NPE，而不是稳定的 `PARAMS_ERROR` | [#60](https://github.com/StephenQiu30/mallchat-cloud/issues/60) |
| log | 参数契约 | 同上 | 4 个 list 接口在读取 `queryRequest.getCurrent()` 前未判空 | 空请求可能 NPE，错误响应不可控 | [#60](https://github.com/StephenQiu30/mallchat-cloud/issues/60) |
| log | 响应契约 | `LogFeignClient.java` 与 4 个 log Controller | add/delete 使用 `BaseResponse<Boolean>` | 不利于前端生成和后续扩展操作结果字段 | [#60](https://github.com/StephenQiu30/mallchat-cloud/issues/60) |
| log | 请求语义 | log Service / Controller | add 空请求可能被包装为 `success(false)` | 调用方难以区分参数错误和业务成功但结果为 false | [#60](https://github.com/StephenQiu30/mallchat-cloud/issues/60) |
| file | 上传边界 | `application.yml`、`FileUploadValidator.java` | Spring multipart 全局 10MB，小于 `chat_voice` 20MB 和 `chat_video` 100MB 业务上限 | 较大语音/视频会在业务校验前被容器拒绝，接口承诺与实际不一致 | [#61](https://github.com/StephenQiu30/mallchat-cloud/issues/61) |
| file | 契约守护 | `FileController.java`、`FileFeignClient.java` | 上传契约依赖 `@RequestPart("file") + @RequestParam("bizType")`，暂无 Controller 契约测试 | 后续容易被机械 DTO 化，破坏 multipart 调用 | [#61](https://github.com/StephenQiu30/mallchat-cloud/issues/61) |
| notification | 响应契约 | `NotificationController.java`、`NotificationFeignClient.java` | 存在 `BaseResponse<List<Long>>`、`BaseResponse<Long>`、`BaseResponse<Boolean>`、`BaseResponse<Integer>` | 响应结构对前端生成和批量结果解释不稳定 | [#62](https://github.com/StephenQiu30/mallchat-cloud/issues/62) |
| notification | 请求契约 | `NotificationController.java` | 删除接口复用通用 `DeleteRequest`，详情接口使用 `@RequestParam("id")` | 业务含义不清，和 DTO Request 规范不一致 | [#62](https://github.com/StephenQiu30/mallchat-cloud/issues/62) |
| notification | 业务通知边界 | `NotificationCreateRequest.java`、`NotificationServiceImpl.java` | `userId`、`type` 等业务通知必填字段未形成强契约 | MQ 推送依赖 userId，缺失时可能在下游失败 | [#62](https://github.com/StephenQiu30/mallchat-cloud/issues/62) |
| notification | Feign 同步 | `NotificationFeignClient.java`、`NotificationController.java` | Feign 只暴露 get/list/internal add，未覆盖 read/unread/batch 等 Controller 能力 | 跨服务契约边界不清，后续需明确哪些能力允许 RPC | [#62](https://github.com/StephenQiu30/mallchat-cloud/issues/62) |

### 3.3 P2

| 领域 | 类型 | 问题 | 建议 |
| --- | --- | --- | --- |
| log | 查询语义 | 多个 QueryRequest 声明 `searchText`，对应 Service wrapper 暂未消费 | 后续补测试后决定删除字段或实现搜索，避免“参数有效但无效果” |
| log | Feign 边界 | `LogFeignClient` 暴露 admin list/page，但不暴露 delete | 明确 Feign 是内部写日志优先，还是也作为后台查询 RPC 契约 |
| file | OpenAPI 表达 | `FileController.uploadFile` 未显式声明 `consumes = multipart/form-data` | 可与 Feign 保持一致，提升接口文档清晰度 |
| file | 上传响应 | `FileVO` 当前只有 `url/key/fileName/size` | 如端侧需要立即区分文件类型，再评估补充 `bizType/contentType/suffix`，当前不强制 |
| notification | Bean Validation | DTO 上已有局部注解，但 Controller 仍以手写校验为主 | 后续可统一为 Bean Validation + 少量业务校验，不在 #59 中改 |
| notification | 排序参数 | `sortOrder` 如显式传 null 可能存在空指针风险 | 在 #62 的契约测试中覆盖后修正 |

### 3.4 当前修正状态

| 领域 | 修正状态 | 当前证据 |
| --- | --- | --- |
| log | 已修正 | `LogIdRequest` 替代通用 `DeleteRequest`；add/delete 返回 `LogOperationResultVO`；空请求兜底已补；log-service focused tests 通过 |
| file | 已修正 | 上传接口显式 `multipart/form-data`；Spring multipart 上限对齐 100MB；file-service focused tests 通过 |
| notification | 已修正 | 通知 ID、ID 列表、操作结果、未读数、批量结果均有 VO；详情和删除使用 `NotificationIdRequest`；业务通知必填注解已补；notification-service focused tests 通过 |
| Feign 消费方 | 已验证 | 全仓 `mvn -B -DskipTests compile` 通过；通知 Feign 返回类型变化已同步 chat 侧 4 个调用点 |

## 4. 过度设计拦截

1. file 上传接口不要为了 DTO 统一改成 JSON 请求体；`multipart/form-data` 的 `file + bizType` 是合理边界。
2. m12 不引入统一接口生成器、不新增全局 `PageVO<T>`，只记录 `Page<VO>` 对前端生成的长期风险。
3. notification 的简单 ID / 操作结果可以复用少量清晰 VO，不按每个接口创建大量空壳响应类。
4. log 的 Feign 边界先明确用途，再决定是否补 delete RPC，不为了“完全对称”增加未使用接口。

## 5. 后续 TDD 输入

| Issue | RED 建议 | GREEN 验收命令 |
| --- | --- | --- |
| [#60](https://github.com/StephenQiu30/mallchat-cloud/issues/60) | 新增 log Controller 契约测试，先证明空 delete/list/add 请求和裸 Boolean 契约缺口 | `mvn -B -pl mallchat-service/mallchat-log-service -am -Dsurefire.failIfNoSpecifiedTests=false test` |
| [#61](https://github.com/StephenQiu30/mallchat-cloud/issues/61) | 新增 file 上传契约测试，覆盖 multipart 参数名、非法 `bizType`、业务上限与配置一致性 | `mvn -B -pl mallchat-service/mallchat-file-service -am -Dsurefire.failIfNoSpecifiedTests=false test` |
| [#62](https://github.com/StephenQiu30/mallchat-cloud/issues/62) | 参考 `ChatApiContractConsistencyTest`，新增 notification 契约护栏，先暴露裸响应、`DeleteRequest`、`@RequestParam` 和必填缺口 | `mvn -B -pl mallchat-service/mallchat-notification-service -am -Dsurefire.failIfNoSpecifiedTests=false test` |

## 6. #59 验收记录

| 命令 | 结果 | 说明 |
| --- | --- | --- |
| `openspec validate --all --strict` | 通过 | 21 items passed |
| `git diff --check` | 通过 | 无空白或补丁格式问题 |
| `bash scripts/validate-repository.sh` | 通过 | 仓库规范校验通过 |

## 7. #60 TDD 记录

| 阶段 | 命令 | 结果 | 说明 |
| --- | --- | --- | --- |
| RED | `mvn -B -pl mallchat-service/mallchat-log-service -am -Dtest=LogApiContractConsistencyTest -Dsurefire.failIfNoSpecifiedTests=false test` | 失败 | 契约护栏发现 16 个违例：4 个 delete 使用通用 `DeleteRequest`，Controller / Feign 多处返回 `BaseResponse<Boolean>` |
| GREEN | `mvn -B -pl mallchat-service/mallchat-log-service -am -Dtest=LogApiContractConsistencyTest -Dsurefire.failIfNoSpecifiedTests=false test` | 通过 | 新增 `LogIdRequest`、`LogOperationResultVO` 后，契约护栏 1/1 通过 |
| Focused | `mvn -B -pl mallchat-service/mallchat-log-service -am -Dsurefire.failIfNoSpecifiedTests=false test` | 通过 | log-service 4 tests，0 failures，0 errors |

## 8. #60 修正内容

1. 新增 `LogIdRequest`，替代 log 删除接口中的通用 `DeleteRequest`。
2. 新增 `LogOperationResultVO`，替代 log add/delete 和 `LogFeignClient` 中的裸 `Boolean` 响应。
3. 为 4 个 log Controller 的 add/list/delete 入口补充空请求兜底，避免空请求 NPE 或 `success(false)` 语义。
4. 同步 `LogFeignClient` 返回类型，保持 Controller 与 Feign 契约一致。
5. 保持 Mapper / Convert / Entity 不变，不做无收益重构。

## 9. #61 TDD 记录

| 阶段 | 命令 | 结果 | 说明 |
| --- | --- | --- | --- |
| RED | `mvn -B -pl mallchat-service/mallchat-file-service -am -Dtest=FileUploadContractConsistencyTest -Dsurefire.failIfNoSpecifiedTests=false test` | 失败 | 2 个断言按预期失败：上传接口未显式声明 multipart consumes；Spring multipart 上限仍为 10MB |
| GREEN | `mvn -B -pl mallchat-service/mallchat-file-service -am -Dtest=FileUploadContractConsistencyTest -Dsurefire.failIfNoSpecifiedTests=false test` | 通过 | 上传接口保留 `multipart/form-data`，全局上限对齐业务最大 100MB |
| Focused | `mvn -B -pl mallchat-service/mallchat-file-service -am -Dsurefire.failIfNoSpecifiedTests=false test` | 通过 | file-service 18 tests，0 failures，0 errors |

## 10. #61 修正内容

1. 为 `FileController.uploadFile` 显式声明 `consumes = multipart/form-data`，与 `FileFeignClient` 保持一致。
2. 将 file-service Spring multipart `max-file-size` / `max-request-size` 从 10MB 调整为 100MB，覆盖 `FileUploadValidator` 中 `chat_video` 的最大业务上限。
3. 新增 `FileUploadContractConsistencyTest` 固化 multipart 边界和上传大小配置，防止后续被机械 DTO 化或配置回退。
4. 未修改 `FileVO` 字段，也未引入上传 JSON DTO，保持现有前端和 Feign 调用方式。

## 11. #62 TDD 记录

| 阶段 | 命令 | 结果 | 说明 |
| --- | --- | --- | --- |
| RED | `mvn -B -pl mallchat-service/mallchat-notification-service -am -Dtest=NotificationApiContractConsistencyTest -Dsurefire.failIfNoSpecifiedTests=false test` | 失败 | 契约护栏发现 14 个接口违例：裸 `Long/Boolean/Integer/List<Long>` 响应、通用 `DeleteRequest`、`@RequestParam` 和 `NotificationCreateRequest` 必填注解缺口 |
| GREEN | `mvn -B -pl mallchat-service/mallchat-notification-service -am -Dtest=NotificationApiContractConsistencyTest -Dsurefire.failIfNoSpecifiedTests=false test` | 通过 | 新增通知 DTO/VO 薄封装后，契约护栏 4/4 通过 |
| Focused | `mvn -B -pl mallchat-service/mallchat-notification-service -am -Dsurefire.failIfNoSpecifiedTests=false test` | 通过 | notification-service 20 tests，0 failures，0 errors |
| Compile | `mvn -B -pl mallchat-service/mallchat-chat-service -am -DskipTests compile` | 通过 | chat-service 编译通过，验证 `NotificationFeignClient` 返回类型变化未破坏调用方 |

## 12. #62 修正内容

1. 新增 `NotificationIdRequest`，替代通知详情和删除接口中的裸 ID / 通用 `DeleteRequest`。
2. 新增 `NotificationIdVO`、`NotificationIdListVO`、`NotificationOperationResultVO`、`NotificationUnreadCountVO`、`NotificationBatchOperationResultVO`，替代裸 ID、裸布尔值、裸数量和 `List<Long>` 响应。
3. 为 `NotificationCreateRequest` 的 `userId/title/content/type` 补充 Bean Validation 必填注解，业务通知调用边界更明确。
4. `NotificationController` 的管理分页返回 `Page<NotificationVO>`，不再直接暴露 `Notification` 实体。
5. `NotificationFeignClient` 对齐 Controller 契约：详情查询使用 `NotificationIdRequest`，业务通知返回 `NotificationIdVO`。
6. 同步 chat 侧 4 个业务通知调用的返回类型，保持跨服务调用可编译。
7. 修正通知查询 `sortOrder` 为空时的 NPE 风险，并用契约测试固化。
8. 补充 `NotificationIdRequest` query 参数绑定契约测试，覆盖 `GET /notification/get/vo?id=1` 的 DTO 绑定风险。
9. 未扩展 read/unread/batch 的 Feign 能力，不为了接口对称增加未使用 RPC。

## 13. #63 Code Review 与验收记录

| 项目 | 结果 | 说明 |
| --- | --- | --- |
| 只读 Code Review | 通过，已处理阻塞项 | 子智能体发现 A-028 前半段口径仍像“当前未修复状态”，已改为“初始审查基线”并新增当前修正状态 |
| 过度设计审查 | 通过 | file 保留 multipart；notification 只新增少量薄 VO；未新增全局接口生成器、全局 PageVO 或未使用 Feign 能力 |
| Feign 调用方审查 | 通过 | notification 4 个 chat 调用点已同步为 `NotificationIdVO`；log Feign 返回类型调用方未消费 data，无需额外改造 |
| 绑定契约补充 | 通过 | 新增 `NotificationIdRequest` query 参数绑定测试，覆盖详情 GET DTO 绑定风险 |
| 文档一致性审查 | 通过 | A-028 同步记录 #60/#61/#62 RED/GREEN/Focused 和 #63 验收结论 |

## 14. #63 验收命令

| 命令 | 结果 | 说明 |
| --- | --- | --- |
| `mvn -B -pl mallchat-service/mallchat-log-service -am -Dsurefire.failIfNoSpecifiedTests=false test` | 通过 | log-service 4 tests |
| `mvn -B -pl mallchat-service/mallchat-file-service -am -Dsurefire.failIfNoSpecifiedTests=false test` | 通过 | file-service 18 tests |
| `mvn -B -pl mallchat-service/mallchat-notification-service -am -Dsurefire.failIfNoSpecifiedTests=false test` | 通过 | notification-service 20 tests |
| `mvn -B -DskipTests compile` | 通过 | 全仓 25 个 Maven 模块编译通过 |
| `mvn -B -pl mallchat-service/mallchat-user-service,mallchat-service/mallchat-file-service,mallchat-service/mallchat-chat-service,mallchat-service/mallchat-notification-service,mallchat-service/mallchat-ai-service -am -DskipTests compile` | 通过 | 跨消费者 23 个 Maven 模块编译通过 |
| `openspec validate --all --strict` | 通过 | 21 items passed |
| `git diff --check` | 通过 | 无空白或补丁格式问题 |
| `bash scripts/validate-repository.sh` | 通过 | 仓库规范校验通过 |

## 15. 变更记录

| 日期 | 作者 | 版本 | 变更说明 |
| --- | --- | --- | --- |
| 2026-05-21 | StephenQiu30 | 0.1.0 | 初始化 m12 支撑领域工程化一致性审查清单 |
| 2026-05-21 | StephenQiu30 | 0.1.1 | 记录 #60 log 契约 TDD 修正和 focused tests 结果 |
| 2026-05-21 | StephenQiu30 | 0.1.2 | 记录 #61 file 上传边界 TDD 修正和 focused tests 结果 |
| 2026-05-21 | StephenQiu30 | 0.1.3 | 记录 #62 notification 契约 TDD 修正、focused tests 和 chat 侧编译验证结果 |
| 2026-05-21 | StephenQiu30 | 0.1.4 | 按 Code Review 修正初始基线口径，记录 #63 只读审查和整体验收结果 |
