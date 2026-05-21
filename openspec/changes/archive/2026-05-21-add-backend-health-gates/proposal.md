## Why

MallChat 后端需要在生产环境中提供可诊断的健康检查和启动门禁，便于部署前确认核心依赖、运行中区分存活与就绪状态。

## What Changes

- 补齐公共 Web/Nacos 健康检查配置，暴露 `health`、`info`、`metrics`。
- 启用 Actuator liveness/readiness probes。
- 按服务实际导入的公共配置递进绑定 readiness：基础 ping、Redis、数据库、RabbitMQ。
- 增加配置测试，避免健康端点被误删或降级为不可诊断状态。

## Non-Goals

- 不新增复杂部署平台。
- 不强绑定 Kubernetes、Prometheus 或商业监控平台。
- 不改造业务接口返回结构。
