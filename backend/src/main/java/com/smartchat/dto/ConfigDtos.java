package com.smartchat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** API 配置 DTO */
public final class ConfigDtos {

    private ConfigDtos() {
    }

    /** 保存用户 AI 配置 */
    public record ApiConfigRequest(
            @NotBlank(message = "provider 不能为空")
            @Pattern(regexp = "^(openai|anthropic)$", message = "provider 仅支持 openai / anthropic")
            String provider,

            @NotBlank(message = "接口地址不能为空")
            @Pattern(regexp = "^https?://.+", message = "接口地址需以 http(s):// 开头")
            String baseUrl,

            @NotBlank(message = "API Key 不能为空") String apiKey,

            @NotBlank(message = "模型名称不能为空") String model
    ) {
    }

    /** 用户 AI 配置（返回给前端，API Key 打码显示） */
    public record ApiConfigInfo(
            String provider, String baseUrl, String apiKeyMasked, String model
    ) {
    }
}
