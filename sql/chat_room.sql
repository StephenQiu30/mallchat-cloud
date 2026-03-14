-- ============================================
-- 5. 聊天室相关表
-- ============================================
USE `mallchat_cloud`;
-- 聊天室表
DROP TABLE IF EXISTS `chat_room`;
CREATE TABLE `chat_room`
(
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '房间ID',
    `name`        varchar(64) NOT NULL COMMENT '房间名称',
    `type`        tinyint     NOT NULL DEFAULT 1 COMMENT '房间类型：1-群聊，2-私聊',
    `avatar`      varchar(256)         DEFAULT NULL COMMENT '房间头像',
    `create_user` bigint      NOT NULL COMMENT '创建者用户ID',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`   tinyint     NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_name` (`name`),
    KEY `idx_type` (`type`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '聊天室表';

-- 聊天室成员表
DROP TABLE IF EXISTS `chat_room_member`;
CREATE TABLE `chat_room_member`
(
    `id`             bigint   NOT NULL AUTO_INCREMENT COMMENT '成员ID',
    `room_id`        bigint   NOT NULL COMMENT '房间ID',
    `user_id`        bigint   NOT NULL COMMENT '用户ID',
    `role`           tinyint  NOT NULL DEFAULT 1 COMMENT '角色：1-普通成员，2-管理员，3-群主',
    `last_read_message_id` bigint DEFAULT NULL COMMENT '最后已读消息ID',
    `create_time`    datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `update_time`    datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`      tinyint  NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_room_user` (`room_id`, `user_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_room_id` (`room_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '聊天室成员表';

-- 聊天消息表
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message`
(
    `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `room_id`     bigint   NOT NULL COMMENT '房间ID',
    `from_user_id` bigint   NOT NULL COMMENT '发送者ID',
    `content`     text     NOT NULL COMMENT '消息内容',
    `type`        tinyint  NOT NULL DEFAULT 1 COMMENT '消息类型：1-文本，2-图片，3-文件',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`   tinyint  NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_room_id` (`room_id`),
    KEY `idx_from_user_id` (`from_user_id`),
    KEY `idx_room_id_create_time` (`room_id`, `create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '聊天消息表';
