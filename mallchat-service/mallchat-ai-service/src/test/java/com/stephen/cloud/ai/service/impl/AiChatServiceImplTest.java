package com.stephen.cloud.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.stephen.cloud.ai.factory.AiClientFactory;
import com.stephen.cloud.ai.model.entity.AiChatRecord;
import com.stephen.cloud.ai.service.AiChatRecordService;
import com.stephen.cloud.api.ai.model.dto.AiChatRequest;
import com.stephen.cloud.common.constants.SecurityConstant;
import com.stephen.cloud.common.exception.BusinessException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

class AiChatServiceImplTest {

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), AiChatRecord.class);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void chatMemoryShouldFilterByCurrentUserAndSessionId() {
        AiChatRecordService recordService = Mockito.mock(AiChatRecordService.class);
        Mockito.when(recordService.list(Mockito.any(LambdaQueryWrapper.class))).thenReturn(List.of());

        AiChatServiceImpl chatService = new AiChatServiceImpl();
        ReflectionTestUtils.setField(chatService, "aiChatRecordService", recordService);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SecurityConstant.USER_ID_HEADER, "1001");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        AiChatRequest chatRequest = AiChatRequest.builder()
                .message("hello")
                .sessionId("session-a")
                .build();
        ReflectionTestUtils.invokeMethod(chatService, "getChatMemory", chatRequest);

        ArgumentCaptor<LambdaQueryWrapper<AiChatRecord>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        Mockito.verify(recordService).list(captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();
        Assertions.assertTrue(sqlSegment.contains("session_id"), sqlSegment);
        Assertions.assertTrue(sqlSegment.contains("user_id"), sqlSegment);
    }

    @Test
    void chatMemoryShouldRejectMissingUserWhenSessionIdProvided() {
        AiChatServiceImpl chatService = new AiChatServiceImpl();
        ReflectionTestUtils.setField(chatService, "aiChatRecordService", Mockito.mock(AiChatRecordService.class));

        AiChatRequest chatRequest = AiChatRequest.builder()
                .message("hello")
                .sessionId("session-a")
                .build();

        Assertions.assertThrows(BusinessException.class,
                () -> ReflectionTestUtils.invokeMethod(chatService, "getChatMemory", chatRequest));
    }

    @Test
    void aiClientFactoryShouldRejectUnknownModelType() {
        AiClientFactory factory = new AiClientFactory();
        AiChatRequest request = AiChatRequest.builder()
                .message("hello")
                .modelType("unknown-model")
                .build();

        Assertions.assertThrows(BusinessException.class, () -> factory.getChatModel(request));
    }
}
