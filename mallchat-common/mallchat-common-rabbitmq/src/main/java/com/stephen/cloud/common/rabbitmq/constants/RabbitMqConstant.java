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
    String WEBSOCKET_EXCHANGE = "mallchat.websocket.exchange";

    /**
     * WebSocket 推送队列
     */
    String WEBSOCKET_PUSH_QUEUE = "mallchat.websocket.push.queue";

    /**
     * WebSocket 推送路由键
     */
    String WEBSOCKET_PUSH_ROUTING_KEY = "mallchat.websocket.push";

    /**
     * WebSocket 广播队列
     */
    String WEBSOCKET_BROADCAST_QUEUE = "mallchat.websocket.broadcast.queue";

    /**
     * WebSocket 广播路由键
     */
    String WEBSOCKET_BROADCAST_ROUTING_KEY = "mallchat.websocket.broadcast";

    /**
     * 聊天消息推送队列
     */
    String CHAT_MESSAGE_PUSH_QUEUE = "mallchat.chat.message.push.queue";

    /**
     * 聊天消息推送路由键
     */
    String CHAT_MESSAGE_PUSH_ROUTING_KEY = "mallchat.chat.message.push";

    /**
     * WebSocket 死信交换机
     */
    String WEBSOCKET_DLX_EXCHANGE = "mallchat.websocket.dlx.exchange";

    /**
     * WebSocket 死信队列
     */
    String WEBSOCKET_DLX_QUEUE = "mallchat.websocket.dlx.queue";

    /**
     * WebSocket 死信路由键
     */
    String WEBSOCKET_DLX_ROUTING_KEY = "mallchat.websocket.dlx";


    // ==================== Notification 相关 ====================

    /**
     * 通知交换机
     */
    String NOTIFICATION_EXCHANGE = "mallchat.notification.exchange";

    /**
     * 通知队列
     */
    String NOTIFICATION_QUEUE = "mallchat.notification.queue";

    /**
     * 通知路由键
     */
    String NOTIFICATION_ROUTING_KEY = "mallchat.notification.create";

    /**
     * 通知死信交换机
     */
    String NOTIFICATION_DLX_EXCHANGE = "mallchat.notification.dlx.exchange";

    /**
     * 通知死信队列
     */
    String NOTIFICATION_DLX_QUEUE = "mallchat.notification.dlx.queue";

    /**
     * 通知死信路由键
     */
    String NOTIFICATION_DLX_ROUTING_KEY = "mallchat.notification.dlx";


    /**
     * 关注事件队列
     */
    String FOLLOW_EVENT_QUEUE = "mallchat.notification.follow.queue";

    /**
     * 关注事件路由键
     */
    String FOLLOW_EVENT_ROUTING_KEY = "mallchat.event.follow.create";



    // ==================== AI 相关 ====================

    /**
     * AI 对话记录交换机
     */
    String AI_CHAT_RECORD_EXCHANGE = "mallchat.ai.chat.record.exchange";

    /**
     * AI 对话记录队列
     */
    String AI_CHAT_RECORD_QUEUE = "mallchat.ai.chat.record.queue";

    /**
     * AI 对话记录路由键
     */
    String AI_CHAT_RECORD_ROUTING_KEY = "mallchat.ai.chat.record.create";
}
