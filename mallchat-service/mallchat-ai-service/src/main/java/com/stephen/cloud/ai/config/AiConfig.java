package com.stephen.cloud.ai.config;

import dev.langchain4j.model.dashscope.QwenChatModel;
import dev.langchain4j.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * AI 模型配置类
 *
 * @author StephenQiu30
 */
@Configuration
public class AiConfig {

    @Resource
    private DashScopeProperties dashScopeProperties;

    @Resource
    private OllamaProperties ollamaProperties;

    /**
     * 通义千问同步模型客户端
     *
     * @return {@link QwenChatModel}
     */
    @Bean
    public QwenChatModel qwenChatModel() {
        return QwenChatModel.builder()
                .apiKey(dashScopeProperties.getApiKey())
                .modelName(dashScopeProperties.getModelName())
                .build();
    }

    /**
     * 通义千问流式模型客户端
     *
     * @return {@link QwenStreamingChatModel}
     */
    @Bean
    public QwenStreamingChatModel qwenStreamingChatModel() {
        return QwenStreamingChatModel.builder()
                .apiKey(dashScopeProperties.getApiKey())
                .modelName(dashScopeProperties.getModelName())
                .build();
    }

    /**
     * Ollama 同步模型客户端
     *
     * @return {@link OllamaChatModel}
     */
    @Bean
    public OllamaChatModel ollamaChatModel() {
        return OllamaChatModel.builder()
                .baseUrl(ollamaProperties.getBaseUrl())
                .modelName(ollamaProperties.getModelName())
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    /**
     * Ollama 流式模型客户端
     *
     * @return {@link OllamaStreamingChatModel}
     */
    @Bean
    public OllamaStreamingChatModel ollamaStreamingChatModel() {
        return OllamaStreamingChatModel.builder()
                .baseUrl(ollamaProperties.getBaseUrl())
                .modelName(ollamaProperties.getModelName())
                .timeout(Duration.ofSeconds(60))
                .build();
    }

}
