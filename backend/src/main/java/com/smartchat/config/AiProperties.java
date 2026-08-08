package com.smartchat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * smartchat.ai.* 配置项（application.yml）
 */
@Component
@ConfigurationProperties(prefix = "smartchat.ai")
public class AiProperties {

    /** 默认厂商：openai / anthropic */
    private String provider = "openai";

    /** 默认接口地址（OpenAI 兼容协议） */
    private String baseUrl = "https://api.deepseek.com";

    /** 默认 API Key（生产建议用环境变量 AI_API_KEY 注入） */
    private String apiKey = "";

    /** 默认模型（为空时按厂商自动选择：openai→deepseek-chat，anthropic→claude-opus-4-8） */
    private String model = "";

    /** 携带的对话上下文最大条数 */
    private int maxContextMessages = 20;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getMaxContextMessages() {
        return maxContextMessages;
    }

    public void setMaxContextMessages(int maxContextMessages) {
        this.maxContextMessages = maxContextMessages;
    }
}
