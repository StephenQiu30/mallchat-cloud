package com.stephen.cloud.mail.mq.handler;

import com.stephen.cloud.api.log.model.dto.email.EmailRecordAddRequest;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqDedupeLock;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqHandler;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.EmailMessage;
import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import com.stephen.cloud.mail.service.MailService;
import jakarta.annotation.Resource;
import jakarta.mail.AuthenticationFailedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 邮件发送处理器
 * <p>
 * 遵循策略模式与 MVP 原则，负责 {@link MqBizTypeEnum#EMAIL_SEND} 类型的相关业务执行。
 * 融合 {@link RabbitMqDedupeLock} 声明式解决重复发送问题。
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
@RabbitMqDedupeLock(prefix = "mq:email:send", expire = 86400)
public class EmailSendHandler implements RabbitMqHandler<EmailMessage> {

    @Resource
    private MailService mailService;

    /**
     * 不可重试异常的关键词（配置错误，重试无意义）
     */
    private static final String[] NON_RETRYABLE_KEYWORDS = {
            "Authentication failed", "authentication failed", "535 Error"
    };

    @Override
    public String getBizType() {
        return MqBizTypeEnum.EMAIL_SEND.getValue();
    }

    @Override
    public void onMessage(EmailMessage emailMessage, RabbitMessage rabbitMessage) throws Exception {
        String msgId = rabbitMessage.getMsgId();

        if (emailMessage.getTo() == null) {
            log.error("[EmailSendHandler] 邮件内容缺少收件人, msgId: {}", msgId);
            throw new IllegalArgumentException("邮件缺少收件人");
        }

        log.info("[EmailSendHandler] 准备发送邮件, to: {}, subject: {}, msgId: {}",
                emailMessage.getTo(), emailMessage.getSubject(), msgId);

        try {
            // 同步发送邮件
            mailService.sendMailSync(emailMessage);

            // 记录成功日志
            mailService.recordEmail(EmailRecordAddRequest.builder()
                    .msgId(msgId)
                    .toEmail(emailMessage.getTo())
                    .subject(emailMessage.getSubject())
                    .content(emailMessage.getContent())
                    .isHtml(Boolean.TRUE.equals(emailMessage.getIsHtml()) ? 1 : 0)
                    .status("SUCCESS")
                    .bizType(emailMessage.getBizType())
                    .bizId(emailMessage.getBizId())
                    .provider("MQ")
                    .sendTime(new Date())
                    .build());

        } catch (Exception e) {
            log.error("[EmailSendHandler] 邮件发送处理异常, msgId: {}", msgId, e);
            String errorMsg = getRootCauseMessage(e);

            mailService.recordEmail(EmailRecordAddRequest.builder()
                    .msgId(msgId)
                    .toEmail(emailMessage.getTo())
                    .subject(emailMessage.getSubject())
                    .content(emailMessage.getContent())
                    .isHtml(Boolean.TRUE.equals(emailMessage.getIsHtml()) ? 1 : 0)
                    .status("FAILED")
                    .errorMessage(errorMsg)
                    .bizType(emailMessage.getBizType())
                    .bizId(emailMessage.getBizId())
                    .provider("MQ")
                    .sendTime(new Date())
                    .build());

            // 无论是否可以重试，都由 Dispatcher 介入进行处理并 NACK 到 DLX
            if (isNonRetryableException(e)) {
                log.error("[EmailSendHandler] 遭遇不可重试致命异常，抛出退出");
            }
            throw e;
        }
    }

    @Override
    public Class<EmailMessage> getDataType() {
        return EmailMessage.class;
    }

    /**
     * 判断是否为不可重试的异常
     */
    private boolean isNonRetryableException(Throwable e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof MailAuthenticationException || cause instanceof AuthenticationFailedException) {
                return true;
            }
            String msg = cause.getMessage();
            if (msg != null) {
                for (String keyword : NON_RETRYABLE_KEYWORDS) {
                    if (msg.contains(keyword)) {
                        return true;
                    }
                }
            }
            cause = cause.getCause();
        }
        return false;
    }

    /**
     * 获取最终原因消息
     */
    private String getRootCauseMessage(Throwable e) {
        if (e == null)
            return "unknown";
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
    }
}
