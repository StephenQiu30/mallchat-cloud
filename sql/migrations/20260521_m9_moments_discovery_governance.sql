-- m9 动态发现与内容治理增量升级脚本
-- 适用范围：已有 mallchat 数据库升级到 m9。
-- 执行前请先备份生产数据；新环境仍可直接执行 sql/mallchat.sql。

USE `mallchat`;

DROP PROCEDURE IF EXISTS `add_m9_moment_discovery_columns`;

DELIMITER //

CREATE PROCEDURE `add_m9_moment_discovery_columns`()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM `information_schema`.`COLUMNS`
        WHERE `TABLE_SCHEMA` = DATABASE()
          AND `TABLE_NAME` = 'chat_moment'
          AND `COLUMN_NAME` = 'visibility'
    ) THEN
        ALTER TABLE `chat_moment`
            ADD COLUMN `visibility` tinyint NOT NULL DEFAULT 0 COMMENT '可见范围：0-好友可见，1-公开' AFTER `comment_count`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM `information_schema`.`COLUMNS`
        WHERE `TABLE_SCHEMA` = DATABASE()
          AND `TABLE_NAME` = 'chat_moment'
          AND `COLUMN_NAME` = 'audit_status'
    ) THEN
        ALTER TABLE `chat_moment`
            ADD COLUMN `audit_status` tinyint NOT NULL DEFAULT 1 COMMENT '审核状态：0-待审，1-通过，2-拒绝' AFTER `visibility`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM `information_schema`.`STATISTICS`
        WHERE `TABLE_SCHEMA` = DATABASE()
          AND `TABLE_NAME` = 'chat_moment'
          AND `INDEX_NAME` = 'idx_public_rank'
    ) THEN
        ALTER TABLE `chat_moment`
            ADD INDEX `idx_public_rank` (`visibility`, `audit_status`, `status`, `like_count`, `comment_count`, `create_time` DESC, `id` DESC);
    END IF;
END //

DELIMITER ;

CALL `add_m9_moment_discovery_columns`();

DROP PROCEDURE IF EXISTS `add_m9_moment_discovery_columns`;
