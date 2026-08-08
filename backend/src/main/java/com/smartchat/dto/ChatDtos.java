package com.smartchat.dto;

import com.smartchat.entity.Conversation;
import com.smartchat.entity.Message;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** 会话与消息 DTO */
public final class ChatDtos {

    private ChatDtos() {
    }

    /** 会话信息 */
    public record ConversationInfo(
            Long id, String title, LocalDateTime createdAt, LocalDateTime updatedAt, long messageCount
    ) {
    }

    /** 会话列表项（含最后一条消息摘要，便于侧边栏展示） */
    public record ConversationItem(
            Long id, String title, LocalDateTime updatedAt, long messageCount, String lastMessage
    ) {
    }

    /** 创建会话 */
    public record CreateConversationRequest(
            @Size(max = 100, message = "标题最长 100 字符") String title
    ) {
    }

    /** 重命名会话 */
    public record RenameConversationRequest(
            @NotBlank(message = "标题不能为空")
            @Size(max = 100, message = "标题最长 100 字符") String title
    ) {
    }

    /** 消息信息 */
    public record MessageInfo(
            Long id, String role, String content, int tokens, String meta, LocalDateTime createdAt
    ) {
        public static MessageInfo from(Message m) {
            return new MessageInfo(m.getId(), m.getRole(), m.getContent(), m.getTokens(), m.getMeta(), m.getCreatedAt());
        }
    }

    /** 发送消息请求（SSE 流式响应） */
    public record SendMessageRequest(
            @NotBlank(message = "消息内容不能为空")
            @Size(max = 20000, message = "单条消息最长 20000 字符")
            String content,

            /** 快捷动作：polish 润色 / translate 翻译 / summarize 总结 / expand 扩写，为空表示普通对话 */
            String action,

            /** 使用的提示词模板 ID（可选，与 action 二选一） */
            Long templateId
    ) {
    }

    /** 由 Conversation 构建列表项 */
    public static ConversationItem toItem(Conversation c, long messageCount, String lastMessage) {
        return new ConversationItem(c.getId(), c.getTitle(), c.getUpdatedAt(), messageCount, lastMessage);
    }
}
