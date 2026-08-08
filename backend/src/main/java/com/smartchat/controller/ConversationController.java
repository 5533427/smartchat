package com.smartchat.controller;

import com.smartchat.common.ApiResponse;
import com.smartchat.dto.ChatDtos;
import com.smartchat.security.AuthContext;
import com.smartchat.service.AiService;
import com.smartchat.service.ConversationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 会话：CRUD + 消息 + AI 流式对话
 */
@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;
    private final AiService aiService;

    public ConversationController(ConversationService conversationService, AiService aiService) {
        this.conversationService = conversationService;
        this.aiService = aiService;
    }

    /** 会话列表（支持按标题关键字过滤） */
    @GetMapping
    public ApiResponse<List<ChatDtos.ConversationItem>> list(
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(conversationService.list(AuthContext.currentUserId(), keyword));
    }

    /** 新建会话 */
    @PostMapping
    public ApiResponse<ChatDtos.ConversationInfo> create(@RequestBody ChatDtos.CreateConversationRequest req) {
        return ApiResponse.ok(conversationService.create(AuthContext.currentUserId(),
                req == null ? null : req.title()));
    }

    /** 重命名会话 */
    @PutMapping("/{id}")
    public ApiResponse<ChatDtos.ConversationInfo> rename(
            @PathVariable Long id, @Valid @RequestBody ChatDtos.RenameConversationRequest req) {
        return ApiResponse.ok(conversationService.rename(AuthContext.currentUserId(), id, req.title()));
    }

    /** 删除会话（连同消息） */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        conversationService.delete(AuthContext.currentUserId(), id);
        return ApiResponse.ok();
    }

    /** 会话消息列表 */
    @GetMapping("/{id}/messages")
    public ApiResponse<List<ChatDtos.MessageInfo>> messages(@PathVariable Long id) {
        return ApiResponse.ok(conversationService.messages(AuthContext.currentUserId(), id));
    }

    /** 删除单条消息 */
    @DeleteMapping("/{id}/messages/{messageId}")
    public ApiResponse<Void> deleteMessage(@PathVariable Long id, @PathVariable Long messageId) {
        conversationService.deleteMessage(AuthContext.currentUserId(), id, messageId);
        return ApiResponse.ok();
    }

    /**
     * 发送消息（SSE 流式响应）
     * <p>
     * 事件流：start → delta×N → done / error
     * 前端通过 fetch + ReadableStream 读取，停止生成 = abort 请求。
     */
    @PostMapping("/{id}/messages")
    public SseEmitter send(@PathVariable Long id, @Valid @RequestBody ChatDtos.SendMessageRequest req) {
        return aiService.sendMessage(AuthContext.currentUserId(), id, req);
    }

    /** 重新生成上一条 AI 回复（SSE 流式响应，事件格式同上） */
    @PostMapping("/{id}/regenerate")
    public SseEmitter regenerate(@PathVariable Long id) {
        return aiService.regenerate(AuthContext.currentUserId(), id);
    }
}
