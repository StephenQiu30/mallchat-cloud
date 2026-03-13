-- ============================================
-- mallchat-cloud 全系统数据库初始化脚本
-- ============================================

-- 1. 创建数据库
CREATE DATABASE IF NOT EXISTS `mallchat_cloud` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `mallchat_cloud`;

-- ============================================
-- 2. 用户服务相关表
-- ============================================

-- 用户表
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `user_name`       varchar(256)          DEFAULT NULL COMMENT '用户昵称',
    `user_avatar`     varchar(1024)         DEFAULT NULL COMMENT '用户头像',
    `user_profile`    varchar(512)          DEFAULT NULL COMMENT '用户简介',
    `user_role`       varchar(256) NOT NULL DEFAULT 'user' COMMENT '用户角色：user/admin/ban',
    `user_phone`      varchar(128)          DEFAULT NULL COMMENT '用户手机号',
    `mp_open_id`      varchar(256)          DEFAULT NULL COMMENT '微信公众号 OpenID',
    `wx_union_id`     varchar(256)          DEFAULT NULL COMMENT '微信 UnionID',
    `wx_open_id`      varchar(256)          DEFAULT NULL COMMENT '微信开放平台 OpenID',
    `github_id`       varchar(256)          DEFAULT NULL COMMENT 'GitHub ID',
    `github_login`    varchar(256)          DEFAULT NULL COMMENT 'GitHub 账号',
    `github_url`      varchar(512)          DEFAULT NULL COMMENT 'GitHub 主页',
    `last_login_time` datetime              DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip`   varchar(128)          DEFAULT NULL COMMENT '最后登录IP',
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`       tinyint      NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_github_id` (`github_id`),
    KEY `idx_wx_union_id` (`wx_union_id`),
    KEY `idx_user_phone` (`user_phone`),
    KEY `idx_github_id_is_delete` (`github_id`, `is_delete`),
    KEY `idx_wx_union_id_is_delete` (`wx_union_id`, `is_delete`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '用户表';

-- 用户登录日志表
DROP TABLE IF EXISTS `user_login_log`;
CREATE TABLE `user_login_log`
(
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '登录日志ID',
    `user_id`     bigint               DEFAULT NULL COMMENT '用户ID',
    `account`     varchar(256)         DEFAULT NULL COMMENT '登录账号',
    `login_type`  varchar(64)          DEFAULT NULL COMMENT '登录类型',
    `status`      varchar(32) NOT NULL COMMENT '登录状态',
    `fail_reason` varchar(512)         DEFAULT NULL COMMENT '失败原因',
    `client_ip`   varchar(64)          DEFAULT NULL COMMENT '客户端IP',
    `location`    varchar(256)         DEFAULT NULL COMMENT '归属地',
    `user_agent`  varchar(512)         DEFAULT NULL COMMENT 'User-Agent',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`   tinyint     NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_account` (`account`),
    KEY `idx_status_create_time` (`status`, `create_time` DESC),
    KEY `idx_client_ip` (`client_ip`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '用户登录日志表';

-- ============================================
-- 3. 日志与存储相关表
-- ============================================

-- 接口访问日志表
DROP TABLE IF EXISTS `api_access_log`;
CREATE TABLE `api_access_log`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `trace_id`      varchar(64)           DEFAULT NULL COMMENT '链路追踪ID',
    `user_id`       bigint                DEFAULT NULL COMMENT '用户ID',
    `method`        varchar(16)  NOT NULL COMMENT 'HTTP方法',
    `path`          varchar(512) NOT NULL COMMENT '请求路径',
    `query`         varchar(1024)         DEFAULT NULL COMMENT '查询参数',
    `status`        int                   DEFAULT NULL COMMENT '响应状态码',
    `latency_ms`    int                   DEFAULT NULL COMMENT '耗时毫秒',
    `client_ip`     varchar(64)           DEFAULT NULL COMMENT '客户端IP',
    `user_agent`    varchar(512)          DEFAULT NULL COMMENT 'User-Agent',
    `referer`       varchar(512)          DEFAULT NULL COMMENT 'Referer',
    `request_size`  bigint                DEFAULT NULL COMMENT '请求大小',
    `response_size` bigint                DEFAULT NULL COMMENT '响应大小',
    `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`     tinyint      NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_path` (`path`(191)),
    KEY `idx_status` (`status`),
    KEY `idx_client_ip` (`client_ip`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_trace_id` (`trace_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '接口访问日志表';

-- 操作日志表
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log`
(
    `id`              bigint   NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `operator_id`     bigint            DEFAULT NULL COMMENT '操作人ID',
    `operator_name`   varchar(128)      DEFAULT NULL COMMENT '操作人名称',
    `module`          varchar(64)       DEFAULT NULL COMMENT '模块',
    `action`          varchar(128)      DEFAULT NULL COMMENT '操作类型',
    `method`          varchar(16)       DEFAULT NULL COMMENT 'HTTP方法',
    `path`            varchar(512)      DEFAULT NULL COMMENT '请求路径',
    `request_params`  text COMMENT '请求参数',
    `response_status` int               DEFAULT NULL COMMENT '响应状态码',
    `success`         tinyint  NOT NULL DEFAULT 1 COMMENT '是否成功',
    `error_message`   varchar(1024)     DEFAULT NULL COMMENT '错误信息',
    `client_ip`       varchar(64)       DEFAULT NULL COMMENT '客户端IP',
    `location`        varchar(256)      DEFAULT NULL COMMENT '归属地',
    `create_time`     datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`       tinyint  NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_operator_id` (`operator_id`),
    KEY `idx_module` (`module`),
    KEY `idx_success` (`success`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '操作日志表';

-- 文件上传记录表
DROP TABLE IF EXISTS `file_upload_record`;
CREATE TABLE `file_upload_record`
(
    `id`            bigint        NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id`       bigint        NOT NULL COMMENT '上传用户ID',
    `biz_type`      varchar(64)   NOT NULL COMMENT '业务类型',
    `file_name`     varchar(512)  NOT NULL COMMENT '原始文件名',
    `file_size`     bigint        NOT NULL COMMENT '文件大小',
    `file_suffix`   varchar(32)            DEFAULT NULL COMMENT '文件后缀',
    `content_type`  varchar(128)           DEFAULT NULL COMMENT '内容类型',
    `storage_type`  varchar(32)   NOT NULL COMMENT '存储类型',
    `bucket`        varchar(128)           DEFAULT NULL COMMENT '存储桶',
    `object_key`    varchar(512)  NOT NULL COMMENT '对象键/路径',
    `url`           varchar(1024) NOT NULL COMMENT '访问URL',
    `md5`           varchar(64)            DEFAULT NULL COMMENT '文件MD5',
    `client_ip`     varchar(64)            DEFAULT NULL COMMENT '客户端IP',
    `status`        varchar(32)   NOT NULL DEFAULT 'SUCCESS' COMMENT '上传状态',
    `error_message` varchar(1024)          DEFAULT NULL COMMENT '错误信息',
    `create_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`     tinyint       NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_biz_type` (`biz_type`),
    KEY `idx_md5` (`md5`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_storage_type` (`storage_type`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '文件上传记录表';


-- ============================================
-- 4. 业务与消息相关表
-- ============================================

-- 通知表
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification`
(
    `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '通知ID',
    `title`        varchar(256) NOT NULL COMMENT '通知标题',
    `content`      text         NOT NULL COMMENT '通知内容',
    `type`         varchar(64)  NOT NULL COMMENT '通知类型（system-系统通知，user-用户通知，comment-评论通知，like-点赞通知，follow-关注通知，broadcast-全员广播）',
    `biz_id`       varchar(128) NOT NULL DEFAULT '' COMMENT '业务幂等ID',
    `user_id`      bigint       NOT NULL COMMENT '接收用户ID',
    `related_id`   bigint                DEFAULT NULL COMMENT '关联对象ID',
    `related_type` varchar(64)  NOT NULL DEFAULT '' COMMENT '关联对象类型',
    `is_read`      tinyint      NOT NULL DEFAULT 0 COMMENT '是否已读',
    `status`       tinyint      NOT NULL DEFAULT 0 COMMENT '状态（0-正常，1-停用）',
    `content_url`  varchar(512) NOT NULL DEFAULT '' COMMENT '跳转链接',
    `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`    tinyint      NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz_user` (`biz_id`, `user_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_type` (`type`),
    KEY `idx_is_read` (`is_read`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_user_id_is_read_create_time` (`user_id`, `is_read`, `create_time` DESC)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '通知表';

-- AI 对话记录表
DROP TABLE IF EXISTS `ai_chat_record`;
CREATE TABLE `ai_chat_record`
(
    `id`                bigint   NOT NULL AUTO_INCREMENT COMMENT '对话ID',
    `user_id`           bigint   NOT NULL COMMENT '用户ID',
    `session_id`        varchar(128)      DEFAULT NULL COMMENT '会话ID',
    `message`           text     NOT NULL COMMENT '对话消息',
    `response`          text              DEFAULT NULL COMMENT 'AI响应内容',
    `model_type`        varchar(128)      DEFAULT NULL COMMENT '模型类型',
    `total_tokens`      int               DEFAULT NULL COMMENT '总消耗 token',
    `prompt_tokens`     int               DEFAULT NULL COMMENT '提示消耗 token',
    `completion_tokens` int               DEFAULT NULL COMMENT '生成消耗 token',
    `create_time`       datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`         tinyint  NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_user_id_create_time` (`user_id`, `create_time` DESC),
    KEY `idx_session_create` (`session_id`, `create_time` DESC)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = 'AI 对话记录表';
