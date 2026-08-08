package com.smartchat.ai;

import com.smartchat.common.BusinessException;
import com.smartchat.entity.ApiConfig;

/**
 * AI 客户端工厂：根据配置（provider / baseUrl / apiKey）创建对应客户端。
 */
public final class AiClientFactory {

    private AiClientFactory() {
    }

    /**
     * 创建客户端。
     *
     * @param config 用户个人配置；若 baseUrl/apiKey 为空则使用系统默认（环境变量）
     */
    public static AiClient create(String provider, String baseUrl, String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException("未配置 AI API Key，请到「设置」中填写，或通过环境变量 AI_API_KEY 配置");
        }
        if (ApiConfig.PROVIDER_ANTHROPIC.equals(provider)) {
            return new ClaudeClient(baseUrl, apiKey);
        }
        if (ApiConfig.PROVIDER_OPENAI.equals(provider)) {
            return new OpenAiClient(baseUrl, apiKey);
        }
        throw new BusinessException("不支持的 AI 厂商: " + provider);
    }
}
