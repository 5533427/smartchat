package com.smartchat.ai;

import java.util.List;

/**
 * 与具体模型厂商无关的 AI 请求模型。
 * <p>
 * {@link AiClient} 实现（OpenAI 兼容 / Anthropic）负责把它翻译成各自的协议。
 */
public record AiRequest(
        String model,
        /** 系统提示词，可为 null */
        String systemPrompt,
        /** 对话消息（role 仅为 user / assistant，已按顺序交替） */
        List<AiMessage> messages,
        /** 最大输出 token 数 */
        int maxTokens
) {

    /** 对话消息 */
    public record AiMessage(String role, String content) {
    }

    /** token 用量（厂商未返回时按字符估算） */
    public record AiUsage(int promptTokens, int completionTokens) {

        public int total() {
            return promptTokens + completionTokens;
        }
    }
}
