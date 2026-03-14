package com.stephen.cloud.common.rabbitmq.constants;

/**
 * RabbitMQ 常量
 *
 * @author StephenQiu30
 */
public interface RabbitMqConstant {


    // ==================== WebSocket 相关 ====================

    /**
     * WebSocket 交换机
     */
    String WEBSOCKET_EXCHANGE = "websocket.exchange";

    /**
     * WebSocket 推送队列
     */
    String WEBSOCKET_PUSH_QUEUE = "websocket.push.queue";

    /**
     * WebSocket 推送路由键
     */
    String WEBSOCKET_PUSH_ROUTING_KEY = "websocket.push";

    /**
     * WebSocket 广播队列
     */
    String WEBSOCKET_BROADCAST_QUEUE = "websocket.broadcast.queue";

    /**
     * WebSocket 广播路由键
     */
    String WEBSOCKET_BROADCAST_ROUTING_KEY = "websocket.broadcast";

    /**
     * 聊天消息推送队列
     */
    String CHAT_MESSAGE_PUSH_QUEUE = "chat.message.push.queue";

    /**
     * 聊天消息推送路由键
     */
    String CHAT_MESSAGE_PUSH_ROUTING_KEY = "chat.message.push";

    /**
     * WebSocket 死信交换机
     */
    String WEBSOCKET_DLX_EXCHANGE = "websocket.dlx.exchange";

    /**
     * WebSocket 死信队列
     */
    String WEBSOCKET_DLX_QUEUE = "websocket.dlx.queue";

    /**
     * WebSocket 死信路由键
     */
    String WEBSOCKET_DLX_ROUTING_KEY = "websocket.dlx";


    // ==================== Notification 相关 ====================

    /**
     * 通知交换机
     */
    String NOTIFICATION_EXCHANGE = "notification.exchange";

    /**
     * 通知队列
     */
    String NOTIFICATION_QUEUE = "notification.queue";

    /**
     * 通知路由键
     */
    String NOTIFICATION_ROUTING_KEY = "notification.create";

    /**
     * 通知死信交换机
     */
    String NOTIFICATION_DLX_EXCHANGE = "notification.dlx.exchange";

    /**
     * 通知死信队列
     */
    String NOTIFICATION_DLX_QUEUE = "notification.dlx.queue";

    /**
     * 通知死信路由键
     */
    String NOTIFICATION_DLX_ROUTING_KEY = "notification.dlx";


    /**
     * 关注事件队列
     */
    String FOLLOW_EVENT_QUEUE = "notification.follow.queue";

    /**
     * 关注事件路由键
     */
    String FOLLOW_EVENT_ROUTING_KEY = "event.follow.create";



    // ==================== AI 相关 ====================

    /**
     * AI 对话记录交换机
     */
    String AI_CHAT_RECORD_EXCHANGE = "ai.chat.record.exchange";

    /**
     * AI 对话记录队列
     */
    String AI_CHAT_RECORD_QUEUE = "ai.chat.record.queue";

    /**
     * AI 对话记录路由键
     */
    String AI_CHAT_RECORD_ROUTING_KEY = "ai.chat.record.create";
}
