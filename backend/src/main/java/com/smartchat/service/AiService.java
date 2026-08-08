package com.smartchat.service;

import com.smartchat.ai.AiClient;
import com.smartchat.ai.AiClientFactory;
import com.smartchat.ai.AiRequest;
import com.smartchat.common.BusinessException;
import com.smartchat.config.AiProperties;
import com.smartchat.dto.ChatDtos;
import com.smartchat.entity.ApiConfig;
import com.smartchat.entity.Conversation;
import com.smartchat.entity.Message;
import com.smartchat.entity.PromptTemplate;
import com.smartchat.repository.ApiConfigRepository;
import com.smartchat.repository.ConversationRepository;
import com.smartchat.repository.MessageRepository;
import com.smartchat.repository.PromptTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI 服务：发送消息 / 重新生成（SSE 流式）
 * <p>
 * 整体流程：
 * 1. 保存用户消息，返回 SseEmitter
 * 2. 后台线程调用上游 AI（OpenAI 兼容 / Anthropic），把增量文本以 SSE 事件推给前端
 * 3. 流结束后把 AI 回复 + token 用量落库，发送 done 事件
 * 4. 用户点「停止」或断开连接时取消上游请求
 */
@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    /** 默认系统提示词 */
    public static final String DEFAULT_SYSTEM_PROMPT = """
            你是 SmartChat 的 AI 助手，一个友善、专业的智能对话伙伴。
            - 回答清晰有条理，中文优先；涉及代码时给出可直接运行的示例；
            - 用户未要求时不要堆砌无关信息；
            - 支持 Markdown 格式输出。
            """;

    /** 快捷动作 → 系统提示词 */
    private static final Map<String, String> ACTION_PROMPTS = Map.of(
            "polish", """
                    你是一位文字润色专家。请对用户输入的文字进行润色：
                    - 修正语病、错别字与标点问题；
                    - 优化表达，使其更流畅、更自然、更准确；
                    - 保持原文意思与风格不变；
                    - 直接输出润色后的完整文本，不要解释修改了哪里。
                    """,
            "translate", """
                    你是一位专业的翻译专家，精通中文与英文。
                    请将用户输入的内容翻译成另一门语言：
                    - 若输入为中文，翻译成英文；若输入为英文，翻译成中文；
                    - 保留原文的语气、格式与专业术语；
                    - 只输出译文，不要任何解释。
                    """,
            "summarize", """
                    你是一位内容提炼专家。请对用户输入的内容进行总结：
                    - 提炼核心观点与关键信息，按要点分条输出；
                    - 保持客观，不添加原文没有的内容；
                    - 摘要控制在原文长度的四分之一以内。
                    """,
            "expand", """
                    你是一位写作专家。请对用户输入的内容进行扩写：
                    - 在不改变原意的前提下，补充细节、例证与逻辑衔接；
                    - 语言流畅自然，保持原文风格；
                    - 输出扩写后的完整文本。
                    """);

    /** 上游最大输出 token 数 */
    private static final int MAX_TOKENS = 16000;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ApiConfigRepository apiConfigRepository;
    private final PromptTemplateRepository templateRepository;
    private final UserService userService;
    private final AiProperties aiProperties;
    private final ExecutorService executor = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "ai-stream");
        t.setDaemon(true);
        return t;
    });

    public AiService(ConversationRepository conversationRepository,
                     MessageRepository messageRepository,
                     ApiConfigRepository apiConfigRepository,
                     PromptTemplateRepository templateRepository,
                     UserService userService,
                     AiProperties aiProperties) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.apiConfigRepository = apiConfigRepository;
        this.templateRepository = templateRepository;
        this.userService = userService;
        this.aiProperties = aiProperties;
    }

    /**
     * 发送消息并流式获取 AI 回复。
     * 同步保存用户消息，异步执行上游调用，返回 SSE 流。
     */
    public SseEmitter sendMessage(Long userId, Long conversationId, ChatDtos.SendMessageRequest req) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(404, "会话不存在"));
        if (!conv.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权访问该会话");
        }

        // 1. 保存用户消息
        Message userMessage = new Message();
        userMessage.setConversationId(conversationId);
        userMessage.setUserId(userId);
        userMessage.setRole(Message.ROLE_USER);
        userMessage.setContent(req.content());
        userMessage.setMeta(metaOf(req));
        messageRepository.save(userMessage);

        // 2. 默认标题 → 用首条消息摘要
        if (Conversation.DEFAULT_TITLE.equals(conv.getTitle())) {
            conv.setTitle(truncate(req.content(), 20));
            conv.setUpdatedAt(LocalDateTime.now());
            conversationRepository.save(conv);
        }

        // 3. 解析本次系统提示词（动作 / 模板）
        String systemPrompt = resolveSystemPrompt(userId, req);
        return streamAndPersist(userId, conversationId, systemPrompt);
    }

    /**
     * 重新生成：删除最后一条 AI 回复，用同样的上下文重新请求。
     * 触发方式：前端删除最后一条 assistant 消息后调用 regenerate。
     */
    public SseEmitter regenerate(Long userId, Long conversationId) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(404, "会话不存在"));
        if (!conv.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权访问该会话");
        }
        List<Message> msgs = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        if (msgs.isEmpty() || !Message.ROLE_ASSISTANT.equals(msgs.get(msgs.size() - 1).getRole())) {
            throw new BusinessException("最后一条不是 AI 回复，无法重新生成");
        }
        Message lastAssistant = msgs.get(msgs.size() - 1);
        messageRepository.delete(lastAssistant);

        // 用上一条用户消息的 meta 还原动作 / 模板
        String meta = null;
        for (int i = msgs.size() - 2; i >= 0; i--) {
            if (Message.ROLE_USER.equals(msgs.get(i).getRole())) {
                meta = msgs.get(i).getMeta();
                break;
            }
        }
        return streamAndPersist(userId, conversationId, resolveMetaPrompt(userId, meta));
    }

    /** 组装并执行流式请求（工作线程） */
    private SseEmitter streamAndPersist(Long userId, Long conversationId, String systemPrompt) {
        SseEmitter emitter = new SseEmitter(0L); // 0 = 不超时（长回答可能超过 1 分钟）
        AiClient.AiStreamHandle handle = new AiClient.AiStreamHandle();
        emitter.onCompletion(handle::cancel);
        emitter.onTimeout(() -> {
            handle.cancel();
            emitter.complete();
        });
        emitter.onError(e -> handle.cancel());

        executor.submit(() -> {
            try {
                sendEvent(emitter, "start", Map.of("type", "start"));

                // 解析用户配置（个人配置 > 环境变量默认值）
                ResolvedAi config = resolveAiConfig(userId);
                AiClient client = AiClientFactory.create(config.provider(), config.baseUrl(), config.apiKey());
                AiRequest request = new AiRequest(config.model(), systemPrompt,
                        buildHistory(conversationId), MAX_TOKENS);

                StringBuilder answer = new StringBuilder();
                int[] promptTokens = {0};
                int[] completionTokens = {0};
                client.stream(request, handle, delta -> {
                    answer.append(delta);
                    sendEvent(emitter, "delta", Map.of("type", "delta", "content", delta));
                }, usage -> {
                    if (usage != null) {
                        promptTokens[0] = usage.promptTokens();
                        completionTokens[0] = usage.completionTokens();
                    }
                });

                // 停止生成（用户点停止/断连）时丢弃半截回复
                if (handle.isCancelled()) {
                    return;
                }

                // 落库 AI 回复
                Message aiMessage = saveAssistantMessage(userId, conversationId, answer.toString(),
                        promptTokens[0], completionTokens[0]);
                sendEvent(emitter, "done", Map.of(
                        "type", "done",
                        "messageId", aiMessage.getId(),
                        "promptTokens", promptTokens[0],
                        "completionTokens", completionTokens[0],
                        "totalTokens", promptTokens[0] + completionTokens[0]));
            } catch (BusinessException e) {
                sendEvent(emitter, "error", Map.of("type", "error", "message", e.getMessage()));
            } catch (Exception e) {
                log.error("AI 流式生成失败, conversationId={}", conversationId, e);
                sendEvent(emitter, "error", Map.of("type", "error", "message", "AI 生成失败：" + e.getMessage()));
            } finally {
                emitter.complete();
            }
        });
        return emitter;
    }

    /** 保存 AI 回复；token 未统计到时按字符估算 */
    private Message saveAssistantMessage(Long userId, Long conversationId, String content,
                                         int promptTokens, int completionTokens) {
        Message msg = new Message();
        msg.setConversationId(conversationId);
        msg.setUserId(userId);
        msg.setRole(Message.ROLE_ASSISTANT);
        msg.setContent(content);
        int total = promptTokens + completionTokens;
        msg.setTokens(total > 0 ? total : estimateTokens(content));
        msg.setCreatedAt(LocalDateTime.now());
        Conversation conv = conversationRepository.findById(conversationId).orElse(null);
        if (conv != null) {
            conv.setUpdatedAt(LocalDateTime.now());
            conversationRepository.save(conv);
        }
        return messageRepository.save(msg);
    }

    /** 构建发送给上游的对话历史：最近 N 条，合并连续同角色，保证以 user 开头 */
    private List<AiRequest.AiMessage> buildHistory(Long conversationId) {
        List<Message> all = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        List<AiRequest.AiMessage> result = new ArrayList<>();
        int max = Math.max(1, aiProperties.getMaxContextMessages());
        for (Message m : all) {
            String role = m.getRole();
            if (!Message.ROLE_USER.equals(role) && !Message.ROLE_ASSISTANT.equals(role)) {
                continue;
            }
            String content = m.getContent();
            if (content == null || content.isBlank()) {
                continue;
            }
            result.add(new AiRequest.AiMessage(role, content));
        }
        // 只保留最近 max 条
        if (result.size() > max) {
            result = new ArrayList<>(result.subList(result.size() - max, result.size()));
        }
        // 合并连续同角色（防止历史异常）
        List<AiRequest.AiMessage> merged = new ArrayList<>();
        for (AiRequest.AiMessage m : result) {
            if (!merged.isEmpty() && merged.get(merged.size() - 1).role().equals(m.role())) {
                merged.set(merged.size() - 1,
                        new AiRequest.AiMessage(m.role(), merged.get(merged.size() - 1).content() + "\n" + m.content()));
            } else {
                merged.add(m);
            }
        }
        // 必须以 user 开头（Anthropic 协议要求）
        while (!merged.isEmpty() && !Message.ROLE_USER.equals(merged.get(0).role())) {
            merged.remove(0);
        }
        return merged;
    }

    /** 解析用户配置：个人配置 > 系统默认（环境变量） */
    private ResolvedAi resolveAiConfig(Long userId) {
        ApiConfig cfg = apiConfigRepository.findByUserId(userId).orElse(null);
        String provider = cfg != null && cfg.getProvider() != null ? cfg.getProvider() : aiProperties.getProvider();
        String baseUrl = cfg != null && notBlank(cfg.getBaseUrl()) ? cfg.getBaseUrl() : aiProperties.getBaseUrl();
        String apiKey = cfg != null && notBlank(cfg.getApiKey()) ? cfg.getApiKey() : aiProperties.getApiKey();
        String model = cfg != null && notBlank(cfg.getModel()) ? cfg.getModel() : userService.defaultModel(provider);
        return new ResolvedAi(provider, baseUrl, apiKey, model);
    }

    private record ResolvedAi(String provider, String baseUrl, String apiKey, String model) {
    }

    /** 解析发送请求中的系统提示词：动作 > 模板 > 默认 */
    private String resolveSystemPrompt(Long userId, ChatDtos.SendMessageRequest req) {
        if (notBlank(req.action())) {
            return ACTION_PROMPTS.getOrDefault(req.action(), DEFAULT_SYSTEM_PROMPT);
        }
        if (req.templateId() != null) {
            PromptTemplate t = templateRepository.findById(req.templateId())
                    .orElseThrow(() -> new BusinessException("模板不存在"));
            if (!t.isSystem() && !t.getUserId().equals(userId)) {
                throw new BusinessException(403, "无权使用该模板");
            }
            return t.getSystemPrompt();
        }
        return DEFAULT_SYSTEM_PROMPT;
    }

    /** 重新生成时根据 meta 还原系统提示词 */
    private String resolveMetaPrompt(Long userId, String meta) {
        if (meta == null) {
            return DEFAULT_SYSTEM_PROMPT;
        }
        if (meta.startsWith("action:")) {
            return ACTION_PROMPTS.getOrDefault(meta.substring("action:".length()), DEFAULT_SYSTEM_PROMPT);
        }
        if (meta.startsWith("template:")) {
            try {
                Long id = Long.valueOf(meta.substring("template:".length()));
                PromptTemplate t = templateRepository.findById(id).orElse(null);
                if (t != null && (t.isSystem() || t.getUserId().equals(userId))) {
                    return t.getSystemPrompt();
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return DEFAULT_SYSTEM_PROMPT;
    }

    private String metaOf(ChatDtos.SendMessageRequest req) {
        if (notBlank(req.action())) {
            return "action:" + req.action();
        }
        if (req.templateId() != null) {
            return "template:" + req.templateId();
        }
        return null;
    }

    /** 发送 SSE 事件；客户端已断开时标记取消 */
    private void sendEvent(SseEmitter emitter, String name, Map<String, Object> data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (IOException e) {
            // 客户端断开，交给 onCompletion 取消上游
        }
    }

    private String truncate(String s, int max) {
        String oneLine = s.replace("\n", " ").trim();
        return oneLine.length() <= max ? oneLine : oneLine.substring(0, max) + "…";
    }

    /** 粗略估算 token：中英混合按字符数/4（README 已说明仅为估算） */
    private int estimateTokens(String s) {
        return Math.max(1, s.length() / 4);
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
