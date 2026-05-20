## 1. OpenSpec

- [x] 1.1 创建 `add-backend-health-gates` change。
- [x] 1.2 明确健康检查和启动门禁最小范围。
- [x] 1.3 运行 `openspec validate add-backend-health-gates --strict`。

## 2. TDD

- [x] 2.1 先补公共 Web 健康配置测试，覆盖端点暴露、liveness/readiness、核心依赖 readiness。
- [x] 2.2 运行目标测试确认 RED。

## 3. Implementation

- [x] 3.1 更新 `nacos-config/common-web.yml`。
- [x] 3.2 更新 `nacos-config/common-web-prod.yml`。
- [x] 3.3 保持配置轻量，不引入额外部署平台。

## 4. Validation

- [x] 4.1 运行 common-web 目标测试。
- [x] 4.2 运行 OpenSpec strict 校验。
- [ ] 4.3 同步 GitHub Issue #14。
