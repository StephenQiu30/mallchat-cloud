-- 审计事件表
CREATE TABLE `chat_audit_event`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '审计ID',
    `user_id`     bigint       NOT NULL COMMENT '操作用户ID',
    `action`      varchar(64)  NOT NULL COMMENT '操作类型：MESSAGE_SEND/MESSAGE_RECALL/USER_BLOCK/MEMBER_REMOVE',
    `target_type` varchar(32)  NOT NULL COMMENT '目标类型：MESSAGE/USER/ROOM_MEMBER',
    `target_id`   bigint       NOT NULL COMMENT '目标ID',
    `room_id`     bigint                DEFAULT NULL COMMENT '房间ID',
    `detail`      text                  DEFAULT NULL COMMENT '操作详情JSON',
    `client_ip`   varchar(64)           DEFAULT NULL COMMENT '客户端IP',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_action` (`user_id`, `action`),
    KEY `idx_room_time` (`room_id`, `create_time`),
    KEY `idx_target` (`target_type`, `target_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '聊天审计事件表';
