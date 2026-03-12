package com.stephen.cloud.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * DashScope 配置属性
 *
 * @author StephenQiu30
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.dashscope")
public class DashScopeProperties {

    /**
     * API Key
     */
    private String apiKey;

    /**
     * 模型名称 (例如: qwen-plus)
     */
    private String modelName = "qwen-plus";

    /**
     * 温度 (0.0 ~ 2.0, 越大越随机)
     */
    private Double temperature = 0.7;

    /**
     * Top P (0.0 ~ 1.0)
     */
    private Double topP = 1.0;

    /**
     * 最大生成 Token 数
     */
    private Integer maxTokens = 2000;

}
