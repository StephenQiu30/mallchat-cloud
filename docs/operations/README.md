# Operations 文档

本目录存放运维、发布和协作操作类文档。

## 适合放入

1. Git 提交规范。
2. PR 创建、更新和合并流程。
3. 合并前 tag 与回滚说明。
4. 发布、部署和运行手册。

## 不适合放入

1. 产品需求。
2. 技术方案正文。
3. 测试报告主体。
4. 单次执行命令流水账或已由 OpenSpec tasks 归档的过程记录。

## 命名建议

使用 `{layer}-{doc_no}-{主题}.md` 格式，例如 `O-001-github-ci.md`。

## 文档清单

| 文档 | 说明 |
| --- | --- |
| [O-001-github-ci.md](./O-001-github-ci.md) | GitHub CI 与仓库验证流程 |
| [O-002-websocket-runtime-contract.md](./O-002-websocket-runtime-contract.md) | WebSocket 运行契约与联调说明 |
| [O-003-im-production-runbook.md](./O-003-im-production-runbook.md) | MallChat 后端生产上线、健康检查、故障定位、回滚和恢复 Runbook |
