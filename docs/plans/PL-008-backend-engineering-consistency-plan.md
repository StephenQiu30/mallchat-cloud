---
layer: Plan
doc_no: "PL-008"
audience:
  - PM
  - Dev
  - QA
  - Ops
feature_area: backend-engineering-consistency
purpose: "编排 MallChat 后端工程化一致性治理的分批任务、审查流程、TDD 与 Code Review 验收门禁。"
canonical_path: "docs/plans/PL-008-backend-engineering-consistency-plan.md"
status: review
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "docs/design/D-003-backend-engineering-consistency-design.md"
  - "AGENTS.md"
outputs:
  - "后端工程化一致性治理批次计划"
  - "m11+ 分批治理任务基线"
triggers:
  - "启动后端工程化治理"
  - "新增或修改后端接口、DTO、VO、Enum、Entity、Service、Controller"
downstream:
  - "docs/acceptance/A-026-m11-chat-consistency-acceptance.md"
  - "后续 GitHub Issue 与 PR"
---

# 后端工程化一致性治理计划

## 1. 背景

当前 MallChat 后端已经完成 IM 生产化 P0、P1、P2 多轮任务，接口和模型数量增加。为了保证后续功能继续保持可读、可测、可扩展，需要用按领域分批的方式治理接口风格、模型分层和代码职责边界。

## 2. 目标

1. 以 `chat` 主领域为第一批，建立后端工程化一致性治理样板。
2. 每批先审查再修正，避免在没有事实清单时直接改代码。
3. 行为变化必须 TDD，完成后必须 Code Review。
4. 将治理结果沉淀到 docs、验收文档和后续 PR checklist。

## 3. 非目标

1. 不一次性修全仓所有风格问题。
2. 不在治理 PR 中新增业务功能。
3. 不引入复杂静态分析平台或代码生成器。
4. 不破坏已稳定接口兼容性。

## 4. 批次计划

| 批次 | 范围 | 主要检查点 | 计划 PR |
| --- | --- | --- | --- |
| E1 | `chat` 领域 | `mallchat-api-chat` DTO/VO/Enum；`mallchat-chat-service` Controller/Service/Entity/Convert/Mapper；前端可生成的 DTO Request / VO Response 契约 | `m11` |
| E2 | `log/file/notification` 支撑领域 | 查询请求、日志记录、文件上传、通知事实和返回 VO 风格 | `m12` |
| E3 | `user/ai/gateway/common` 基础领域 | 权限、公共响应、公共工具、跨服务契约和网关边界 | `m13` |
| E4 | 工程化守护 | 轻量脚本或测试检查目录、命名、注解和 PR 规则 | `m14` |

## 5. 每批执行流程

每批按以下顺序推进：

1. 调研当前领域文件结构、接口契约、测试覆盖和历史 OpenSpec。
2. 输出事实清单：具体文件、具体不一致表现、是否影响调用方。
3. 做任务审查：按 P0、P1、P2 分级。
4. 明确非目标：本批不做的重构、功能和风格偏好。
5. 确认测试策略：行为变化先写 RED 测试；非行为变化跑编译和 focused tests。
6. 实现最小修正。
7. 运行验证命令。
8. 触发只读 Code Review。
9. 修复 Critical / Important 问题。
10. 写验收文档并提交 PR。

## 6. 第一批 E1 Chat 治理草案

### 6.1 审查范围

1. `mallchat-api/mallchat-api-chat/src/main/java/com/stephen/cloud/api/chat/model/dto`
2. `mallchat-api/mallchat-api-chat/src/main/java/com/stephen/cloud/api/chat/model/vo`
3. `mallchat-api/mallchat-api-chat/src/main/java/com/stephen/cloud/api/chat/model/enums`
4. `mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/controller`
5. `mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/service`
6. `mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/model/entity`
7. `mallchat-service/mallchat-chat-service/src/main/java/com/stephen/cloud/chat/convert`

### 6.2 审查输出

E1 第一阶段不直接修改代码，先输出审查清单；确认 P0/P1/P2 后，再按 TDD 流程进入最小修正：

| 字段 | 说明 |
| --- | --- |
| 文件 | 具体路径 |
| 类型 | DTO、VO、Enum、Controller、Service、Entity、Convert |
| 问题 | 不一致表现 |
| 风险 | 兼容性、可读性、权限、测试或扩展风险 |
| 等级 | P0、P1、P2 |
| 建议 | 修复、延后或不处理 |

### 6.3 E1 修正原则

1. P0 必须本批修复。
2. P1 尽量本批修复，但不能扩大到跨领域大改。
3. P2 只记录，不作为本批验收阻塞项。
4. 接口路径、字段名、枚举 code 只有在确认无兼容风险或有迁移方案时才修改。
5. 所有面向前端或跨服务生成契约的接口，都需要收敛为 DTO Request 入参和 VO Response 出参；涉及现有接口契约变化时必须单独列出兼容性影响和 TDD 验收。

### 6.4 E1 验收命令

```bash
mvn -B -pl mallchat-service/mallchat-chat-service -am test
mvn -B -DskipTests compile
openspec validate --all --strict
git diff --check
bash scripts/validate-repository.sh
```

如果只做 docs 或审查清单，至少运行：

```bash
openspec validate --all --strict
git diff --check
bash scripts/validate-repository.sh
```

## 7. TDD 规则

1. 行为变化必须先写 RED 测试。
2. 接口契约变化必须能通过测试或编译失败证明缺口，包括 `@RequestParam` 收敛为 DTO、裸返回值收敛为 VO、字段校验注解和 Schema 文档一致性。
3. Convert、Enum lookup、权限、幂等、缓存、推送边界变化都属于行为变化。
4. 纯文档和纯审查清单不强制 RED，但必须保留验证命令。

## 8. Code Review 规则

1. 每批 PR 创建前必须先安排只读 reviewer。
2. reviewer 输出 Critical、Important、Minor。
3. Critical 和 Important 必须处理后再合并。
4. Minor 可以记录到对应验收文档或后续批次。
5. review 重点包括：风格一致性、过度设计、兼容性、权限、事务、幂等、缓存、推送和测试有效性。

## 9. PR 与提交规则

每批 PR 使用 m 系列继续推进：

1. `test:` 红灯测试。
2. `impl:` 最小实现。
3. `refactor:` 行为不变整理。
4. `docs:` 规范、计划和验收文档。
5. `chore:` CI 或脚本检查。

如果批次只有审查文档，没有代码行为变化，可以使用 `docs:` 提交并在 PR 中说明不适用 RED。

## 10. 风险控制

1. 每批只处理一个领域，避免跨服务大范围变更。
2. 不把风格偏好包装成 P0。
3. 不用工具代替人工审查。
4. 不用治理名义新增业务功能。
5. 每批结束后都要更新验收文档和后续风险清单。

## 11. 任务审查结论

1. 编排顺序有效：先 `chat` 主领域建立样板，再扩展到支撑领域、基础领域和工程化守护，避免全仓扫荡。
2. 交付粒度可控：每批对应一个 m 系列 PR，Issue 与 PR 按 Epic 关联，不按零碎文件频繁提交。
3. TDD 约束清楚：行为变化必须 RED/GREEN；纯文档或审查清单不伪造 RED，只保留仓库校验。
4. Code Review 约束清楚：Critical / Important 必须修复，Minor 可进入验收风险或后续批次。
5. DTO Request / VO Response 契约需要单独审查和迁移，避免影响前端接口生成。
6. 过度设计已拦截：本计划不引入新框架、生成器、平台层或大规模重排目录。

## 12. 变更记录

| 日期 | 作者 | 版本 | 变更说明 |
| --- | --- | --- | --- |
| 2026-05-21 | StephenQiu30 | 0.1.0 | 初始化后端工程化一致性治理计划 |
