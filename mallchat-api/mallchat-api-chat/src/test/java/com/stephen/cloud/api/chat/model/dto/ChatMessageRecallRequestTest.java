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
 * 撤回消息请求校验测试
 *
 * @author StephenQiu30
 */
class ChatMessageRecallRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void shouldPassValidationWithValidData() {
        ChatMessageRecallRequest request = new ChatMessageRecallRequest();
        request.setMessageId(1L);

        Set<ConstraintViolation<ChatMessageRecallRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "有效数据不应有校验错误");
    }

    @Test
    void shouldFailWhenMessageIdIsNull() {
        ChatMessageRecallRequest request = new ChatMessageRecallRequest();

        Set<ConstraintViolation<ChatMessageRecallRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("messageId")));
    }
}
