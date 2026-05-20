package com.stephen.cloud.chat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.EnableAsync;

class ChatServiceApplicationTest {

    @Test
    void shouldEnableAsyncOperationLogRecorder() {
        Assertions.assertTrue(ChatServiceApplication.class.isAnnotationPresent(EnableAsync.class));
    }
}
