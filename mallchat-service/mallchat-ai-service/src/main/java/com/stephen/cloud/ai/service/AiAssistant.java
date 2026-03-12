package com.stephen.cloud.ai.service;

import dev.langchain4j.service.Result;
import dev.langchain4j.service.TokenStream;

/**
 * AI 助手接口
 * <p>
 * 基于 LangChain4j 的声明式 AI 服务接口，定义了 AI 的交互能力。
 * 通过 AiServices 动态生成代理实现，自动处理消息历史、工具调用和系统语境。
 * </p>
 *
 * @author StephenQiu30
 */
public interface AiAssistant {

    /**
     * 普通对话
     *
     * @param userMessage 用户消息
     * @return AI 回复 (包含元数据)
     */
    Result<String> chat(String userMessage);

    /**
     * 流式对话
     *
     * @param userMessage 用户消息
     * @return Token 流
     */
    TokenStream streamChat(String userMessage);
}
