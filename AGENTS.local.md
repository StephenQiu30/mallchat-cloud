# AGENTS.local.md

本文件用于记录放在具体项目中的局部规范性配置，与 `AGENTS.md` 中的全局协作规则进行区分。

## 使用边界

1. `AGENTS.md` 存放长期稳定的 Codex 全局规则、角色协作原则和交付格式。
2. `AGENTS.local.md` 存放当前项目特有的规范、路径、命令、环境约束和临时协作约定。
3. 当局部规范与全局规则冲突时，应优先确认项目上下文，并以更具体、更贴近当前项目的规则为准。

## 当前项目规范

1. 本项目内的角色配置放在 `.claude/agents/` 目录。
2. 本项目内的可复用流程放在 `.claude/skills/` 目录。
3. 本项目按 `StephenQiu30/stephen-cladue` 接入 Claude 规范资产，包括角色、skills、Workpad、TDD、提交、PR 和 Linear 协作流程。
4. 项目级 IM 生产化、接口契约、CI focused tests、docs 分类和数据库事实源规则以本仓库 `AGENTS.md`、`CLAUDE.md`、`WORKFLOW.md`、`scripts/validate-repository.sh` 和 `.github/` 配置为准，不用模板覆盖本地增强。
5. 本项目不再依赖额外规格框架；需求、设计、验收和过程闭环分别沉淀在 `docs/`、GitHub Issue、PR 模板和 `.claude/` 协作资产中。
