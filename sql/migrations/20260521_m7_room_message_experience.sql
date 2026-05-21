-- m7 群聊治理与消息体验增量升级脚本
-- 适用范围：已有 mallchat 数据库升级到 m7。
-- 执行前请先备份生产数据；新环境仍可直接执行 sql/mallchat.sql。

USE `mallchat`;

CREATE TABLE IF NOT EXISTS `chat_room_join_apply`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `room_id`     bigint       NOT NULL COMMENT '群聊 id',
    `user_id`     bigint       NOT NULL COMMENT '申请人 id',
    `reviewer_id` bigint                DEFAULT NULL COMMENT '审核人 id',
    `msg`         varchar(256) NOT NULL DEFAULT '' COMMENT '申请留言',
    `review_msg`  varchar(256)          DEFAULT NULL COMMENT '审核留言',
    `status`      tinyint      NOT NULL DEFAULT 1 COMMENT '状态：1-待处理，2-已同意，3-已拒绝',
    `active_key`  varchar(64)           DEFAULT NULL COMMENT '待处理幂等键：roomId:userId',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`   tinyint      NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_active_key` (`active_key`),
    KEY `idx_room_status_time` (`room_id`, `status`, `create_time`),
    KEY `idx_user_time` (`user_id`, `create_time`),
    KEY `idx_reviewer_id` (`reviewer_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '入群申请表';

DROP PROCEDURE IF EXISTS `add_m7_mute_status`;

DELIMITER //

CREATE PROCEDURE `add_m7_mute_status`()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM `information_schema`.`COLUMNS`
        WHERE `TABLE_SCHEMA` = DATABASE()
          AND `TABLE_NAME` = 'chat_session'
          AND `COLUMN_NAME` = 'mute_status'
    ) THEN
        ALTER TABLE `chat_session`
            ADD COLUMN `mute_status` tinyint NOT NULL DEFAULT 0 COMMENT '免打扰状态：0-否，1-是' AFTER `top_status`;
    END IF;
END //

DELIMITER ;

CALL `add_m7_mute_status`();

DROP PROCEDURE IF EXISTS `add_m7_mute_status`;
