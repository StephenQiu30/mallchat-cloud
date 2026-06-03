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
 * 转发消息请求校验测试
 *
 * @author StephenQiu30
 */
class ChatMessageForwardRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void shouldPassValidationWithValidData() {
        ChatMessageForwardRequest request = new ChatMessageForwardRequest();
        request.setSourceMessageId(100L);
        request.setTargetRoomId(2L);
        request.setClientMsgId("pc-1710000000000-forward-1");

        Set<ConstraintViolation<ChatMessageForwardRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "有效数据不应有校验错误");
    }

    @Test
    void shouldFailWhenSourceMessageIdIsNull() {
        ChatMessageForwardRequest request = new ChatMessageForwardRequest();
        request.setTargetRoomId(2L);
        request.setClientMsgId("pc-1710000000000-forward-1");

        Set<ConstraintViolation<ChatMessageForwardRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("sourceMessageId")));
    }

    @Test
    void shouldFailWhenTargetRoomIdIsNull() {
        ChatMessageForwardRequest request = new ChatMessageForwardRequest();
        request.setSourceMessageId(100L);
        request.setClientMsgId("pc-1710000000000-forward-1");

        Set<ConstraintViolation<ChatMessageForwardRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("targetRoomId")));
    }

    @Test
    void shouldFailWhenClientMsgIdIsNull() {
        ChatMessageForwardRequest request = new ChatMessageForwardRequest();
        request.setSourceMessageId(100L);
        request.setTargetRoomId(2L);

        Set<ConstraintViolation<ChatMessageForwardRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("clientMsgId")));
    }
}
