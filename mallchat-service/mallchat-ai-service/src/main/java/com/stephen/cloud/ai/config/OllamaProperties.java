package com.stephen.cloud.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Ollama 配置属性
 *
 * @author StephenQiu30
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.ollama")
public class OllamaProperties {

    /**
     * 服务地址
     */
    private String baseUrl = "http://localhost:11434";

    /**
     * 模型名称 (例如: llama3)
     */
    private String modelName = "llama3";

    /**
     * 温度 (0.0 ~ 2.0, 越大越随机)
     */
    private Double temperature = 0.7;

    /**
     * Top P (0.0 ~ 1.0)
     */
    private Double topP = 1.0;

}
