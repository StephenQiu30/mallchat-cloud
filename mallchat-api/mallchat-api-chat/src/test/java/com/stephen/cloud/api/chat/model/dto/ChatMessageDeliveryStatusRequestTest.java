package com.stephen.cloud.api.chat.model.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 消息投递状态查询请求校验测试
 *
 * @author StephenQiu30
 */
class ChatMessageDeliveryStatusRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void shouldPassValidationWithValidData() {
        ChatMessageDeliveryStatusRequest request = new ChatMessageDeliveryStatusRequest();
        request.setMessageId(100L);

        Set<ConstraintViolation<ChatMessageDeliveryStatusRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "有效数据不应有校验错误");
    }

    @Test
    void shouldFailWhenMessageIdIsNull() {
        ChatMessageDeliveryStatusRequest request = new ChatMessageDeliveryStatusRequest();

        Set<ConstraintViolation<ChatMessageDeliveryStatusRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("messageId")));
    }
}
