---
layer: acceptance
doc_no: A-001
audience: Dev, QA, Ops
purpose: 记录 AGENTS 规范迁移和后端 CI 门禁调整的验收结论
owner: StephenQiu30
inputs: AGENTS.md, .github/workflows/ci.yml
outputs: 后端规范迁移与 CI 结论
triggers: 规范入口迁移、CI 门禁变更
downstream: GitHub Actions
---

# AGENTS 迁移与后端 CI 验收结论

## 1. 计划范围

1. 将 `CLAUDE.md` / `CLAUDE.local.md` 迁移为 `AGENTS.md` / `AGENTS.local.md`。
2. 同步贡献说明、docs 说明和仓库校验脚本中的规范入口命名。
3. 将 GitHub Actions 调整为 Java 后端真实可执行的门禁：仓库结构校验、Java 21、Maven 编译。
4. 升级 Lombok 到 `1.18.46`，补齐 `mallchat-common-websocket` 的 Lombok 依赖，并在父 POM 中显式声明 Lombok annotation processor，使干净 Maven 编译环境可以生成 getter 和日志字段。
5. 不提交构建产物、临时过程记录或一次性排查日志。

## 2. 已执行命令

```bash
bash scripts/validate-repository.sh
mvn -B -DskipTests compile
```

## 3. 测试结论

1. 仓库结构与规范入口校验已通过。
2. `mvn -B -DskipTests compile` 已通过。
3. CI 已从不存在的 `npm test` 改为 `mvn -B -DskipTests compile`，避免后端仓库在 GitHub Actions 中执行无效 npm 命令。
4. 本次未修改 Java 业务逻辑，未新增后端单元测试。
5. WebSocket 模块缺少 Lombok 直接依赖、显式 annotation processor 和新版 Java 兼容 Lombok 版本的问题已修复，避免 CI 干净编译时出现 `getPort` / `log` 符号缺失。

## 4. 残余风险

1. 后续修改 chat 核心逻辑时，应继续运行 chat-service 定向回归测试。

## 5. 变更记录

| 日期 | 作者 | 变更说明 |
| --- | --- | --- |
| 2026-05-19 | Stephen Qiu | 初始化 AGENTS 迁移与后端 CI 验收结论 |
