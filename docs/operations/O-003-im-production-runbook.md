---
layer: Operations
doc_no: "O-003"
audience:
  - Dev
  - Ops
  - QA
feature_area: im-production-runbook
purpose: "记录 MallChat 后端生产上线、健康检查、故障定位、回滚和恢复步骤。"
canonical_path: "docs/operations/O-003-im-production-runbook.md"
status: review
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "GitHub Issue #14"
  - "GitHub Issue #15"
  - "GitHub Issue #16"
  - "openspec/changes/add-backend-health-gates"
  - "openspec/changes/add-im-business-metrics"
  - "openspec/changes/document-im-production-runbook"
outputs:
  - "MallChat 后端生产上线 Runbook"
---

# MallChat 后端生产上线 Runbook

## 1. 范围

本 Runbook 面向 `mallchat-cloud` 后端生产发布。覆盖 gateway、user-service、chat-service、notification-service、file-service、log-service、ai-service，以及 MySQL、Redis、RabbitMQ、Nacos。

## 2. 上线前检查

1. 确认目标分支已通过 PR 验收，且 `openspec validate --all --strict` 通过。
2. 确认生产 Nacos namespace、group、账号和密码已配置。
3. 确认生产 MySQL、Redis、RabbitMQ 地址、账号、密码和网络连通性。
4. 确认 `sql/mallchat.sql` 已在目标数据库执行，且备份策略已生效。
5. 确认生产密钥不沿用本地默认值，尤其是 `common-secret.properties`、Sa-Token JWT secret、数据库密码、Redis 密码、RabbitMQ 密码。

## 3. 配置说明

各服务当前通过 `application.yml` 导入 `common-*.yml` Nacos dataId。生产环境建议使用独立 namespace，并在该 namespace 中维护同名 dataId 的生产值；仓库中的 `common-*-prod.yml` 可作为生产值模板，不默认假设服务会自动加载 `*-prod.yml`。

关键配置组：

1. `common-secret.properties`
2. `common-web.yml`
3. `common-cache.yml`
4. `common-mysql.yml`
5. `common-rabbitmq.yml`
6. `common-sentinel.yml`

健康检查 readiness 按配置层递进覆盖：`common-web.yml` 只声明进程级 `ping`，`common-cache.yml` 增加 Redis，`common-mysql.yml` 增加数据库，`common-rabbitmq.yml` 增加 RabbitMQ。服务应按实际依赖导入对应 dataId，避免 gateway、file-service 等非 MQ/DB 服务被错误地绑定到不存在的健康指标。

Actuator 访问边界：

1. 生产环境 `/actuator/**` 只允许内网、负载均衡健康检查或监控采集侧访问。
2. 不应把 `/actuator/metrics` 暴露到公网或客户端可直接访问的网段。
3. 生产 `common-web-prod.yml` 使用 `health.show-details=when_authorized`；如运维侧需要详细依赖状态，应在网关、Ingress 或服务网格层补齐访问控制后再放开。

## 4. 启动顺序

1. 启动 MySQL、Redis、RabbitMQ、Nacos。
2. 导入或确认 Nacos 配置。
3. 启动基础业务服务：user-service、log-service、file-service。
4. 启动 IM 核心服务：chat-service、notification-service。
5. 启动 gateway。
6. 启动 ai-service。

## 5. 健康检查

公共健康端点由 Actuator 提供，生产 Nacos 配置应暴露 `health`、`info`、`metrics`。

示例：

```bash
curl -fsS http://localhost:8080/actuator/health
curl -fsS http://localhost:8086/actuator/health/liveness
curl -fsS http://localhost:8086/actuator/health/readiness
curl -fsS http://localhost:8083/actuator/health/readiness
```

判定规则：

1. `liveness` 只表示服务进程存活，不应因 Redis、RabbitMQ 或 MySQL 抖动直接失败。
2. `readiness` 包含 `ping`、`db`、`redis`、`rabbit`，任一核心依赖不可用时应返回可诊断状态。
3. gateway 需要先通过自身 `/actuator/health`，再检查后端服务 readiness。

## 6. 关键指标

最小生产观测项：

1. `mallchat.im.business.total`：关键业务行为计数，tag 为 `action`、`result`。
2. `mallchat.im.push.total`：实时推送结果计数，tag 为 `bizType`、`eventType`、`result`。
3. `mallchat.rabbitmq.publish.total`：RabbitMQ 发布结果计数，tag 为 `bizType`、`result`。
4. `mallchat.rabbitmq.confirm.total`：RabbitMQ confirm 结果计数，tag 为 `bizType`、`result`。
5. `mallchat.rabbitmq.return.total`：RabbitMQ return 结果计数，tag 为 `bizType`、`result`。

## 7. 告警处理

建议先采用低成本阈值告警，不引入新的监控平台：

1. readiness 连续 3 次失败：由值班开发确认对应服务依赖，优先检查 Nacos、MySQL、Redis、RabbitMQ 连通性。
2. `mallchat.im.push.total{result="failure"}` 5 分钟内持续增长：检查 notification-service、WebSocket 连接数和 RabbitMQ 消费日志。
3. `mallchat.rabbitmq.publish.total{result!="accepted"}` 或 `mallchat.rabbitmq.confirm.total{result="nack"}` 出现连续增长：检查 RabbitMQ exchange/queue/binding 和 confirm 配置。
4. `mallchat.im.business.total{action="message_send",result="success"}` 明显跌为 0：检查 gateway 路由、chat-service readiness 和用户登录态。
5. 处理链路：值班开发先按 Runbook 排查；30 分钟内无法恢复时通知后端负责人；涉及数据恢复时先冻结写入入口或摘除 gateway 流量。

## 8. 常见故障处理

### gateway 无法转发

1. 检查 gateway `/actuator/health`。
2. 检查 Nacos 服务发现中目标服务是否在线。
3. 检查 Redis 是否可用，限流依赖 Redis。
4. 检查对应服务 readiness。

### 消息发送成功但对方未收到

1. 检查 chat-service readiness。
2. 检查 `mallchat.rabbitmq.publish.total` 是否出现 `failed` 或 `rejected`。
3. 检查 `mallchat.rabbitmq.confirm.total` 是否出现 `nack`。
4. 检查 notification-service readiness。
5. 检查 `mallchat.im.push.total` 中 `offline` 或 `failure` 是否升高。
6. 使用 `/chat/message/list/after/vo` 进行断线补偿验证。

### 好友申请或动态互动没有通知

1. 检查 chat-service 到 notification-service 的 Feign 调用日志。
2. 检查 notification-service readiness。
3. 检查业务事实是否已写入数据库；通知失败不应回滚好友申请或动态互动事实。

## 9. 回滚

1. 停止 gateway 或摘除流量。
2. 回滚后端服务镜像或 Jar 到上一稳定版本。
3. 保留数据库备份，不执行破坏性 SQL。
4. 如涉及 Nacos 配置变更，回滚同名 dataId 到上一版本。
5. 恢复 gateway 流量并重新检查 readiness。

## 10. 数据与缓存恢复

1. MySQL 以备份为准，恢复前先导出当前故障现场数据。
2. Redis 缓存丢失不应造成永久业务事实丢失；登录态需要用户重新登录或服务侧刷新。
3. RabbitMQ 消息堆积时先确认消费者 readiness，再观察 DLX 或失败日志。
4. Nacos 配置误改时优先回滚配置版本，再重启受影响服务。

核心 IM 表恢复 smoke：

```bash
bash scripts/backup-im-core-tables.sh --dry-run
bash scripts/verify-im-core-data-recovery.sh --dry-run
```

生产演练时使用 `MYSQL_HOST`、`MYSQL_PORT`、`MYSQL_USER`、`MYSQL_PASSWORD`、`MYSQL_DATABASE` 指定源库。`verify-im-core-data-recovery.sh` 默认恢复到 `mallchat_recovery_smoke_*` 临时库并在退出时清理，验证消息、会话、房间成员、私聊房间、群资料、好友和动态关联不存在孤儿数据。

Redis 失效恢复边界：

1. 好友缓存和房间成员缓存以 MySQL 为事实来源，缓存 key 缺失时从数据库回源。
2. WebSocket 连接仍在本机存活但 Redis 在线态丢失时，心跳刷新会重建当前用户连接集合和连接元数据。
3. Sa-Token 登录态丢失后旧 token 可以失效，用户通过重新登录恢复，不尝试服务端复活旧登录态。

文件上传边界：

1. `user_avatar`、`chat_image` 只允许常见图片类型，并做最小图片魔数校验。
2. `chat_file` 只允许 PDF、文本、Office 和 zip 等白名单类型。
3. 空文件、超业务大小、危险文件名、无后缀和不支持类型会在上传到对象存储前拒绝。

## 11. 验收命令

```bash
openspec validate --all --strict
mvn -pl mallchat-common/mallchat-common-web -am -Dtest=BackendHealthGateConfigTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl mallchat-service/mallchat-chat-service -am -Dtest=ChatBusinessMetricsRecorderTest,ChatMessageServiceImplTest,UserFriendApplyServiceImplTest,ChatMomentServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl mallchat-common/mallchat-common-websocket -am -Dtest=ChannelManagerTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl mallchat-service/mallchat-chat-service -am -Dtest=ChatRoomMemberServiceImplTest,UserFriendServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl mallchat-service/mallchat-file-service -am -Dtest=FileUploadValidatorTest,FileUploadRecordRecorderTest,FileServiceApplicationTest -Dsurefire.failIfNoSpecifiedTests=false test
bash scripts/verify-im-core-data-recovery.sh --dry-run
docker compose config
bash scripts/validate-repository.sh
git diff --check
```
