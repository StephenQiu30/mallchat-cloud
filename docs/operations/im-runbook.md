---
layer: Operations
doc_no: "OPS-001"
audience:
  - Dev
  - Ops
  - QA
feature_area: im-system
purpose: "定义 MallChat IM 系统的故障排查、数据备份恢复、回滚和观测运维流程。"
canonical_path: "docs/operations/im-runbook.md"
status: draft
version: "0.1.0"
owner: "StephenQiu30"
inputs:
  - "docs/design/D-001-im-production-architecture.md"
  - "scripts/backup-im-core-tables.sh"
  - "scripts/verify-im-core-data-recovery.sh"
  - "nacos-config/common-rabbitmq.yml"
  - "nacos-config/common-cache.yml"
outputs:
  - "运维操作可执行命令和验证路径"
triggers:
  - "IM 消息投递异常时"
  - "核心 IM 数据需要备份或恢复时"
  - "服务版本需要回滚时"
  - "生产环境健康巡检时"
downstream:
  - "docs/acceptance/001-im-e2e-rag-acceptance.md"
---

# IM 运行手册

## 1. 背景

IM 生产化需要可复用运行手册，覆盖消息发送、投递、会话、好友权限的故障排查路径，以及核心数据的备份、恢复、回滚和观测流程。本手册基于 MallChat Cloud 实际架构编写，所有命令和路径均可执行验证。

## 2. 目标

1. 提供消息链路全链路故障排查路径。
2. 提供核心 IM 表备份、恢复和验证的可执行命令。
3. 提供回滚流程、风险识别和常见故障处理方案。
4. 提供健康检查、指标观测和日志排查入口。

```gherkin
Given IM 系统发生消息投递异常、数据需要恢复或服务需要回滚
When 运维人员按本手册执行对应排查或恢复流程
Then 能定位问题根因并完成恢复，且每一步有可执行验证命令
```

## 3. 非目标

- 不沉淀一次性排查日志或临时调试记录。
- 不替代 PR 验证记录或 CI 流程。
- 不覆盖基础设施层（MySQL 主从、Redis 集群、K8s 编排）的运维。

## 4. 核心内容

### 4.1 消息链路故障排查

#### 4.1.1 消息发送失败

消息发送链路：`Controller → Service 校验 → 幂等检查 → DB 写入 → MQ 推送 → 会话更新`。

**排查步骤：**

1. **检查服务健康状态**

   ```bash
   # Chat Service 健康检查（端口 8086）
   curl -s http://localhost:8086/actuator/health/readiness

   # Gateway 健康检查（端口 8080）
   curl -s http://localhost:8080/actuator/health
   ```

2. **检查消息是否已落库**

   ```sql
   -- 按 clientMsgId 查询消息是否存在
   SELECT id, room_id, from_user_id, content, status, create_time
   FROM chat_message
   WHERE from_user_id = #{userId} AND client_msg_id = #{clientMsgId};
   ```

   - 如果消息已存在且 `status = 0`（NORMAL），说明发送成功但推送可能失败。
   - 如果消息不存在，检查 Service 层日志中的校验失败原因。

3. **检查权限校验**

   ```sql
   -- 检查私聊房间映射
   SELECT * FROM chat_private_room
   WHERE (user_low = #{userA} AND user_high = #{userB})
      OR (user_low = #{userB} AND user_high = #{userA});

   -- 检查好友关系
   SELECT * FROM user_friend
   WHERE user_id = #{senderId} AND friend_user_id = #{receiverId};

   -- 检查拉黑状态
   SELECT * FROM user_friend_block
   WHERE (user_id = #{senderId} AND blocked_user_id = #{receiverId})
      OR (user_id = #{receiverId} AND blocked_user_id = #{senderId});

   -- 检查群聊成员身份
   SELECT * FROM chat_room_member
   WHERE room_id = #{roomId} AND user_id = #{userId};
   ```

4. **检查 MQ 推送状态**

   ```bash
   # RabbitMQ 管理界面（默认端口 15672）
   # 检查交换机 mallchat.websocket.exchange 是否存在
   # 检查队列 mallchat.chat.message.push.queue 是否有堆积
   # 注意：生产环境必须使用最小权限账号，禁止使用默认 admin 凭据
   curl -s -u "${RABBITMQ_USER}:${RABBITMQ_PASS}" http://localhost:15672/api/queues | jq '.[] | {name, messages, consumers}'
   ```

5. **检查 Micrometer 指标**

   ```bash
   # 查看消息发送指标
   curl -s http://localhost:8086/actuator/metrics/mallchat.im.business.total \
     | jq '.availableTags[] | select(.tag == "action")'
   ```

   指标标签：`action=message_send`, `result=success|duplicate|error`。

#### 4.1.2 消息投递不到达

消息投递链路：`MQ Consumer → WebSocket ChannelManager → 用户 Channel`。

**排查步骤：**

1. **检查用户 WebSocket 连接状态**

   ```bash
   # Redis 中查询用户连接
   redis-cli SMEMBERS "ws:user:connections:{userId}"
   ```

   - 如果集合为空，用户未连接，消息会依赖离线历史拉取。
   - 如果集合非空，检查连接元数据。

2. **检查连接元数据**

   ```bash
   redis-cli HGETALL "ws:connection:meta:{connectionId}"
   ```

3. **检查 MQ 消费者状态**

   ```bash
   # 检查消费者数量和未确认消息
   curl -s -u "${RABBITMQ_USER}:${RABBITMQ_PASS}" http://localhost:15672/api/queues \
     | jq '.[] | select(.name | contains("chat.message")) | {name, messages, consumers, message_stats}'
   ```

4. **检查慢消费告警**

   查看日志中是否有 `slow-consume-threshold-ms` 超过 5000ms 的 WARN 日志。

#### 4.1.3 会话未读数异常

**排查步骤：**

1. **检查会话状态**

   ```sql
   SELECT user_id, room_id, last_message_id, unread_count, active_time
   FROM chat_session
   WHERE user_id = #{userId} AND room_id = #{roomId};
   ```

2. **校验 last_message_id 是否被旧消息覆盖**

   ```sql
   -- 检查 last_message_id 指向的消息时间
   SELECT cm.id, cm.create_time
   FROM chat_session cs
   JOIN chat_message cm ON cm.id = cs.last_message_id
   WHERE cs.user_id = #{userId} AND cs.room_id = #{roomId};

   -- 对比该房间最新消息
   SELECT id, create_time
   FROM chat_message
   WHERE room_id = #{roomId} AND status = 0
   ORDER BY id DESC LIMIT 1;
   ```

3. **校验 last_read_message_id**

   ```sql
   SELECT user_id, last_read_message_id
   FROM chat_room_member
   WHERE room_id = #{roomId} AND user_id = #{userId};
   ```

#### 4.1.4 好友权限问题

**排查步骤：**

1. **检查双向好友关系**

   ```sql
   -- 必须双向存在才是互为好友
   SELECT * FROM user_friend
   WHERE (user_id = #{userA} AND friend_user_id = #{userB})
      OR (user_id = #{userB} AND friend_user_id = #{userA});
   ```

2. **检查好友申请状态**

   ```sql
   SELECT user_id, target_id, msg, status, create_time
   FROM user_friend_apply
   WHERE (user_id = #{userA} AND target_id = #{userB})
      OR (user_id = #{userB} AND target_id = #{userA})
   ORDER BY create_time DESC;
   ```

   状态：1=待处理，2=已通过，3=已忽略。

### 4.2 数据备份与恢复

#### 4.2.1 核心 IM 表备份

使用 `scripts/backup-im-core-tables.sh` 备份 13 张核心 IM 表。

```bash
# 执行备份（输出备份文件路径）
bash scripts/backup-im-core-tables.sh

# 预览备份命令（不实际执行）
bash scripts/backup-im-core-tables.sh --dry-run

# 列出备份表
bash scripts/backup-im-core-tables.sh --print-tables
```

**环境变量配置：**

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `MYSQL_HOST` | `127.0.0.1` | MySQL 主机 |
| `MYSQL_PORT` | `3306` | MySQL 端口 |
| `MYSQL_USER` | `root` | MySQL 用户 |
| `MYSQL_PASSWORD` | `root` | MySQL 密码 |
| `MYSQL_DATABASE` | `mallchat` | 数据库名 |
| `BACKUP_OUTPUT` | `backups/im-core-{timestamp}.sql` | 备份输出路径 |

**备份表清单（13 张）：**

`user`, `user_friend`, `user_friend_apply`, `chat_room`, `chat_room_member`, `chat_private_room`, `chat_group_info`, `chat_message`, `chat_session`, `chat_moment`, `chat_moment_media`, `chat_moment_like`, `chat_moment_comment`

#### 4.2.2 数据恢复验证

使用 `scripts/verify-im-core-data-recovery.sh` 将备份恢复到临时数据库并运行 20 条孤儿检测断言。

```bash
# 执行恢复验证（自动创建临时数据库，验证后清理）
BACKUP_FILE=backups/im-core-20260606120000.sql bash scripts/verify-im-core-data-recovery.sh

# 预览验证过程
bash scripts/verify-im-core-data-recovery.sh --dry-run

# 保留临时数据库用于调试
KEEP_RECOVERY_DB=true BACKUP_FILE=backups/im-core-20260606120000.sql bash scripts/verify-im-core-data-recovery.sh
```

**环境变量配置：**

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `BACKUP_FILE` | （自动调用备份脚本） | 要验证的备份文件 |
| `RECOVERY_DATABASE` | `mallchat_recovery_smoke_$$` | 临时数据库名 |
| `KEEP_RECOVERY_DB` | `false` | 是否保留临时数据库 |

**验证断言覆盖（20 条）：**

- `user_friend` → `user` 双向外键完整
- `user_friend_apply` → `user` 双向外键完整
- `chat_room` → `user` 创建者完整
- `chat_room_member` → `user` 和 `chat_room` 完整
- `chat_message` → `chat_room` 和 `user` 完整
- `chat_session` → `chat_message`、`chat_room` 和 `user` 完整
- `chat_private_room` → `chat_room` 完整
- `chat_group_info` → `chat_room` 完整
- `chat_moment` → `user` 完整
- `chat_moment_media/like/comment` → `chat_moment` 和 `user` 完整

#### 4.2.3 备份恢复操作

```bash
# 1. 停止写入（可选：将 Gateway 限流设为 0 或摘除 Chat Service 节点）

# 2. 执行备份
bash scripts/backup-im-core-tables.sh

# 3. 恢复到目标数据库
mysql -h ${MYSQL_HOST} -P ${MYSQL_PORT} -u ${MYSQL_USER} -p${MYSQL_PASSWORD} \
  ${MYSQL_DATABASE} < backups/im-core-{timestamp}.sql

# 4. 验证恢复结果
BACKUP_FILE=backups/im-core-{timestamp}.sql bash scripts/verify-im-core-data-recovery.sh

# 5. 清除 Redis 缓存（可选：强制回源）
# 注意：使用 SCAN 替代 KEYS 避免阻塞 Redis；--scan 自动分批迭代
redis-cli --scan --pattern "mallchat:*" | xargs -r redis-cli DEL
```

### 4.3 回滚流程

#### 4.3.1 服务版本回滚

```bash
# 1. 确认当前版本
git log --oneline -5

# 2. 创建回滚前 tag
git tag pre-rollback-$(date +%Y%m%d%H%M%S)

# 3. 回退到目标版本
git revert <commit-sha>
# 或
git reset --hard <target-tag>

# 4. 重新构建和部署
mvn clean package -DskipTests
# 按部署流程重启服务
```

#### 4.3.2 数据回滚

```bash
# 1. 确认回滚目标时间点的备份文件
ls -la backups/

# 2. 使用恢复验证脚本确认备份完整性
BACKUP_FILE=backups/im-core-{target}.sql bash scripts/verify-im-core-data-recovery.sh

# 3. 执行数据恢复（见 4.2.3）
```

#### 4.3.3 回滚风险边界

| 风险 | 影响 | 缓解措施 |
| --- | --- | --- |
| 回滚期间新消息丢失 | 用户发送的消息未被保存 | 回滚前执行全量备份；回滚窗口尽量短 |
| 缓存与数据库不一致 | 权限、在线状态异常 | 回滚后清除 Redis 缓存强制回源 |
| MQ 消息堆积 | 推送延迟 | 回滚前检查 MQ 队列状态；必要时暂停消费者 |
| 客户端缓存旧数据 | UI 显示不一致 | 通知客户端团队做版本兼容检查 |

### 4.4 观测与健康检查

#### 4.4.1 服务健康端点

```bash
# Gateway（端口 8080）
curl -s http://localhost:8080/actuator/health

# Chat Service（端口 8086）- readiness 包含 ping, db, redis, rabbit
curl -s http://localhost:8086/actuator/health/readiness

# User Service（端口 8081）
curl -s http://localhost:8081/actuator/health

# Notification Service（端口 8083）
curl -s http://localhost:8083/actuator/health
```

#### 4.4.2 关键指标

```bash
# 消息发送业务指标
curl -s http://localhost:8086/actuator/metrics/mallchat.im.business.total

# MQ 发布观测
curl -s http://localhost:8086/actuator/metrics/mallchat.mq.publish

# HTTP 请求指标
curl -s http://localhost:8080/actuator/metrics/http.server.requests
```

#### 4.4.3 日志排查

```bash
# 关键日志关键字
# 消息发送异常
grep -r "sendMessage" logs/ | grep -i "error\|exception"

# MQ 推送失败
grep -r "sendChatMessageGroupPush" logs/ | grep -i "error\|fail"

# 会话更新异常
grep -r "updateSessionBatch" logs/ | grep -i "error\|exception"

# 慢消费告警（阈值 5000ms）
grep -r "slow-consume" logs/

# WebSocket 连接异常
grep -r "ChannelManager" logs/ | grep -i "reject\|abnormal"
```

#### 4.4.4 基础设施状态

```bash
# MySQL 连接检查
mysql -h ${MYSQL_HOST} -P ${MYSQL_PORT} -u ${MYSQL_USER} -p -e "SHOW STATUS LIKE 'Threads_connected';"

# Redis 连接检查
redis-cli PING
redis-cli INFO clients

# RabbitMQ 队列状态
curl -s -u "${RABBITMQ_USER}:${RABBITMQ_PASS}" http://localhost:15672/api/queues \
  | jq '.[] | {name, messages, consumers, state}'

# RabbitMQ 连接数
curl -s -u "${RABBITMQ_USER}:${RABBITMQ_PASS}" http://localhost:15672/api/connections \
  | jq 'length'
```

### 4.5 常见故障处理

#### 4.5.1 消息重复发送

**现象：** 同一消息在聊天窗口出现多次。

**排查：**

```sql
-- 检查是否有重复的 clientMsgId
SELECT from_user_id, client_msg_id, COUNT(*) as cnt
FROM chat_message
GROUP BY from_user_id, client_msg_id
HAVING cnt > 1;
```

**处理：** 幂等保护基于 `uk_from_user_client_msg` 唯一键。如果出现重复，检查应用层是否有重试逻辑绕过了幂等检查。

#### 4.5.2 MQ 消息堆积

**现象：** 消息发送成功但接收方延迟收到。

**排查：**

```bash
curl -s -u "${RABBITMQ_USER}:${RABBITMQ_PASS}" http://localhost:15672/api/queues \
  | jq '.[] | select(.messages > 100) | {name, messages, consumers}'
```

**处理：**

1. 检查消费者是否存活（`consumers > 0`）。
2. 检查是否有慢消费（查看慢消费告警日志）。
3. 必要时增加消费者并发（调整 `max-concurrency`，当前默认 1-5）。
4. 检查死信队列是否有被拒绝的消息。

#### 4.5.3 WebSocket 连接失败

**现象：** 客户端无法建立 WebSocket 连接。

**排查：**

```bash
# 检查 Netty WebSocket 端口是否监听
netstat -tlnp | grep ${WS_PORT}

# 检查 Redis 中连接状态
redis-cli SCARD "ws:user:connections:{userId}"

# 检查连接数限制（每用户最多 5 个）
redis-cli SMEMBERS "ws:user:connections:{userId}"
```

**处理：**

1. 检查 Sa-Token 认证是否正常。
2. 检查连接数是否达到上限（默认 5）。
3. 检查最小连接间隔是否被限流。

#### 4.5.4 缓存穿透导致数据库压力

**现象：** MySQL CPU 飙升，大量回源查询。

**排查：**

```bash
# Redis 命中率
redis-cli INFO stats | grep keyspace

# Caffeine 本地缓存配置（TTL 300s, 最大 10000 条）
grep -r "caffeine" nacos-config/
```

**处理：**

1. 检查 Redis 是否正常运行。
2. 检查缓存 key 是否存在热点。
3. 必要时重启服务重建本地缓存。

## 5. 关联文档

### 5.1 输入文档

1. `docs/design/D-001-im-production-architecture.md` — 架构设计和失败补偿边界。
2. `docs/prd/001-im-system-srd.md` — 需求规格和 RAG 定义。

### 5.2 输出文档

1. `docs/acceptance/001-im-e2e-rag-acceptance.md` — E2E 验收门禁。

### 5.3 下游文档

1. `scripts/backup-im-core-tables.sh` — 备份脚本。
2. `scripts/verify-im-core-data-recovery.sh` — 恢复验证脚本。

## 6. 验收门禁

- `scripts/backup-im-core-tables.sh` 可执行，覆盖 13 张核心 IM 表。
- `scripts/verify-im-core-data-recovery.sh` 可执行，20 条孤儿检测断言全部通过。
- 健康检查端点与 Nacos 配置一致（readiness 包含 `ping,db,redis,rabbit`）。
- 消息链路排查路径覆盖发送、投递、会话、好友权限四个维度。
- 回滚流程包含风险边界说明和缓解措施。

## 7. 风险与边界

1. 本手册假设 MySQL 单实例部署；主从复制、读写分离的运维不在范围内。
2. Redis 集群故障的排查依赖具体部署模式，本手册只覆盖单实例场景。
3. RabbitMQ 集群模式下的队列同步和脑裂处理不在范围内。
4. 客户端兼容性问题需要与前端团队协作排查。
5. 备份脚本使用 `mysqldump --single-transaction`，对大表可能有性能影响。

## 8. 待确认问题

- 生产环境是否配置了 ELK 集群用于日志聚合？
- RabbitMQ 死信队列的监控和告警是否已接入？
- WebSocket 端口在生产环境的具体配置？

## 9. 变更记录

| 日期 | 作者 | 版本 | 变更说明 |
| --- | --- | --- | --- |
| 2026-06-06 | StephenQiu30 | 0.1.0 | 初始化运行手册 |
