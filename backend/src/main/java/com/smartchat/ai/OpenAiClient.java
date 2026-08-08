package com.smartchat.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * OpenAI 兼容协议客户端。
 * <p>
 * 兼容 DeepSeek / OpenAI / Kimi / 通义千问 / GLM 等所有实现
 * {@code POST {baseUrl}/chat/completions} 流式接口的厂商。
 */
public class OpenAiClient implements AiClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Duration TIMEOUT = Duration.ofMinutes(5);

    private final String baseUrl;
    private final String apiKey;
    private final HttpClient httpClient;

    public OpenAiClient(String baseUrl, String apiKey) {
        // 归一化：去掉末尾斜杠
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public String provider() {
        return "openai";
    }

    @Override
    public void stream(AiRequest request, AiStreamHandle handle,
                       Consumer<String> onDelta, Consumer<AiRequest.AiUsage> onUsage) throws IOException {
        String url = this.baseUrl + "/chat/completions";
        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(url))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(buildBody(request)))
                .build();

        CompletableFuture<HttpResponse<InputStream>> future =
                httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

        try (InputStream body = future.join().body();
             BufferedReader reader = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
            String line;
            int promptTokens = 0;
            int completionTokens = 0;
            while ((line = reader.readLine()) != null) {
                if (handle.isCancelled()) {
                    break;
                }
                if (!line.startsWith("data:")) {
                    continue; // 心跳等其它行
                }
                String data = line.substring(5).trim();
                if (data.isEmpty() || data.equals("[DONE]")) {
                    break;
                }
                JsonNode node = MAPPER.readTree(data);
                // 增量文本：choices[0].delta.content
                String delta = node.path("choices").path(0).path("delta").path("content").asText(null);
                if (delta != null && !delta.isEmpty()) {
                    onDelta.accept(delta);
                }
                // 用量：开启 stream_options.include_usage 后，最后一个 chunk 携带 usage
                JsonNode usage = node.get("usage");
                if (usage != null && usage.has("prompt_tokens")) {
                    promptTokens = usage.path("prompt_tokens").asInt();
                    completionTokens = usage.path("completion_tokens").asInt();
                }
            }
            if (onUsage != null && promptTokens + completionTokens > 0) {
                onUsage.accept(new AiRequest.AiUsage(promptTokens, completionTokens));
            }
        } catch (Exception e) {
            // 取消时抛出的中断异常不视为错误
            if (handle.isCancelled()) {
                return;
            }
            throw new IOException("AI 接口流式读取失败：" + e.getMessage(), e);
        }
    }

    private String buildBody(AiRequest request) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("model", request.model());
        root.put("stream", true);
        if (request.maxTokens() > 0) {
            root.put("max_tokens", request.maxTokens());
        }
        // 请求最后一个 chunk 返回 usage
        root.putObject("stream_options").put("include_usage", true);

        ArrayNode messages = root.putArray("messages");
        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            ObjectNode system = messages.addObject();
            system.put("role", "system");
            system.put("content", request.systemPrompt());
        }
        for (AiRequest.AiMessage m : request.messages()) {
            ObjectNode node = messages.addObject();
            node.put("role", m.role());
            node.put("content", m.content());
        }
        return root.toString();
    }

    static List<String> parseSseLinesForTest(String raw) {
        return raw.lines().filter(l -> l.startsWith("data:")).toList();
    }
}
