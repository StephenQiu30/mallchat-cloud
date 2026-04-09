package com.stephen.cloud.ai.factory;

import com.stephen.cloud.ai.config.DashScopeProperties;
import com.stephen.cloud.ai.config.OllamaProperties;
import com.stephen.cloud.api.ai.model.dto.AiChatRequest;
import com.stephen.cloud.api.ai.model.enums.AiModelTypeEnum;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.dashscope.QwenChatModel;
import dev.langchain4j.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * AI 客户端工厂类
 * <p>
 * 负责根据对话请求中的模型类型（如 DashScope, Ollama）和后台配置属性，
 * 动态构建并提供对应的 LangChain4j 对话模型实例。
 * </p>
 *
 * @author StephenQiu30
 */
@Component
public class AiClientFactory {

    @Resource
    private DashScopeProperties dashScopeProperties;

    @Resource
    private OllamaProperties ollamaProperties;

    /**
     * 根据请求动态构建标准对话模型实例
     */
    public ChatLanguageModel getChatModel(AiChatRequest request) {
        String modelType = request.getModelType();
        AiModelTypeEnum typeEnum = AiModelTypeEnum.getEnumByValue(modelType);
        // 默认兜底使用通义千问 (DashScope)
        if (typeEnum == null) {
            typeEnum = AiModelTypeEnum.DASHSCOPE;
        }

        return switch (typeEnum) {
            case DASHSCOPE -> QwenChatModel.builder()
                    .apiKey(dashScopeProperties.getApiKey())
                    .modelName(dashScopeProperties.getModelName())
                    .temperature(dashScopeProperties.getTemperature().floatValue())
                    .topP(dashScopeProperties.getTopP())
                    .maxTokens(dashScopeProperties.getMaxTokens())
                    .build();
            case OLLAMA -> OllamaChatModel.builder()
                    .baseUrl(ollamaProperties.getBaseUrl())
                    .modelName(ollamaProperties.getModelName())
                    .temperature(ollamaProperties.getTemperature())
                    .topP(ollamaProperties.getTopP())
                    .build();
        };
    }

    /**
     * 获取流式对话模型
     *
     * @param request 对话请求
     * @return 流式对话模型
     */
    public StreamingChatLanguageModel getStreamingChatModel(AiChatRequest request) {
        String modelType = request.getModelType();
        AiModelTypeEnum typeEnum = AiModelTypeEnum.getEnumByValue(modelType);
        if (typeEnum == null) {
            typeEnum = AiModelTypeEnum.DASHSCOPE;
        }

        return switch (typeEnum) {
            case DASHSCOPE -> QwenStreamingChatModel.builder()
                    .apiKey(dashScopeProperties.getApiKey())
                    .modelName(dashScopeProperties.getModelName())
                    .temperature(dashScopeProperties.getTemperature().floatValue())
                    .topP(dashScopeProperties.getTopP())
                    .maxTokens(dashScopeProperties.getMaxTokens())
                    .build();
            case OLLAMA -> OllamaStreamingChatModel.builder()
                    .baseUrl(ollamaProperties.getBaseUrl())
                    .modelName(ollamaProperties.getModelName())
                    .temperature(ollamaProperties.getTemperature())
                    .topP(ollamaProperties.getTopP())
                    .build();
        };
    }
}
