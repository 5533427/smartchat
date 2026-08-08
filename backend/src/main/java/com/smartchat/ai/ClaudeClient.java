package com.smartchat.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.RawMessageStreamEvent;

import java.io.IOException;
import java.util.Iterator;
import java.util.function.Consumer;

/**
 * Anthropic Claude 客户端（官方 Java SDK）。
 * <p>
 * 接口：{@code POST {baseUrl}/v1/messages}，SSE 流式事件：
 * - content_block_delta：增量文本
 * - message_start / message_delta：token 用量
 * <p>
 * 注意：Claude Opus 4.7/4.8 不接受 temperature / top_p 等采样参数（会返回 400），
 * 因此本客户端不发送任何采样参数，通过提示词控制风格。
 */
public class ClaudeClient implements AiClient {

    private final AnthropicClient sdkClient;

    public ClaudeClient(String baseUrl, String apiKey) {
        this.sdkClient = AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public String provider() {
        return "anthropic";
    }

    @Override
    public void stream(AiRequest request, AiStreamHandle handle,
                       Consumer<String> onDelta, Consumer<AiRequest.AiUsage> onUsage) throws IOException {
        MessageCreateParams.Builder paramsBuilder = MessageCreateParams.builder()
                .model(request.model())
                .maxTokens(request.maxTokens());
        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            paramsBuilder.system(request.systemPrompt());
        }
        for (AiRequest.AiMessage m : request.messages()) {
            if ("assistant".equals(m.role())) {
                paramsBuilder.addAssistantMessage(m.content());
            } else {
                paramsBuilder.addUserMessage(m.content());
            }
        }
        MessageCreateParams params = paramsBuilder.build();

        int[] promptTokens = {0};
        int[] completionTokens = {0};
        try (StreamResponse<RawMessageStreamEvent> streamResponse = sdkClient.messages().createStreaming(params)) {
            Iterator<RawMessageStreamEvent> events = streamResponse.stream().iterator();
            while (events.hasNext()) {
                if (handle.isCancelled()) {
                    break;
                }
                RawMessageStreamEvent event = events.next();
                // 增量文本：content_block_delta → text_delta
                event.contentBlockDelta().ifPresent(deltaEvent ->
                        deltaEvent.delta().text().ifPresent(textDelta -> {
                            String text = textDelta.text();
                            if (text != null && !text.isEmpty()) {
                                onDelta.accept(text);
                            }
                        }));
                // 用量：message_start 携带 input_tokens
                event.messageStart().ifPresent(start -> {
                    if (start.message().usage() != null) {
                        promptTokens[0] = Math.toIntExact(start.message().usage().inputTokens());
                    }
                });
                // message_delta 携带 output_tokens
                event.messageDelta().ifPresent(deltaEvent -> {
                    if (deltaEvent.usage() != null) {
                        completionTokens[0] = Math.toIntExact(deltaEvent.usage().outputTokens());
                    }
                });
            }
        } catch (RuntimeException e) {
            if (handle.isCancelled()) {
                return;
            }
            throw new IOException("Claude 接口调用失败：" + e.getMessage(), e);
        }
        if (onUsage != null && promptTokens[0] + completionTokens[0] > 0) {
            onUsage.accept(new AiRequest.AiUsage(promptTokens[0], completionTokens[0]));
        }
    }
}
