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
 * 发送消息请求校验测试
 *
 * @author StephenQiu30
 */
class ChatMessageSendRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void shouldPassValidationWithValidData() {
        ChatMessageSendRequest request = new ChatMessageSendRequest();
        request.setRoomId(1L);
        request.setClientMsgId("pc-1710000000000-1");
        request.setContent("Hello");
        request.setType(1);

        Set<ConstraintViolation<ChatMessageSendRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "有效数据不应有校验错误");
    }

    @Test
    void shouldFailWhenRoomIdIsNull() {
        ChatMessageSendRequest request = new ChatMessageSendRequest();
        request.setClientMsgId("pc-1710000000000-1");
        request.setType(1);

        Set<ConstraintViolation<ChatMessageSendRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("roomId")));
    }

    @Test
    void shouldFailWhenClientMsgIdIsNull() {
        ChatMessageSendRequest request = new ChatMessageSendRequest();
        request.setRoomId(1L);
        request.setType(1);

        Set<ConstraintViolation<ChatMessageSendRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("clientMsgId")));
    }

    @Test
    void shouldFailWhenTypeIsNull() {
        ChatMessageSendRequest request = new ChatMessageSendRequest();
        request.setRoomId(1L);
        request.setClientMsgId("pc-1710000000000-1");

        Set<ConstraintViolation<ChatMessageSendRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("type")));
    }

    @Test
    void shouldAllowNullContentForNonTextMessage() {
        ChatMessageSendRequest request = new ChatMessageSendRequest();
        request.setRoomId(1L);
        request.setClientMsgId("pc-1710000000000-1");
        request.setType(2); // IMAGE
        request.setExtra("{\"url\":\"https://example.com/img.png\"}");

        Set<ConstraintViolation<ChatMessageSendRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "非文本消息 content 可为空");
    }

    @Test
    void shouldSupportReplyMsgId() {
        ChatMessageSendRequest request = new ChatMessageSendRequest();
        request.setRoomId(1L);
        request.setClientMsgId("pc-1710000000000-1");
        request.setContent("reply content");
        request.setType(1);
        request.setReplyMsgId(100L);

        Set<ConstraintViolation<ChatMessageSendRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
        assertEquals(100L, request.getReplyMsgId());
    }
}
