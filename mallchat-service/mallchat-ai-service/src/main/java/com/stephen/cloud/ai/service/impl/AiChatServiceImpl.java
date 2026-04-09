package com.stephen.cloud.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stephen.cloud.ai.factory.AiClientFactory;
import com.stephen.cloud.ai.model.entity.AiChatRecord;
import com.stephen.cloud.ai.service.AiAssistant;
import com.stephen.cloud.ai.service.AiChatRecordService;
import com.stephen.cloud.ai.service.AiChatService;
import com.stephen.cloud.api.ai.model.dto.AiChatRecordDTO;
import com.stephen.cloud.api.ai.model.dto.AiChatRequest;
import com.stephen.cloud.api.ai.model.vo.AiChatResponse;
import com.stephen.cloud.common.auth.utils.SecurityUtils;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.producer.RabbitMqSender;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * AI 对话服务实现类
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class AiChatServiceImpl implements AiChatService {

    @Resource
    private AiClientFactory aiClientFactory;

    @Resource
    private AiChatRecordService aiChatRecordService;


    @Resource
    private RabbitMqSender mqSender;

    /**
     * AI 标准对话
     *
     * @param aiChatRequest 对话请求
     * @param request       HTTP 请求
     * @return 对话结果
     */
    @Override
    public AiChatResponse chat(AiChatRequest aiChatRequest, HttpServletRequest request) {
        log.info("执行 AI 标准对话: modelType={}, message={}", aiChatRequest.getModelType(), aiChatRequest.getMessage());
        ThrowUtils.throwIf(aiChatRequest == null, ErrorCode.PARAMS_ERROR);
        String message = aiChatRequest.getMessage();
        ThrowUtils.throwIf(StringUtils.isBlank(message), ErrorCode.PARAMS_ERROR, "消息内容不能为空");

        // 1. 获取对应的 AI 模型
        ChatLanguageModel chatModel = aiClientFactory.getChatModel(aiChatRequest);

        // 2. 构建 AiAssistant (声明式 AI 服务)
        AiAssistant assistant = AiServices.builder(AiAssistant.class)
                .chatLanguageModel(chatModel)
                .systemMessageProvider(chatSessionId -> aiChatRequest.getSystemMessage())
                .chatMemory(getChatMemory(aiChatRequest))
                .build();

        // 3. 执行对话
        Result<String> result = assistant.chat(message);
        String responseText = result.content();
        TokenUsage usage = result.tokenUsage();

        // 异步持久化对话记录
        saveChatRecordAsync(aiChatRequest, message, responseText, usage);

        return AiChatResponse.builder()
                .content(responseText)
                .totalTokens(usage != null ? usage.totalTokenCount() : null)
                .promptTokens(usage != null ? usage.inputTokenCount() : null)
                .completionTokens(usage != null ? usage.outputTokenCount() : null)
                .build();
    }

    /**
     * AI 流式对话 (SSE)
     *
     * @param aiChatRequest 对话请求
     * @param emitter       SSE 发射器
     * @param request       HTTP 请求
     */
    @Override
    public void streamChat(AiChatRequest aiChatRequest, SseEmitter emitter, HttpServletRequest request) {
        log.info("执行 AI 流式对话: modelType={}, message={}", aiChatRequest.getModelType(), aiChatRequest.getMessage());
        ThrowUtils.throwIf(aiChatRequest == null, ErrorCode.PARAMS_ERROR);
        String message = aiChatRequest.getMessage();
        ThrowUtils.throwIf(StringUtils.isBlank(message), ErrorCode.PARAMS_ERROR, "消息内容不能为空");

        StreamingChatLanguageModel streamingModel = aiClientFactory.getStreamingChatModel(aiChatRequest);

        // 构建流式 AiAssistant
        AiAssistant assistant = AiServices.builder(AiAssistant.class)
                .streamingChatLanguageModel(streamingModel)
                .systemMessageProvider(chatSessionId -> aiChatRequest.getSystemMessage())
                .chatMemory(getChatMemory(aiChatRequest))
                .build();

        StringBuilder fullResponse = new StringBuilder();
        assistant.streamChat(message)
                .onNext(token -> {
                    try {
                        fullResponse.append(token);
                        // 推送增量内容到前端
                        emitter.send(SseEmitter.event().data(token));
                    } catch (IOException e) {
                        log.error("SSE 数据推送失败", e);
                        emitter.completeWithError(e);
                    }
                })
                .onComplete(response -> {
                    log.info("AI 流式生成完成");
                    // 异步保存完整记录
                    saveChatRecordAsync(aiChatRequest, message, fullResponse.toString(), response.tokenUsage());
                    emitter.complete();
                })
                .onError(error -> {
                    log.error("AI 流式生成异常", error);
                    try {
                        String errorJson = String.format("{\"code\": 500, \"message\": \"AI 生成失败: %s\"}",
                                error.getMessage());
                        emitter.send(SseEmitter.event().name("error").data(errorJson));
                    } catch (IOException e) {
                        log.error("SSE 异常通知发送失败", e);
                    }
                    emitter.completeWithError(error);
                })
                .start();
    }

    /**
     * 获取 ChatMemory 并加载历史记录
     */
    private ChatMemory getChatMemory(AiChatRequest aiChatRequest) {
        String sessionId = aiChatRequest.getSessionId();
        MessageWindowChatMemory memory = MessageWindowChatMemory.withMaxMessages(20);

        if (StringUtils.isNotBlank(sessionId)) {
            List<AiChatRecord> history = aiChatRecordService.list(
                    new LambdaQueryWrapper<AiChatRecord>()
                            .select(AiChatRecord::getMessage, AiChatRecord::getResponse)
                            .eq(AiChatRecord::getSessionId, sessionId)
                            .orderByDesc(AiChatRecord::getCreateTime)
                            .last("limit 20"));
            Collections.reverse(history);
            for (AiChatRecord record : history) {
                memory.add(UserMessage.from(record.getMessage()));
                memory.add(AiMessage.from(record.getResponse()));
            }
        }
        return memory;
    }

    /**
     * 异步保存对话记录 (通过 MQ)
     */
    private void saveChatRecordAsync(AiChatRequest aiChatRequest, String message, String response, TokenUsage usage) {
        try {
            Long userId = SecurityUtils.getLoginUserIdPermitNull();
            AiChatRecordDTO recordDTO = AiChatRecordDTO.builder()
                    .userId(userId)
                    .sessionId(aiChatRequest.getSessionId())
                    .message(message)
                    .response(response)
                    .modelType(aiChatRequest.getModelType())
                    .totalTokens(usage != null ? usage.totalTokenCount() : null)
                    .promptTokens(usage != null ? usage.inputTokenCount() : null)
                    .completionTokens(usage != null ? usage.outputTokenCount() : null)
                    .build();

            String bizId = "ai_chat:" + System.currentTimeMillis();
            mqSender.send(MqBizTypeEnum.AI_CHAT_RECORD, bizId, recordDTO);
        } catch (Exception e) {
            log.error("异步同步 AI 对话记录到 MQ 失败", e);
        }
    }
}
