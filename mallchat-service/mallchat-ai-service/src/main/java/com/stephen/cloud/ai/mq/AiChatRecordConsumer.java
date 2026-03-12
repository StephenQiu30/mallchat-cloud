package com.stephen.cloud.ai.mq;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.rabbitmq.client.Channel;
import com.stephen.cloud.ai.model.entity.AiChatRecord;
import com.stephen.cloud.ai.service.AiChatRecordService;
import com.stephen.cloud.api.ai.model.dto.AiChatRecordDTO;
import com.stephen.cloud.common.rabbitmq.constants.RabbitMqConstant;
import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * AI 对话记录消息队列消费者
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class AiChatRecordConsumer {

    @Resource
    private AiChatRecordService aiChatRecordService;

    /**
     * 监听 AI 对话记录队列，执行数据库持久化
     *
     * @param rabbitMessage RabbitMessage 对象
     * @param channel       RabbitMQ 通道
     * @param msg           Spring AMQP 消息对象
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = RabbitMqConstant.AI_CHAT_RECORD_QUEUE, durable = "true"),
            exchange = @Exchange(value = RabbitMqConstant.AI_CHAT_RECORD_EXCHANGE, type = "direct"),
            key = RabbitMqConstant.AI_CHAT_RECORD_ROUTING_KEY
    ), ackMode = "MANUAL")
    public void handleAiChatRecord(RabbitMessage rabbitMessage, Channel channel, Message msg) throws IOException {
        long deliveryTag = msg.getMessageProperties().getDeliveryTag();

        if (rabbitMessage == null || rabbitMessage.getMsgId() == null) {
            log.error("[AiChatRecordConsumer] 消息为空或缺少msgId，拒绝消费");
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        String msgId = rabbitMessage.getMsgId();
        try {
            AiChatRecordDTO aiChatRecordDTO = JSONUtil.toBean(rabbitMessage.getMsgText(), AiChatRecordDTO.class);
            if (aiChatRecordDTO == null) {
                log.error("[AiChatRecordConsumer] 消息解析失败, msgId: {}", msgId);
                channel.basicNack(deliveryTag, false, false);
                return;
            }

            log.info("[AiChatRecordConsumer] 收到持久化请求, userId: {}, sessionId: {}",
                    aiChatRecordDTO.getUserId(), aiChatRecordDTO.getSessionId());

            // 映射为实体对象进行持久化
            AiChatRecord aiChatRecord = new AiChatRecord();
            BeanUtil.copyProperties(aiChatRecordDTO, aiChatRecord);

            // 执行持久化
            aiChatRecordService.save(aiChatRecord);

            // 手动确认消息
            channel.basicAck(deliveryTag, false);
            log.info("[AiChatRecordConsumer] 消息处理成功, msgId: {}, recordId: {}", msgId, aiChatRecord.getId());
        } catch (Exception e) {
            log.error("[AiChatRecordConsumer] 处理消息失败, msgId: {}", msgId, e);
            // 允许重试
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
