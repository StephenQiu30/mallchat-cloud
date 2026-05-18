---
layer: design
doc_no: D-001
audience: [后端开发, 架构师, 全栈开发]
purpose: 研究开源 IM 项目的设计模式，提取可参考的架构设计、数据库设计、API 设计和消息协议
owner: StephenQiu
inputs: [OpenIM, Tinode, RuoYi-Vue, JeecgBoot 开源项目]
outputs: [技术方案参考文档]
triggers: [MallChat 项目架构设计阶段]
downstream: [数据库设计, API 设计, WebSocket 协议设计]
---

# 开源 IM 项目技术方案参考文档

> 本文档研究 OpenIM、Tinode、RuoYi-Vue、JeecgBoot 四个开源项目，提取可参考的架构设计、数据库设计、API 设计和消息协议，为 MallChat 项目提供技术方案参考。

---

## 一、项目概览

### 1.1 OpenIM — 开源 IM 系统

- **GitHub**: `openimsdk/open-im-server`
- **语言**: Go
- **架构**: 微服务架构
- **存储**: MongoDB + MySQL + Redis + Kafka + MinIO
- **协议**: WebSocket (Protobuf) + HTTP REST (gRPC 内部)

### 1.2 Tinode — 实时通讯服务器

- **GitHub**: `tinode/chat`
- **语言**: Go (服务端)
- **架构**: 插件化/适配器模式
- **存储**: MySQL / PostgreSQL / MongoDB / RethinkDB / SQLite
- **协议**: JSON over WebSocket (类 JSON-RPC)

### 1.3 RuoYi-Vue — 权限管理系统

- **GitHub**: `yangzongzhuan/RuoYi-Vue`
- **语言**: Java (Spring Boot)
- **架构**: 单体/微服务可选
- **存储**: MySQL + Redis
- **特点**: 完善的 RBAC 权限模型

### 1.4 JeecgBoot — 低代码平台

- **GitHub**: `jeecgboot/JeecgBoot`
- **语言**: Java (Spring Boot + Spring Cloud)
- **架构**: 微服务架构
- **存储**: MySQL + Redis
- **特点**: 代码生成 + 在线表单 + 工作流

---

## 二、数据库表设计参考

### 2.1 用户表设计

#### OpenIM 用户表 (MySQL)

```sql
-- OpenIM 用户表结构
CREATE TABLE `users` (
    `user_id`        VARCHAR(64)  PRIMARY KEY COMMENT '用户ID',
    `nickname`       VARCHAR(256) DEFAULT NULL COMMENT '昵称',
    `face_url`       VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
    `ex`             VARCHAR(1024) DEFAULT NULL COMMENT '扩展字段',
    `create_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `status`         INT          DEFAULT 0 COMMENT '状态：0-正常，1-禁用',
    `app_manger_level` INT        DEFAULT 0 COMMENT '管理等级'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

#### Tinode 用户表

```sql
-- Tinode 用户表结构
CREATE TABLE `users` (
    `id`         BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    `createdat`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updatedat`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `state`      INT          DEFAULT 0 COMMENT '状态：0-活跃，1-暂停，2-软删除',
    `stateat`    DATETIME     DEFAULT NULL COMMENT '状态变更时间',
    `access`     JSON         DEFAULT NULL COMMENT '默认权限配置',
    `public`     JSON         DEFAULT NULL COMMENT '公开信息（vCard）',
    `private`    JSON         DEFAULT NULL COMMENT '私有信息',
    `credentials` JSON        DEFAULT NULL COMMENT '认证凭据（邮箱、手机等）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

#### RuoYi-Vue 用户表 (参考)

```sql
-- RuoYi 用户表结构（精简版）
CREATE TABLE `sys_user` (
    `user_id`      BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    `dept_id`      BIGINT       DEFAULT NULL COMMENT '部门ID',
    `user_name`    VARCHAR(30)  NOT NULL COMMENT '登录用户名',
    `nick_name`    VARCHAR(30)  NOT NULL COMMENT '显示昵称',
    `email`        VARCHAR(50)  DEFAULT NULL COMMENT '邮箱',
    `phonenumber`  VARCHAR(11)  DEFAULT NULL COMMENT '手机号',
    `sex`          CHAR(1)      DEFAULT '2' COMMENT '性别：0-男，1-女，2-未知',
    `avatar`       VARCHAR(100) DEFAULT NULL COMMENT '头像路径',
    `password`     VARCHAR(100) NOT NULL COMMENT '加密密码',
    `status`       CHAR(1)      DEFAULT '0' COMMENT '状态：0-正常，1-禁用',
    `del_flag`     CHAR(1)      DEFAULT '0' COMMENT '删除标志：0-正常，2-删除',
    `login_ip`     VARCHAR(128) DEFAULT NULL COMMENT '最后登录IP',
    `login_date`   DATETIME     DEFAULT NULL COMMENT '最后登录时间',
    `create_by`    VARCHAR(64)  DEFAULT NULL COMMENT '创建者',
    `update_by`    VARCHAR(64)  DEFAULT NULL COMMENT '更新者',
    `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`       VARCHAR(500) DEFAULT NULL COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

#### MallChat 当前用户表

```sql
-- MallChat 已有的用户表设计
CREATE TABLE `user` (
    `id`              BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    `user_name`       VARCHAR(256) DEFAULT NULL COMMENT '用户昵称',
    `user_avatar`     VARCHAR(1024) DEFAULT NULL COMMENT '用户头像',
    `user_profile`    VARCHAR(512) DEFAULT NULL COMMENT '用户简介',
    `user_role`       VARCHAR(256) NOT NULL DEFAULT 'user' COMMENT '用户角色：user/admin/ban',
    `user_phone`      VARCHAR(128) DEFAULT NULL COMMENT '用户手机号',
    `user_email`      VARCHAR(256) DEFAULT NULL COMMENT '用户邮箱',
    `ma_open_id`      VARCHAR(256) DEFAULT NULL COMMENT '微信小程序 OpenID',
    `wx_union_id`     VARCHAR(256) DEFAULT NULL COMMENT '微信 UnionID',
    `wx_open_id`      VARCHAR(256) DEFAULT NULL COMMENT '微信开放平台 OpenID',
    `apple_id`        VARCHAR(256) DEFAULT NULL COMMENT 'Apple ID',
    `last_login_time` DATETIME     DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip`   VARCHAR(128) DEFAULT NULL COMMENT '最后登录IP',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`       TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
```

**设计建议**:
- MallChat 当前设计已覆盖微信生态登录（小程序、开放平台、UnionID），符合项目定位
- 可参考 RuoYi 的 `sys_user` 增加 `dept_id` 字段支持组织架构（如需要）
- 可参考 Tinode 的 JSON 字段设计，用 `public`/`private` 存储灵活的用户资料

---

### 2.2 好友关系表设计

#### OpenIM 好友关系表

```sql
CREATE TABLE `friends` (
    `user_id`      VARCHAR(64) NOT NULL COMMENT '用户ID',
    `friend_uid`   VARCHAR(64) NOT NULL COMMENT '好友用户ID',
    `remark`       VARCHAR(256) DEFAULT NULL COMMENT '好友备注',
    `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `ex`           VARCHAR(1024) DEFAULT NULL COMMENT '扩展字段',
    PRIMARY KEY (`user_id`, `friend_uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友关系表';

CREATE TABLE `friend_requests` (
    `user_id`      VARCHAR(64)  NOT NULL COMMENT '发起者ID',
    `friend_uid`   VARCHAR(64)  NOT NULL COMMENT '目标用户ID',
    `req_msg`      VARCHAR(256) DEFAULT NULL COMMENT '申请消息',
    `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `handler_msg`  VARCHAR(256) DEFAULT NULL COMMENT '处理消息',
    `handle_result` INT         DEFAULT 0 COMMENT '处理结果：0-未处理，1-同意，2-拒绝',
    `handle_time`  DATETIME     DEFAULT NULL COMMENT '处理时间',
    PRIMARY KEY (`user_id`, `friend_uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友申请表';
```

#### MallChat 当前好友表

```sql
CREATE TABLE `user_friend` (
    `id`             BIGINT   PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    `user_id`        BIGINT   NOT NULL COMMENT '用户ID',
    `friend_user_id` BIGINT   NOT NULL COMMENT '好友用户ID',
    `create_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`      TINYINT  NOT NULL DEFAULT 0 COMMENT '是否删除',
    UNIQUE KEY `uk_user_friend` (`user_id`, `friend_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户好友表';

CREATE TABLE `user_friend_apply` (
    `id`          BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '申请ID',
    `user_id`     BIGINT       NOT NULL COMMENT '发起用户ID',
    `target_id`   BIGINT       NOT NULL COMMENT '目标用户ID',
    `msg`         VARCHAR(256) NOT NULL COMMENT '申请消息',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1-待处理，2-已同意，3-已忽略',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`   TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友申请表';
```

**设计建议**:
- 可参考 OpenIM 增加 `remark`（好友备注）字段到 `user_friend` 表
- 可参考 OpenIM 增加 `handler_msg`（处理消息）字段到 `user_friend_apply` 表

---

### 2.3 群组表设计

#### OpenIM 群组表

```sql
CREATE TABLE `groups` (
    `group_id`       VARCHAR(64)  PRIMARY KEY COMMENT '群组ID',
    `group_name`     VARCHAR(256) NOT NULL COMMENT '群名称',
    `notification`   VARCHAR(256) DEFAULT NULL COMMENT '群公告',
    `introduction`   VARCHAR(256) DEFAULT NULL COMMENT '群简介',
    `face_url`       VARCHAR(512) DEFAULT NULL COMMENT '群头像',
    `creator_uid`    VARCHAR(64)  NOT NULL COMMENT '创建者ID',
    `group_type`     INT          DEFAULT 0 COMMENT '群类型：0-普通群，1-超大群',
    `status`         INT          DEFAULT 0 COMMENT '状态',
    `create_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `member_count`   INT          DEFAULT 0 COMMENT '成员数量',
    `ex`             VARCHAR(1024) DEFAULT NULL COMMENT '扩展字段'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群组表';

CREATE TABLE `group_members` (
    `group_id`       VARCHAR(64) NOT NULL COMMENT '群组ID',
    `user_id`        VARCHAR(64) NOT NULL COMMENT '用户ID',
    `role_level`     INT         DEFAULT 1 COMMENT '角色：1-普通成员，2-管理员，3-群主',
    `join_time`      DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `nickname`       VARCHAR(256) DEFAULT NULL COMMENT '群内昵称',
    `ex`             VARCHAR(1024) DEFAULT NULL COMMENT '扩展字段',
    PRIMARY KEY (`group_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群组成员表';
```

#### MallChat 当前群组表

```sql
CREATE TABLE `chat_group_info` (
    `id`           BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    `room_id`      BIGINT       NOT NULL COMMENT '房间ID',
    `group_name`   VARCHAR(128) NOT NULL COMMENT '群聊名称',
    `group_avatar` VARCHAR(512) DEFAULT NULL COMMENT '群聊头像',
    `announcement` TEXT         DEFAULT NULL COMMENT '群公告',
    `create_user`  BIGINT       NOT NULL COMMENT '创建者用户ID',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除',
    UNIQUE KEY `uk_room_id` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群组详情表';
```

**设计建议**:
- MallChat 的 `room_id` 抽象设计较好，将群聊和私聊统一到 `chat_room` 表
- 可参考 OpenIM 增加 `introduction`（群简介）、`member_count`（成员数量）字段
- 可参考 OpenIM 的 `group_type` 区分普通群和超大群

---

### 2.4 消息表设计

#### OpenIM 消息表 (MongoDB)

```javascript
// OpenIM 使用 MongoDB 存储消息
// Collection: chat_logs
{
    "send_id": "user_001",           // 发送者ID
    "recv_id": "user_002",           // 接收者ID（单聊）或群组ID（群聊）
    "content": "...",                // 消息内容
    "msg_type": 1,                   // 消息类型：1-文本，2-图片，3-语音，4-视频，5-文件
    "session_type": 1,               // 会话类型：1-单聊，2-群聊
    "send_time": 1699000000000,      // 发送时间戳
    "create_time": "2023-11-03...",  // 创建时间
    "seq": 1001,                     // 消息序号（用于多端同步）
    "status": 1,                     // 状态：0-发送中，1-已发送，2-已送达，3-已读
    "client_msg_id": "uuid_xxx",     // 客户端消息ID（幂等去重）
    "server_msg_id": "server_xxx",   // 服务端消息ID
    "content_type": 101,             // 内容类型（细分子类型）
    "ex": {}                         // 扩展字段
}
```

#### OpenIM 会话表 (MongoDB)

```javascript
// Collection: conversations
{
    "conversation_id": "conv_001",
    "conversation_type": 1,          // 1-单聊，2-群聊
    "user_id": "user_001",           // 所属用户
    "group_id": "",                  // 群组ID（群聊时有值）
    "recv_msg_opt": 0,               // 消息接收选项：0-正常，1-免打扰
    "unread_count": 5,               // 未读消息数
    "draft_text": "",                // 草稿内容
    "is_pinned": false,              // 是否置顶
    "is_private_chat": false,        // 是否私密聊天
    "max_seq": 1005,                 // 最大消息序号
    "min_seq": 1,                    // 最小消息序号
    "latest_msg": {},                // 最新消息摘要
    "update_time": "2023-11-03..."   // 更新时间
}
```

#### MallChat 当前消息表

```sql
CREATE TABLE `chat_message` (
    `id`           BIGINT   PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    `room_id`      BIGINT   NOT NULL COMMENT '房间ID',
    `from_user_id` BIGINT   NOT NULL COMMENT '发送者ID',
    `client_msg_id` VARCHAR(64) NOT NULL COMMENT '客户端消息ID',
    `content`      TEXT     NOT NULL COMMENT '消息内容',
    `extra`        JSON     DEFAULT NULL COMMENT '消息扩展内容（如图片/文件详细信息）',
    `type`         TINYINT  NOT NULL DEFAULT 1 COMMENT '消息类型：1-文本，2-图片，3-文件',
    `reply_msg_id` BIGINT   DEFAULT NULL COMMENT '回复的消息ID',
    `status`       TINYINT  NOT NULL DEFAULT 0 COMMENT '消息状态：0-正常，1-已撤回，2-已删除',
    `create_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    `update_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`    TINYINT  NOT NULL DEFAULT 0 COMMENT '是否删除',
    UNIQUE KEY `uk_from_user_client_msg` (`from_user_id`, `client_msg_id`),
    KEY `idx_room_id_id` (`room_id`, `id` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表';
```

**设计建议**:
- MallChat 使用 MySQL 存储消息，对于中小型项目足够
- `client_msg_id` 的唯一索引设计良好，实现了消息幂等去重
- 可参考 OpenIM 增加 `seq`（消息序号）字段，用于多端同步和消息排序
- 可参考 OpenIM 增加 `session_type` 字段区分单聊/群聊消息
- `extra` JSON 字段的设计很灵活，可存储图片尺寸、文件大小等扩展信息

---

### 2.5 会话表设计

#### MallChat 当前会话表

```sql
CREATE TABLE `chat_session` (
    `id`                   BIGINT   PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    `user_id`              BIGINT   NOT NULL COMMENT '所属用户ID',
    `room_id`              BIGINT   NOT NULL COMMENT '房间ID',
    `last_message_id`      BIGINT   DEFAULT NULL COMMENT '最后一条消息ID',
    `last_read_message_id` BIGINT   DEFAULT NULL COMMENT '最后一条已读消息ID',
    `unread_count`         INT      NOT NULL DEFAULT 0 COMMENT '未读数',
    `top_status`           TINYINT  NOT NULL DEFAULT 0 COMMENT '置顶状态：0-否，1-是',
    `active_time`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后活跃时间',
    `create_time`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`            TINYINT  NOT NULL DEFAULT 0 COMMENT '是否删除',
    UNIQUE KEY `uk_user_room` (`user_id`, `room_id`),
    KEY `idx_user_id_active` (`user_id`, `active_time` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话列表';
```

**设计建议**:
- MallChat 的会话表设计已经比较完善
- 可参考 OpenIM 增加 `draft_text`（草稿）字段
- 可参考 OpenIM 增加 `recv_msg_opt`（消息接收选项，如免打扰）字段

---

### 2.6 私聊房间映射表

#### MallChat 当前设计

```sql
CREATE TABLE `chat_private_room` (
    `id`          BIGINT   PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    `user_low`    BIGINT   NOT NULL COMMENT '用户ID较小值',
    `user_high`   BIGINT   NOT NULL COMMENT '用户ID较大值',
    `room_id`     BIGINT   NOT NULL COMMENT '私聊房间ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`   TINYINT  NOT NULL DEFAULT 0 COMMENT '是否删除',
    UNIQUE KEY `uk_user_pair` (`user_low`, `user_high`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='私聊房间映射表';
```

**设计亮点**:
- `user_low` + `user_high` 的设计确保任意两个用户只有一个私聊房间
- 通过排序用户 ID 避免重复创建，这是一个很好的设计模式

---

## 三、API 接口设计规范参考

### 3.1 OpenIM API 设计规范

OpenIM 采用 RESTful + gRPC 混合架构：

```
外部 API (RESTful HTTP):
  POST   /api/v1/auth/login              # 用户登录
  POST   /api/v1/auth/register           # 用户注册
  POST   /api/v1/user/get_users_info     # 获取用户信息
  PUT    /api/v1/user/update_user_info   # 更新用户信息
  POST   /api/v1/friend/add_friend       # 添加好友
  GET    /api/v1/friend/get_friend_list  # 获取好友列表
  POST   /api/v1/group/create_group      # 创建群组
  POST   /api/v1/group/join_group        # 加入群组
  POST   /api/v1/message/send_msg        # 发送消息
  GET    /api/v1/message/get_history      # 获取历史消息
  POST   /api/v1/conversation/get_conversations  # 获取会话列表

内部服务间通信 (gRPC):
  - 使用 Protocol Buffers 定义接口
  - 服务间通过服务发现（如 etcd）进行调用
```

**设计特点**:
- API 版本化：统一使用 `/api/v1/` 前缀
- 资源导向：按业务领域划分（user、friend、group、message、conversation）
- 统一响应格式：

```json
{
    "errCode": 0,
    "errMsg": "",
    "data": { ... }
}
```

### 3.2 Tinode API 设计规范

Tinode 使用 WebSocket JSON-RPC 风格协议：

```json
// 请求格式
{
    "id": "msg_001",           // 消息ID，用于匹配响应
    "cmd": "pub",              // 命令类型
    "params": {                // 参数
        "topic": "user_002",
        "content": "Hello!"
    }
}

// 响应格式
{
    "id": "msg_001",           // 对应请求的ID
    "ctrl": {
        "code": 200,           // 状态码
        "text": "ok",          // 状态描述
        "params": { ... }      // 返回参数
    }
}
```

**Tinode 命令类型**:

| 命令 | 用途 | 说明 |
|------|------|------|
| `hi` | 握手 | 建立连接时发送 |
| `acc` | 账户 | 创建/更新账户 |
| `login` | 登录 | 身份认证 |
| `sub` | 订阅 | 订阅话题（加入会话） |
| `pub` | 发布 | 发送消息 |
| `get` | 获取 | 获取数据 |
| `set` | 设置 | 更新数据 |
| `del` | 删除 | 删除资源 |
| `note` | 通知 | 已读回执、正在输入等 |
| `bye` | 断开 | 结束会话 |

### 3.3 RuoYi-Vue API 设计规范

RuoYi 采用标准 RESTful 设计：

```
GET    /system/user/list              # 用户列表
GET    /system/user/{userId}          # 用户详情
POST   /system/user                   # 新增用户
PUT    /system/user                   # 修改用户
DELETE /system/user/{userIds}         # 删除用户
GET    /system/role/list              # 角色列表
POST   /system/role                   # 新增角色
GET    /system/menu/list              # 菜单列表
POST   /system/menu                   # 新增菜单
```

**设计特点**:
- 使用 `@PreAuthorize("@ss.hasPermi('system:user:list')")` 进行权限控制
- 前端使用 `v-hasPermi` 指令控制按钮显示
- 统一响应格式：

```json
{
    "code": 200,
    "msg": "操作成功",
    "data": { ... }
}
```

### 3.4 MallChat API 设计建议

基于以上参考，建议 MallChat 的 API 设计：

```
# 用户相关
POST   /api/v1/auth/login              # 登录
GET    /api/v1/user/info                # 获取当前用户信息
PUT    /api/v1/user/info                # 更新用户信息

# 好友相关
GET    /api/v1/friend/list              # 好友列表
POST   /api/v1/friend/apply             # 发送好友申请
PUT    /api/v1/friend/apply/{id}        # 处理好友申请
DELETE /api/v1/friend/{friendId}        # 删除好友

# 房间相关
GET    /api/v1/room/list                # 房间列表
POST   /api/v1/room/group               # 创建群聊
PUT    /api/v1/room/group/{roomId}      # 更新群信息
POST   /api/v1/room/group/{roomId}/member  # 添加群成员

# 消息相关
GET    /api/v1/message/list             # 获取消息历史
POST   /api/v1/message/send             # 发送消息（REST 备用）
DELETE /api/v1/message/{msgId}          # 删除消息

# 会话相关
GET    /api/v1/session/list             # 会话列表
PUT    /api/v1/session/{sessionId}      # 更新会话设置
```

---

## 四、WebSocket 消息协议设计

### 4.1 OpenIM WebSocket 协议

OpenIM 使用 **Protobuf** 序列化 WebSocket 消息：

```protobuf
// 消息结构定义
message MsgData {
    string send_id = 1;           // 发送者ID
    string recv_id = 2;           // 接收者ID
    int32 content_type = 3;       // 内容类型
    string content = 4;           // 消息内容
    int64 send_time = 5;          // 发送时间
    string client_msg_id = 6;     // 客户端消息ID
    string server_msg_id = 7;     // 服务端消息ID
    int32 status = 8;             // 状态
    int32 session_type = 9;       // 会话类型
    int64 seq = 10;               // 消息序号
    message FileInfo {
        string file_url = 1;
        int64 file_size = 2;
        string file_name = 3;
    }
    FileInfo file_info = 11;      // 文件信息
}

// WebSocket 帧结构
message WebSocketFrame {
    int32 req_identifier = 1;     // 请求标识
    string token = 2;             // 认证Token
    int32 send_id = 3;            // 发送者ID
    int32 service_id = 4;         // 服务ID
    bytes data = 5;               // 数据（Protobuf序列化）
}
```

### 4.2 Tinode WebSocket 协议

Tinode 使用 **JSON** 序列化：

```json
// 客户端 -> 服务端：发送消息
{
    "id": "123",
    "pub": {
        "id": "msg_456",
        "topic": "usr_abc",
        "content": "Hello World!",
        "head": {
            "mime": "text/plain"
        }
    }
}

// 服务端 -> 客户端：消息确认
{
    "id": "123",
    "ctrl": {
        "id": "msg_456",
        "code": 200,
        "text": "ok",
        "params": {
            "seq": 42
        },
        "ts": "2023-11-03T12:00:00Z"
    }
}

// 服务端 -> 客户端：推送新消息
{
    "data": {
        "topic": "usr_abc",
        "from": "usr_def",
        "seq": 42,
        "content": "Hi there!",
        "ts": "2023-11-03T12:00:00Z"
    }
}

// 客户端 -> 服务端：已读回执
{
    "id": "789",
    "note": {
        "topic": "usr_abc",
        "what": "read",
        "seq": 42
    }
}

// 客户端 -> 服务端：正在输入
{
    "note": {
        "topic": "usr_abc",
        "what": "kp"
    }
}
```

### 4.3 建议的 MallChat WebSocket 协议

综合 OpenIM 和 Tinode 的设计，建议 MallChat 采用 JSON 协议：

```json
// ========== WebSocket 消息帧结构 ==========
{
    "type": "消息类型",           // 必填：消息类型标识
    "requestId": "uuid_xxx",     // 可选：请求ID，用于匹配响应
    "data": { ... }              // 消息数据
}

// ========== 客户端 -> 服务端 ==========

// 1. 认证
{
    "type": "auth",
    "requestId": "req_001",
    "data": {
        "token": "jwt_token_xxx"
    }
}

// 2. 发送消息
{
    "type": "message.send",
    "requestId": "req_002",
    "data": {
        "clientMsgId": "client_msg_001",
        "roomId": 1001,
        "content": "Hello!",
        "msgType": 1,
        "replyMsgId": null,
        "extra": {}
    }
}

// 3. 消息已读
{
    "type": "message.read",
    "requestId": "req_003",
    "data": {
        "roomId": 1001,
        "msgId": 5001
    }
}

// 4. 正在输入
{
    "type": "message.typing",
    "data": {
        "roomId": 1001
    }
}

// ========== 服务端 -> 客户端 ==========

// 1. 认证成功
{
    "type": "auth.success",
    "requestId": "req_001",
    "data": {
        "userId": 10001,
        "serverTime": 1699000000000
    }
}

// 2. 消息发送确认
{
    "type": "message.send.ack",
    "requestId": "req_002",
    "data": {
        "clientMsgId": "client_msg_001",
        "serverMsgId": 5002,
        "sendTime": 1699000000000,
        "status": 1
    }
}

// 3. 推送新消息
{
    "type": "message.push",
    "data": {
        "serverMsgId": 5002,
        "roomId": 1001,
        "fromUserId": 10001,
        "content": "Hello!",
        "msgType": 1,
        "sendTime": 1699000000000,
        "extra": {},
        "sender": {
            "userId": 10001,
            "userName": "张三",
            "userAvatar": "https://..."
        }
    }
}

// 4. 已读通知
{
    "type": "message.read.notify",
    "data": {
        "roomId": 1001,
        "userId": 10002,
        "msgId": 5001,
        "readTime": 1699000000000
    }
}

// 5. 正在输入通知
{
    "type": "message.typing.notify",
    "data": {
        "roomId": 1001,
        "userId": 10002,
        "userName": "李四"
    }
}

// 6. 会话更新通知
{
    "type": "session.update",
    "data": {
        "roomId": 1001,
        "lastMsgId": 5002,
        "lastMsgContent": "Hello!",
        "unreadCount": 1,
        "activeTime": 1699000000000
    }
}

// 7. 错误响应
{
    "type": "error",
    "requestId": "req_002",
    "data": {
        "code": 400,
        "message": "消息内容不能为空"
    }
}
```

---

## 五、消息存储策略：写扩散 vs 读扩散

### 5.1 两种策略对比

| 维度 | 写扩散 (Write Fan-out) | 读扩散 (Read Fan-out) |
|------|----------------------|---------------------|
| 写入方式 | 消息写入每个接收者的收件箱 | 消息只写一份到公共时间线 |
| 读取方式 | 直接从自己的收件箱读取 | 需要聚合多个发送者的消息 |
| 写入成本 | 高（N 个成员 = N 次写入） | 低（只写 1 次） |
| 读取成本 | 低（O(1) 读取） | 高（需要聚合） |
| 存储成本 | 高（消息重复存储） | 低（消息只存一份） |
| 未读数计算 | 简单（直接计数） | 复杂（需要对比 seq） |
| 适用场景 | 小群、私聊 | 大群、频道 |

### 5.2 各项目策略选择

#### OpenIM 的策略

```
私聊（单聊）: 写扩散
  - 消息同时写入发送者和接收者的会话
  - 每个用户有独立的 seq 序列

普通群聊: 写扩散
  - 消息写入每个群成员的会话
  - 每个成员有独立的 seq 和未读数

超大群聊: 读扩散
  - 消息只写一份到群的时间线
  - 成员按需拉取（基于 seq）
```

#### Tinode 的策略

```
所有会话: 读扩散
  - 消息按 topic（话题）存储
  - 每个 topic 有独立的 seq 序列
  - 用户通过 subscription 关联 topic
  - 用户的 unread count 通过 lastSeenSeq 计算
```

### 5.3 MallChat 策略建议

基于 MallChat 的定位（中小型 IM），建议：

```
私聊: 写扩散
  - 消息写入 chat_message 表（已实现）
  - 更新双方的 chat_session 表

普通群聊: 写扩散
  - 消息写入 chat_message 表
  - 更新每个成员的 chat_session 表的 unread_count

大型群聊（如 500+ 人）: 混合策略
  - 消息写入 chat_message 表
  - 不主动更新每个成员的 chat_session
  - 成员打开会话时，基于 last_read_message_id 计算未读数
```

**实现示例**:

```java
// 写扩散实现
public void sendMessage(Message msg) {
    // 1. 保存消息
    messageMapper.insert(msg);
    
    // 2. 更新发送者会话
    sessionMapper.updateLastMsg(msg.getFromUserId(), msg.getRoomId(), msg.getId());
    
    // 3. 获取房间所有成员
    List<Long> members = roomMemberMapper.getMemberIds(msg.getRoomId());
    
    // 4. 更新每个成员的会话（写扩散）
    for (Long memberId : members) {
        if (!memberId.equals(msg.getFromUserId())) {
            sessionMapper.incrementUnread(memberId, msg.getRoomId());
            sessionMapper.updateLastMsg(memberId, msg.getRoomId(), msg.getId());
        }
    }
    
    // 5. WebSocket 推送
    wsService.pushToRoom(msg.getRoomId(), msg);
}
```

---

## 六、多端同步方案

### 6.1 OpenIM 的多端同步方案

OpenIM 使用 **基于序列号（Seq）的同步机制**：

```
核心概念：
- 每个用户有全局唯一的 msg_seq（消息序列号）
- 每个会话有独立的 conversation_seq
- 客户端记录最后同步的 seq

同步流程：
1. 客户端连接时，上报本地最大的 seq
2. 服务端返回 seq 之后的所有消息
3. 客户端按 seq 顺序处理消息
4. 多端设备各自维护自己的 seq
```

```
设备 A (已同步到 seq=100)
  |
  |--- 请求 seq > 100 的消息
  |
服务端
  |
  |--- 返回 seq 101-150 的消息
  |
设备 A (同步到 seq=150)

设备 B (已同步到 seq=120)
  |
  |--- 请求 seq > 120 的消息
  |
服务端
  |
  |--- 返回 seq 121-150 的消息
  |
设备 B (同步到 seq=150)
```

### 6.2 Tinode 的多端同步方案

Tinode 使用 **Topic-based 的订阅模型**：

```
核心概念：
- 每个会话是一个 topic
- 用户通过 subscription 订阅 topic
- 每个 subscription 记录 lastSeenSeq
- 未读数 = topic.maxSeq - subscription.lastSeenSeq

同步流程：
1. 客户端订阅 topic 时，获取 topic 的 maxSeq
2. 对比本地 lastSeenSeq，拉取差值消息
3. 多端设备各自维护 subscription 的 lastSeenSeq
```

### 6.3 MallChat 多端同步方案建议

基于 MallChat 当前的 `chat_session` 表设计，建议：

```sql
-- 扩展 chat_session 表
ALTER TABLE `chat_session` ADD COLUMN `sync_seq` BIGINT DEFAULT 0 COMMENT '同步序列号';
ALTER TABLE `chat_room` ADD COLUMN `max_seq` BIGINT DEFAULT 0 COMMENT '房间最大消息序列号';
```

**同步流程**:

```
1. 客户端 WebSocket 连接时，上报各会话的 sync_seq
2. 服务端对比 room.max_seq 和 session.sync_seq
3. 如果有新消息，推送 sync_seq 之后的消息
4. 客户端更新本地 sync_seq
```

---

## 七、文件上传和存储方案

### 7.1 OpenIM 文件存储方案

OpenIM 使用 **MinIO（S3 兼容）** 存储文件：

```
存储架构：
Client → OpenIM Server → MinIO/S3

上传流程：
1. 客户端请求上传，获取预签名 URL
2. 客户端直接上传到 MinIO
3. 上传完成后，回调服务端记录文件信息
4. 发送消息时携带文件 URL

API 设计：
POST /api/v1/object/upload          # 上传文件
GET  /api/v1/object/get             # 下载文件
GET  /api/v1/object/thumbnail       # 获取缩略图
```

**配置示例**:

```yaml
# MinIO 配置
minio:
  endpoint: http://minio:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket:
    image: mallchat-images
    video: mallchat-videos
    file: mallchat-files
```

### 7.2 Tinode 文件存储方案

Tinode 使用 **可插拔存储适配器**：

```
存储架构：
Client → Tinode Server → Storage Adapter → S3/Local FS

上传流程：
1. 客户端上传文件到 /v0/file/u 端点
2. 服务端存储文件，返回文件 ID
3. 消息内容通过 file ID 引用文件
4. 下载时通过 /v0/file/s?id=xxx 获取

存储表结构：
CREATE TABLE `fileuploads` (
    `id`        BIGINT PRIMARY KEY AUTO_INCREMENT,
    `createdat` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `status`    INT DEFAULT 0,
    `location`  VARCHAR(512) NOT NULL,  -- S3 key 或本地路径
    `mimetype`  VARCHAR(128) NOT NULL,
    `size`      BIGINT NOT NULL
);
```

### 7.3 MallChat 文件存储方案建议

MallChat 已有 `file_upload_record` 表，设计良好：

```sql
CREATE TABLE `file_upload_record` (
    `id`            BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    `user_id`       BIGINT        NOT NULL COMMENT '上传用户ID',
    `biz_type`      VARCHAR(64)   NOT NULL COMMENT '业务类型',
    `file_name`     VARCHAR(512)  NOT NULL COMMENT '原始文件名',
    `file_size`     BIGINT        NOT NULL COMMENT '文件大小',
    `file_suffix`   VARCHAR(32)   DEFAULT NULL COMMENT '文件后缀',
    `content_type`  VARCHAR(128)  DEFAULT NULL COMMENT '内容类型',
    `storage_type`  VARCHAR(32)   NOT NULL COMMENT '存储类型',
    `bucket`        VARCHAR(128)  DEFAULT NULL COMMENT '存储桶',
    `object_key`    VARCHAR(512)  NOT NULL COMMENT '对象键/路径',
    `url`           VARCHAR(1024) NOT NULL COMMENT '访问URL',
    `md5`           VARCHAR(64)   DEFAULT NULL COMMENT '文件MD5',
    `client_ip`     VARCHAR(64)   DEFAULT NULL COMMENT '客户端IP',
    `status`        VARCHAR(32)   NOT NULL DEFAULT 'SUCCESS' COMMENT '上传状态',
    `error_message` VARCHAR(1024) DEFAULT NULL COMMENT '错误信息',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`     TINYINT       NOT NULL DEFAULT 0 COMMENT '是否删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件上传记录表';
```

**建议补充**:
- 支持预签名 URL 直传模式，减轻服务端压力
- 图片消息增加缩略图生成逻辑
- 文件消息在 `chat_message.extra` 中存储文件元信息

```json
// 图片消息 extra 示例
{
    "fileId": 12345,
    "url": "https://cdn.mallchat.com/images/xxx.jpg",
    "thumbnailUrl": "https://cdn.mallchat.com/images/xxx_thumb.jpg",
    "width": 1920,
    "height": 1080,
    "size": 256000,
    "mimeType": "image/jpeg"
}

// 文件消息 extra 示例
{
    "fileId": 12346,
    "url": "https://cdn.mallchat.com/files/xxx.pdf",
    "fileName": "文档.pdf",
    "fileSize": 1024000,
    "mimeType": "application/pdf"
}
```

---

## 八、权限控制参考（RuoYi-Vue + JeecgBoot）

### 8.1 RBAC 权限模型

```
用户 (User) ──┬── 角色 (Role) ──┬── 菜单/权限 (Menu/Permission)
              │                 │
              │                 └── 部门数据权限 (Dept Data Scope)
              │
              └── 部门 (Department)
```

**RuoYi 的三级权限**:

```
菜单类型：
- M (目录) → 顶级菜单分组
- C (菜单) → 页面级访问
- F (按钮) → 操作级权限

权限字符串格式：
- system:user:list      → 查看用户列表
- system:user:add       → 新增用户
- system:user:edit      → 修改用户
- system:user:remove    → 删除用户

数据权限范围：
1 = 全部数据
2 = 自定义数据
3 = 本部门数据
4 = 本部门及以下数据
5 = 仅本人数据
```

### 8.2 MallChat 权限设计建议

MallChat 当前使用简单的 `user_role` 字段（user/admin/ban），对于 IM 场景已经足够。如需扩展，可参考：

```sql
-- 简化的权限表设计（如需要）
CREATE TABLE `sys_permission` (
    `id`          BIGINT      PRIMARY KEY AUTO_INCREMENT,
    `name`        VARCHAR(64) NOT NULL COMMENT '权限名称',
    `code`        VARCHAR(64) NOT NULL COMMENT '权限编码',
    `type`        TINYINT     NOT NULL COMMENT '类型：1-菜单，2-按钮',
    `parent_id`   BIGINT      DEFAULT 0 COMMENT '父级ID',
    `path`        VARCHAR(256) DEFAULT NULL COMMENT '路由路径',
    `icon`        VARCHAR(64)  DEFAULT NULL COMMENT '图标',
    `sort_order`  INT         DEFAULT 0 COMMENT '排序',
    `status`      TINYINT     DEFAULT 1 COMMENT '状态',
    `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_delete`   TINYINT     DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

CREATE TABLE `sys_role` (
    `id`          BIGINT      PRIMARY KEY AUTO_INCREMENT,
    `name`        VARCHAR(64) NOT NULL COMMENT '角色名称',
    `code`        VARCHAR(64) NOT NULL COMMENT '角色编码',
    `status`      TINYINT     DEFAULT 1 COMMENT '状态',
    `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_delete`   TINYINT     DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE `sys_user_role` (
    `user_id` BIGINT NOT NULL,
    `role_id` BIGINT NOT NULL,
    PRIMARY KEY (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

CREATE TABLE `sys_role_permission` (
    `role_id`       BIGINT NOT NULL,
    `permission_id` BIGINT NOT NULL,
    PRIMARY KEY (`role_id`, `permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';
```

---

## 九、代码生成参考（JeecgBoot）

### 9.1 JeecgBoot 代码生成架构

```
数据库表 → 代码生成器 → Controller + Service + Mapper + Entity + Vue 页面

技术栈：
- 后端：Spring Boot + MyBatis-Plus
- 前端：Vue 3 + Ant Design Vue
- 模板引擎：Velocity / Freemarker
```

### 9.2 代码生成模板结构

```
生成文件清单：
├── entity/
│   └── ${EntityName}.java          # 实体类
├── mapper/
│   └── ${EntityName}Mapper.java    # Mapper 接口
├── service/
│   ├── ${EntityName}Service.java   # Service 接口
│   └── impl/
│       └── ${EntityName}ServiceImpl.java  # Service 实现
├── controller/
│   └── ${EntityName}Controller.java      # Controller
└── vue/
    ├── api/
    │   └── ${entityName}.js         # API 接口
    └── views/
        └── ${entityName}/
            ├── index.vue            # 列表页
            ├── modules/
            │   └── ${EntityName}Modal.vue  # 编辑弹窗
            └── ${EntityName}List.vue       # 详情页
```

### 9.3 MallChat 代码生成建议

MallChat 可参考 JeecgBoot 的代码生成思路，但应保持 MVP 原则：

```
建议的代码生成范围：
1. Entity（实体类）- 基于数据库表自动生成
2. Mapper（数据访问层）- 基于 MyBatis-Plus
3. VO/DTO（视图对象）- 基于 Entity 生成
4. Controller（接口层）- 生成标准 CRUD 接口

不建议自动生成的：
1. Service 层 - 业务逻辑应手动编写
2. 前端页面 - 根据实际需求定制
```

---

## 十、架构设计总结与建议

### 10.1 MallChat 当前架构优势

1. **房间抽象设计**: `chat_room` + `chat_room_member` 的设计统一了群聊和私聊
2. **私聊映射表**: `user_low` + `user_high` 的设计避免重复创建私聊房间
3. **会话表设计**: `chat_session` 表的 `last_read_message_id` 和 `unread_count` 设计合理
4. **消息幂等**: `client_msg_id` 的唯一索引实现了消息去重
5. **文件存储**: `file_upload_record` 表设计完善，支持多存储类型

### 10.2 可优化方向

| 优化项 | 参考项目 | 建议 |
|--------|---------|------|
| 消息序号 | OpenIM | 增加 `seq` 字段支持多端同步 |
| 好友备注 | OpenIM | `user_friend` 增加 `remark` 字段 |
| 群组扩展 | OpenIM | `chat_group_info` 增加 `introduction`、`member_count` |
| 会话扩展 | OpenIM | `chat_session` 增加 `draft_text`、`recv_msg_opt` |
| WebSocket 协议 | Tinode | 采用 JSON 格式，统一消息帧结构 |
| 多端同步 | OpenIM | 基于 seq 的增量同步机制 |
| 文件直传 | OpenIM | 支持预签名 URL 直传模式 |
| 消息已读 | Tinode | 增加 `note` 类型消息处理已读和输入状态 |

### 10.3 技术选型建议

| 组件 | 推荐方案 | 备选方案 |
|------|---------|---------|
| 数据库 | MySQL 8.0 | PostgreSQL |
| 缓存 | Redis 7.x | - |
| 消息队列 | Redis Stream | RabbitMQ / Kafka |
| 文件存储 | MinIO (S3 兼容) | 阿里云 OSS / 腾讯云 COS |
| WebSocket | Spring WebSocket | Netty |
| 序列化 | JSON | Protobuf |
| 认证 | JWT | Session |

---

## 十一、参考资料

- [OpenIM Server GitHub](https://github.com/openimsdk/open-im-server)
- [OpenIM 官方文档](https://doc.openim.io)
- [Tinode Chat GitHub](https://github.com/tinode/chat)
- [Tinode 协议文档](https://github.com/tinode/chat/blob/master/docs/API.md)
- [RuoYi-Vue GitHub](https://github.com/yangzongzhuan/RuoYi-Vue)
- [JeecgBoot GitHub](https://github.com/jeecgboot/JeecgBoot)
- [JeecgBoot 官方文档](http://doc.jeecg.com)
