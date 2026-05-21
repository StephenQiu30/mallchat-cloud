## Why

MallChat 已有 `operation_log` 审计事实和管理员分页列表，但当前查询请求只覆盖部分精确条件，缺少上线排查常用的操作人名称和时间范围过滤。管理后台需要先拿到稳定的后端检索契约，后续再补 Admin 页面。

## What Changes

- 扩展操作日志查询请求，支持按操作人 ID、操作人名称、模块、操作、业务 ID、成功状态、客户端 IP 和创建时间范围检索。
- 保持现有 `/log/operation/list/page` 管理员接口、DTO/VO、Service/Wrapper 风格，不新增平行审计表或搜索服务。
- 补充 focused tests，先验证查询条件生成，再实现最小代码。

## Non-Goals

- 不实现 Admin 前端页面。
- 不引入 Elasticsearch、OLAP、异步报表或新的审计服务。
- 不改变操作日志写入链路、脱敏规则或业务服务的审计上报方式。
- 不在本次默认实现综合关键字搜索，避免提前锁定后台搜索框交互。
