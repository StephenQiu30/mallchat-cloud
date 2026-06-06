-- E2E Test Schema for H2 database
-- MySQL-compatible schema adapted for H2

-- 用户表
CREATE TABLE IF NOT EXISTS `user`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `user_name`       varchar(256)          DEFAULT NULL COMMENT '用户昵称',
    `user_avatar`     varchar(1024)         DEFAULT NULL COMMENT '用户头像',
    `user_profile`    varchar(512)          DEFAULT NULL COMMENT '用户简介',
    `user_role`       varchar(256) NOT NULL DEFAULT 'user' COMMENT '用户角色：user/admin/ban',
    `user_phone`      varchar(128)          DEFAULT NULL COMMENT '用户手机号',
    `user_email`      varchar(256)          DEFAULT NULL COMMENT '用户邮箱',
    `ma_open_id`      varchar(256)          DEFAULT NULL COMMENT '微信小程序 OpenID',
    `wx_union_id`     varchar(256)          DEFAULT NULL COMMENT '微信 UnionID',
    `wx_open_id`      varchar(256)          DEFAULT NULL COMMENT '微信开放平台 OpenID',
    `apple_id`        varchar(256)          DEFAULT NULL COMMENT 'Apple ID',
    `last_login_time` datetime              DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip`   varchar(128)          DEFAULT NULL COMMENT '最后登录IP',
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`       tinyint      NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`)
);

-- 用户好友表
CREATE TABLE IF NOT EXISTS `user_friend`
(
    `id`                bigint   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`           bigint   NOT NULL COMMENT '用户ID',
    `friend_user_id`    bigint   NOT NULL COMMENT '好友用户ID',
    `remark_name`       varchar(64)      DEFAULT NULL COMMENT '好友备注',
    `friend_group_name` varchar(32)      DEFAULT '默认分组' COMMENT '好友分组名称',
    `create_time`       datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`         tinyint  NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_friend` (`user_id`, `friend_user_id`)
);

-- 好友申请表
CREATE TABLE IF NOT EXISTS `user_friend_apply`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '申请ID',
    `user_id`     bigint       NOT NULL COMMENT '发起用户ID',
    `target_id`   bigint       NOT NULL COMMENT '目标用户ID',
    `msg`         varchar(256) NOT NULL COMMENT '申请消息',
    `status`      tinyint      NOT NULL DEFAULT 1 COMMENT '状态：1-待处理，2-已同意，3-已忽略',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`   tinyint      NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`)
);

-- 用户拉黑关系表
CREATE TABLE IF NOT EXISTS `user_friend_block`
(
    `id`              bigint   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`         bigint   NOT NULL COMMENT '拉黑用户ID',
    `blocked_user_id` bigint   NOT NULL COMMENT '被拉黑用户ID',
    `create_time`     datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_blocked` (`user_id`, `blocked_user_id`)
);

-- 聊天室表
CREATE TABLE IF NOT EXISTS `chat_room`
(
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '房间ID',
    `name`        varchar(64) NOT NULL COMMENT '房间名称',
    `type`        tinyint     NOT NULL DEFAULT 1 COMMENT '房间类型：1-群聊，2-私聊',
    `avatar`      varchar(256)         DEFAULT NULL COMMENT '房间头像',
    `create_user` bigint      NOT NULL COMMENT '创建者用户ID',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`   tinyint     NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`)
);

-- 聊天室成员表
CREATE TABLE IF NOT EXISTS `chat_room_member`
(
    `id`                   bigint   NOT NULL AUTO_INCREMENT COMMENT '成员ID',
    `room_id`              bigint   NOT NULL COMMENT '房间ID',
    `user_id`              bigint   NOT NULL COMMENT '用户ID',
    `role`                 tinyint  NOT NULL DEFAULT 1 COMMENT '角色：1-普通成员，2-管理员，3-群主',
    `last_read_message_id` bigint            DEFAULT NULL COMMENT '最后已读消息ID',
    `create_time`          datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `update_time`          datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`            tinyint  NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_room_user` (`room_id`, `user_id`)
);

-- 私聊房间映射表
CREATE TABLE IF NOT EXISTS `chat_private_room`
(
    `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_low`    bigint   NOT NULL COMMENT '用户ID较小值',
    `user_high`   bigint   NOT NULL COMMENT '用户ID较大值',
    `room_id`     bigint   NOT NULL COMMENT '私聊房间ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`   tinyint  NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_pair` (`user_low`, `user_high`)
);

-- 聊天消息表
CREATE TABLE IF NOT EXISTS `chat_message`
(
    `id`            bigint   NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `room_id`       bigint   NOT NULL COMMENT '房间ID',
    `from_user_id`  bigint   NOT NULL COMMENT '发送者ID',
    `client_msg_id` varchar(64) NOT NULL COMMENT '客户端消息ID',
    `content`       text     NOT NULL COMMENT '消息内容',
    `extra`         varchar(4096)         DEFAULT NULL COMMENT '消息扩展内容',
    `type`          tinyint  NOT NULL DEFAULT 1 COMMENT '消息类型：1-文本，2-图片，3-文件',
    `reply_msg_id`  bigint            DEFAULT NULL COMMENT '回复的消息ID',
    `status`        tinyint  NOT NULL DEFAULT 0 COMMENT '消息状态：0-正常，1-已撤回，2-已删除',
    `create_time`   datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    `update_time`   datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`     tinyint  NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_from_user_client_msg` (`from_user_id`, `client_msg_id`)
);

-- 会话列表
CREATE TABLE IF NOT EXISTS `chat_session`
(
    `id`                   bigint   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`              bigint   NOT NULL COMMENT '所属用户ID',
    `room_id`              bigint   NOT NULL COMMENT '房间ID',
    `last_message_id`      bigint            DEFAULT NULL COMMENT '最后一条消息ID',
    `last_read_message_id` bigint            DEFAULT NULL COMMENT '最后一条已读消息ID',
    `unread_count`         int      NOT NULL DEFAULT 0 COMMENT '未读数',
    `top_status`           tinyint  NOT NULL DEFAULT 0 COMMENT '置顶状态：0-否，1-是',
    `mute_status`          tinyint  NOT NULL DEFAULT 0 COMMENT '免打扰状态：0-否，1-是',
    `active_time`          datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后活跃时间',
    `create_time`          datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`          datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`            tinyint  NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_room` (`user_id`, `room_id`)
);

-- 群组详情表
CREATE TABLE IF NOT EXISTS `chat_group_info`
(
    `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `room_id`      bigint       NOT NULL COMMENT '房间ID',
    `group_name`   varchar(128) NOT NULL COMMENT '群聊名称',
    `group_avatar` varchar(512)          DEFAULT NULL COMMENT '群聊头像',
    `announcement` text                  DEFAULT NULL COMMENT '群公告',
    `create_user`  bigint       NOT NULL COMMENT '创建者用户ID',
    `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`    tinyint      NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_room_id` (`room_id`)
);

-- 通知表
CREATE TABLE IF NOT EXISTS `notification`
(
    `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '通知ID',
    `title`        varchar(256) NOT NULL COMMENT '通知标题',
    `content`      text         NOT NULL COMMENT '通知内容',
    `type`         varchar(64)  NOT NULL COMMENT '通知类型',
    `biz_id`       varchar(128) NOT NULL DEFAULT '' COMMENT '业务幂等ID',
    `user_id`      bigint       NOT NULL COMMENT '接收用户ID',
    `related_id`   bigint                DEFAULT NULL COMMENT '关联对象ID',
    `related_type` varchar(64)  NOT NULL DEFAULT '' COMMENT '关联对象类型',
    `is_read`      tinyint      NOT NULL DEFAULT 0 COMMENT '是否已读',
    `status`       tinyint      NOT NULL DEFAULT 0 COMMENT '状态',
    `content_url`  varchar(512) NOT NULL DEFAULT '' COMMENT '跳转链接',
    `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`    tinyint      NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz_user` (`biz_id`, `user_id`)
);

-- 入群申请表
CREATE TABLE IF NOT EXISTS `chat_room_join_apply`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '申请ID',
    `room_id`     bigint       NOT NULL COMMENT '房间ID',
    `user_id`     bigint       NOT NULL COMMENT '申请用户ID',
    `reviewer_id` bigint                DEFAULT NULL COMMENT '审核用户ID',
    `msg`         varchar(256) NOT NULL DEFAULT '' COMMENT '申请留言',
    `review_msg`  varchar(256)          DEFAULT NULL COMMENT '审核留言',
    `status`      tinyint      NOT NULL DEFAULT 1 COMMENT '状态：1-待处理，2-已同意，3-已拒绝',
    `active_key`  varchar(64)           DEFAULT NULL COMMENT '待处理幂等键',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`   tinyint      NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_active_key` (`active_key`)
);
