package com.stephen.cloud.common.rabbitmq.enums;

import com.stephen.cloud.common.rabbitmq.constants.RabbitMqConstant;
import lombok.Getter;

/**
 * MQ 业务类型策略枚举
 * <p>
 * 采用策略与枚举结合的设计模式，集中管理所有的 Exchange, RoutingKey 和 BizType。
 * 消除散落于各处的硬编码，使得发送时具备强类型检查。
 * </p>
 *
 * @author StephenQiu30
 */
@Getter
public enum MqBizTypeEnum {

    /**
     * WebSocket 推送 - 单发或组发
     */
    WEBSOCKET_PUSH("WEBSOCKET_PUSH", RabbitMqConstant.WEBSOCKET_EXCHANGE,
            RabbitMqConstant.WEBSOCKET_PUSH_ROUTING_KEY),

    /**
     * WebSocket 推送 - 全服广播
     */
    WEBSOCKET_BROADCAST("WEBSOCKET_BROADCAST", RabbitMqConstant.WEBSOCKET_EXCHANGE,
            RabbitMqConstant.WEBSOCKET_BROADCAST_ROUTING_KEY),

    /**
     * 全局通知
     */
    NOTIFICATION_SEND("NOTIFICATION_SEND", RabbitMqConstant.NOTIFICATION_EXCHANGE,
            RabbitMqConstant.NOTIFICATION_ROUTING_KEY),

    /**
     * 关注事件
     */
    FOLLOW_EVENT("FOLLOW_EVENT", RabbitMqConstant.NOTIFICATION_EXCHANGE,
            RabbitMqConstant.FOLLOW_EVENT_ROUTING_KEY),

    /**
     * AI 对话记录同步
     */
    AI_CHAT_RECORD("AI_CHAT_RECORD", RabbitMqConstant.AI_CHAT_RECORD_EXCHANGE,
            RabbitMqConstant.AI_CHAT_RECORD_ROUTING_KEY),

    /**
     * 聊天消息推送
     */
    CHAT_MESSAGE_PUSH("CHAT_MESSAGE_PUSH", RabbitMqConstant.WEBSOCKET_EXCHANGE,
            RabbitMqConstant.CHAT_MESSAGE_PUSH_ROUTING_KEY);

    /**
     * 业务类型唯一标识，由 Listener/Handler 使用 @MqHandler(bizType = "...") 进行订阅匹配
     */
    private final String value;

    /**
     * 目标交换机名称
     */
    private final String exchange;

    /**
     * 目标路由键
     */
    private final String routingKey;

    MqBizTypeEnum(String value, String exchange, String routingKey) {
        this.value = value;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }
}
