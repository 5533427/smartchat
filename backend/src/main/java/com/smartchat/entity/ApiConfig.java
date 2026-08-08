package com.smartchat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 用户个人 AI 配置（每个用户一条，可为空 = 使用系统默认配置）
 * <p>
 * 支持的 provider：
 * - openai    OpenAI 兼容协议（DeepSeek / OpenAI / Kimi / 通义千问 等，接口均为 /chat/completions）
 * - anthropic Anthropic Claude（/v1/messages）
 */
@Entity
@Table(name = "api_configs")
public class ApiConfig {

    public static final String PROVIDER_OPENAI = "openai";
    public static final String PROVIDER_ANTHROPIC = "anthropic";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, length = 20)
    private String provider = PROVIDER_OPENAI;

    /** 上游接口地址，如 https://api.deepseek.com 或 https://api.anthropic.com */
    @Column(length = 200)
    private String baseUrl;

    @Column(length = 200)
    private String apiKey;

    /** 模型名，如 deepseek-chat / gpt-4o-mini / claude-sonnet-4-6 */
    @Column(length = 100)
    private String model;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
