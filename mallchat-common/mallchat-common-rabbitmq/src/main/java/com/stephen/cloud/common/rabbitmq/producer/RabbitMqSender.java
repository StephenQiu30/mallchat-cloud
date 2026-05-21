package com.stephen.cloud.common.rabbitmq.producer;

import cn.hutool.json.JSONUtil;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

/**
 * RabbitMQ 统一发送门面（Producer / Facade）
 * <p>
 * 发送端 MVP：统一封装 {@link RabbitMessage} 元数据，并支持直接发送与事务提交后发送。
 * </p>
 */
@Slf4j
@Component
public class RabbitMqSender {

    @Resource
    private RabbitTemplate rabbitTemplateBean;

    @Resource
    private RabbitMqPublishObservation publishObservation;

    public void send(MqBizTypeEnum bizTypeEnum, String msgId, Object payload) {
        if (payload == null) {
            log.error("[RabbitMqSender] 发送被拒绝，因业务载体 (Payload) 为 null。业务分类: {}", bizTypeEnum.getValue());
            publishObservation.recordPublish(bizTypeEnum.getValue(), msgId, "rejected");
            return;
        }

        String finalMsgId = msgId != null ? msgId : UUID.randomUUID().toString();
        try {
            RabbitMessage rabbitMessage = RabbitMessage.builder()
                    .msgId(finalMsgId)
                    .bizType(bizTypeEnum.getValue())
                    .msgText(JSONUtil.toJsonStr(payload))
                    .build();

            rabbitTemplateBean.convertAndSend(bizTypeEnum.getExchange(), bizTypeEnum.getRoutingKey(), rabbitMessage,
                    message -> {
                        message.getMessageProperties().setHeader("bizType", bizTypeEnum.getValue());
                        message.getMessageProperties().setHeader("bizId", finalMsgId);
                        return message;
                    }, new CorrelationData(buildCorrelationId(bizTypeEnum, finalMsgId)));

            publishObservation.recordPublish(bizTypeEnum.getValue(), finalMsgId, "accepted");
            log.info("[RabbitMqSender - 直接发送成功] Exchange={}, Route={}, BizType={}, MsgId={}",
                    bizTypeEnum.getExchange(), bizTypeEnum.getRoutingKey(), bizTypeEnum.getValue(),
                    rabbitMessage.getMsgId());
        } catch (Exception e) {
            publishObservation.recordPublish(bizTypeEnum.getValue(), finalMsgId, "failed");
            log.error("[RabbitMqSender - 网络投递异常] 业务类型: {}, 消息编号: {}", bizTypeEnum.getValue(), msgId, e);
            throw e;
        }
    }

    public void send(MqBizTypeEnum bizTypeEnum, Object payload) {
        send(bizTypeEnum, null, payload);
    }

    public void sendTransactional(MqBizTypeEnum bizTypeEnum, String msgId, Object payload) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(bizTypeEnum, msgId, payload);
                }
            });
            log.debug("[RabbitMqSender] sendTransactional 已注册事务提交后发送: BizType={}, MsgId={}",
                    bizTypeEnum.getValue(), msgId);
            return;
        }
        log.debug("[RabbitMqSender] 当前无线程事务同步，按直接发送处理: BizType={}, MsgId={}",
                bizTypeEnum.getValue(), msgId);
        send(bizTypeEnum, msgId, payload);
    }

    public void sendTransactional(MqBizTypeEnum bizTypeEnum, Object payload) {
        sendTransactional(bizTypeEnum, null, payload);
    }

    private String buildCorrelationId(MqBizTypeEnum bizTypeEnum, String msgId) {
        return bizTypeEnum.getValue() + ":" + msgId;
    }
}
