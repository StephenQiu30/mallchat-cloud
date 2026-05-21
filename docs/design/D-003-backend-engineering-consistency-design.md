---
layer: Design
doc_no: "D-003"
audience:
  - Dev
  - QA
  - Ops
feature_area: backend-engineering-consistency
purpose: "定义 MallChat 后端工程化一致性治理的设计边界、代码风格规则、TDD 与 Code Review 门禁。"
canonical_path: "docs/design/D-003-backend-engineering-consistency-design.md"
status: review
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "AGENTS.md"
  - "docs/plans/PL-006-im-p2-backend-experience-plan.md"
  - "docs/plans/PL-007-multi-client-e2e-matrix-plan.md"
outputs:
  - "后端工程化一致性治理设计"
  - "分批治理代码风格规则"
triggers:
  - "新增后端 API、DTO、VO、Enum、Entity、Service 或 Controller"
  - "进入后端工程化一致性治理批次"
downstream:
  - "docs/plans/PL-008-backend-engineering-consistency-plan.md"
  - "后续 m11+ 分批治理 PR"
---

# 后端工程化一致性治理设计

## 1. 背景

MallChat 后端已经形成 `mallchat-api-*`、`mallchat-service/*`、`mallchat-common/*` 的微服务分层，并围绕 IM 能力补齐了好友、群聊、消息、会话、动态、通知、文件、日志和治理能力。随着功能增加，接口、DTO、VO、Enum、Entity、Convert、Controller、Service 的风格需要从“原则一致”进一步收敛为“可审查、可验证、可分批修正”的工程化规范。

本设计采用按领域分批治理的方式，先从 `chat` 主领域开始，再扩展到支撑领域和基础领域。治理目标是提升可读性、一致性和后续扩展稳定性，不进行全仓大重构。

## 2. 目标

1. 建立后端接口、模型、DTO、VO、Enum、Convert、Service 和 Controller 的一致性规则。
2. 使用分批治理方式修正现有明确不一致代码，每批都有任务审查和验收边界。
3. 将 TDD 和 Code Review 写入治理门禁，减少逻辑漏洞和回归风险。
4. 保持 MVP 和最小可用闭环，不为单个问题引入平行架构、复杂工具或过度抽象。

## 3. 非目标

1. 不做全仓一次性格式化或大规模目录迁移。
2. 不引入新的 Web 框架、ORM、代码生成器、复杂静态分析平台或统一脚手架。
3. 不为了统一命名破坏已稳定的接口兼容性。
4. 不把低收益命名偏好列为必须修复项。
5. 不在设计阶段修改业务代码。

## 4. 分层边界

### 4.1 API 契约

1. 跨服务 DTO、VO、Enum、Feign Client 继续放在 `mallchat-api-*`。
2. API 模块只表达契约，不放业务实现、Mapper、数据库实体或服务端专用工具。
3. 新增请求对象优先放在对应 `model/dto`，返回对象放在 `model/vo`，枚举放在 `model/enums`。
4. 不把临时业务对象塞进 service 模块后再跨服务引用。

### 4.2 Service 实现

1. 业务实现继续放在 `mallchat-service/*`。
2. Entity、Mapper、Service、ServiceImpl、Convert 按当前项目目录组织。
3. ServiceImpl 负责事务、权限、幂等、缓存、落库、推送和转换协调。
4. Controller 不承载复杂业务逻辑。

### 4.3 Common 能力

1. `mallchat-common/*` 只承载跨服务复用能力，例如认证、缓存、Web、MySQL、RabbitMQ、WebSocket、日志切面。
2. 单个业务域独有能力不提前上升到 common。
3. common 变更必须说明调用方和兼容性影响。

## 5. 代码风格规则

### 5.1 Controller

1. Controller 只做参数接收、基础空值或 ID 校验、登录态读取、Service 调用和 `ResultUtils.success` 返回。
2. 路径按领域聚合，例如 `/chat/message/*`、`/chat/room/*`、`/chat/moment/*`。
3. 用户侧接口、管理接口、内部接口应通过路径、权限注解和认证注解表达边界。
4. 不在 Controller 中写复杂权限、事务、缓存回源、消息推送或实体拼装。

### 5.2 DTO 和 QueryRequest

1. 命名使用 `*Request` 或已有 `*QueryRequest` 风格。
2. 分页查询继承现有 `PageRequest`，不新增平行分页对象。
3. 字段命名贴合实体和业务语义，例如 `roomId`、`messageId`、`momentId`、`targetId`、`bizId`。
4. 请求字段应保留 `@Schema`，必填、长度、范围使用现有校验注解。
5. DTO 不放业务计算方法、数据库实体对象或跨层实现依赖。

### 5.3 VO

1. 接口返回使用 VO，不直接返回 Entity。
2. VO 面向端侧稳定展示，不暴露 `isDelete`、内部临时状态或调试字段。
3. 分页返回复用当前 `Page<T>` 风格，不新增自定义分页包装。
4. VO 转换优先由 Convert 或 Service 内明确方法完成，Controller 不手写复杂拼装。

### 5.4 Entity

1. Entity 继续放在 service 模块的 `model/entity`。
2. Entity 使用当前 MyBatis Plus 注解风格。
3. 数据库字段、实体字段、`sql/mallchat.sql` 和 migration 需要同步。
4. 逻辑删除、时间字段、唯一键、索引命名沿用当前 SQL 风格。
5. Entity 不引用 DTO 或 VO。

### 5.5 Enum

1. 跨模块共享枚举放在对应 `mallchat-api-*` 的 `model/enums`。
2. 枚举字段保持 `code` / `desc` 风格，提供必要 lookup 方法。
3. 不用裸数字或裸字符串散落在 Service 中；已有历史代码按批次逐步收敛。
4. 不为单个条件创建复杂策略枚举。

### 5.6 Convert

1. Convert 类保持静态转换工具风格。
2. Convert 只做简单字段转换，不访问数据库、不调用远程服务、不做权限判断。
3. 复杂聚合转换可以留在 Service，但方法名必须清楚，例如 `toVO`、`buildSessionVO`。

### 5.7 Service 和 ServiceImpl

1. Service 接口表达业务能力，ServiceImpl 处理业务流程。
2. 复杂逻辑优先拆成语义清楚的私有方法，并通过现有 Service 或接口测试覆盖，不为单个场景抽象平台层。
3. 涉及推送、MQ、通知的流程保持“事实先落库，推送失败不破坏事实”。
4. 涉及权限的流程必须能从方法名或测试看出本人、好友、群成员、管理员等边界。

## 6. 分批治理策略

治理按领域推进：

1. E1 `chat`：IM 主领域，先建立样板。
2. E2 `log/file/notification`：支撑领域，统一查询、记录、通知和文件边界。
3. E3 `user/ai/gateway/common`：基础领域，处理通用响应、权限、公共工具和跨服务契约边界。
4. E4 工程化守护：在已有规则稳定后，再考虑轻量脚本或测试检查目录、命名和注解。

每批必须先输出事实清单、影响判断、修正等级和非目标，再进入实现。

## 7. 修正等级

1. `P0`：会造成接口不一致、运行错误、权限缺口或明显误用，必须修复。
2. `P1`：影响可读性、扩展性或团队协作，建议本批修复。
3. `P2`：只属于低收益风格偏好，记录但不在本批强制修复。

## 8. TDD 门禁

1. 涉及行为变化、接口契约变化、转换逻辑变化、权限边界变化时，必须先写 RED 测试。
2. RED 测试必须能证明当前缺口，例如编译失败、断言失败、权限未拦截或返回结构不一致。
3. GREEN 阶段只写让测试通过的最小实现。
4. 纯文档、纯注释、纯命名整理可以不强制 RED，但必须说明不涉及行为变化，并跑编译或轻量检查。
5. 每批 PR 必须记录 RED 证据、GREEN 命令、影响范围和未覆盖风险。

## 9. Code Review 门禁

1. 每批实现完成后必须安排只读 reviewer 审查。
2. reviewer 重点检查是否违反现有分层、是否过度抽象、是否引入平行架构、是否存在兼容性风险。
3. 涉及权限、幂等、事务、推送、缓存边界的变更必须重点审查。
4. Critical 和 Important 反馈必须在合并前处理。
5. Minor 反馈可以记录到验收文档或后续批次。

## 10. 验收方式

第一批 `chat` 治理建议至少运行：

```bash
mvn -B -pl mallchat-service/mallchat-chat-service -am test
mvn -B -DskipTests compile
openspec validate --all --strict
git diff --check
bash scripts/validate-repository.sh
```

如果修改 API DTO、VO 或 Enum，需要确认对应 API 模块参与编译。若修改非 chat 领域，则按对应模块补充 focused tests。

## 11. 风险与边界

1. 接口兼容性优先于风格统一；已被端侧使用的字段、枚举 code、路径不做无兼容方案的破坏性改名。
2. 治理 PR 不应夹带新业务功能。
3. 每批只处理当前领域，不跨多个领域扫荡式重构。
4. 工程化守护脚本只能作为辅助，不替代人工审查和测试。

## 12. 设计自审结论

1. 逻辑闭环：设计只定义分层、风格、TDD 和 Code Review 门禁；具体修正进入对应计划、Issue、PR 和验收文档。
2. 风格一致性：规则基于当前 `chat-*` / `Chat*` 命名、MyBatis Plus、`ResultUtils.success`、`Page<T>` 和 Convert 工具类风格，不引入平行体系。
3. 兼容性：接口路径、字段名、枚举 code、数据库字段不因风格偏好直接破坏兼容。
4. 不过度设计：暂不引入代码生成器、复杂静态分析平台、新框架或统一平台层。
5. 测试优先：行为变化进入实现批次时必须先 RED；纯文档阶段以 OpenSpec、仓库校验和 diff 检查作为验收。

## 13. 变更记录

| 日期 | 作者 | 版本 | 变更说明 |
| --- | --- | --- | --- |
| 2026-05-21 | StephenQiu30 | 0.1.0 | 初始化后端工程化一致性治理设计 |
