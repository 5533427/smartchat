package com.smartchat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 消息（用户或 AI 的一条发言）
 */
@Entity
@Table(name = "messages")
public class Message {

    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long conversationId;

    @Column(nullable = false)
    private Long userId;

    /** 角色：user / assistant */
    @Column(nullable = false, length = 20)
    private String role;

    /** 消息内容（支持 Markdown） */
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 触发方式元信息：action:polish / template:3 / null（普通对话），重新生成时复用 */
    @Column(length = 50)
    private String meta;

    /** 本次回复消耗的 token 数（AI 未返回 usage 时按字符估算） */
    @Column(nullable = false)
    private int tokens = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMeta() {
        return meta;
    }

    public void setMeta(String meta) {
        this.meta = meta;
    }

    public int getTokens() {
        return tokens;
    }

    public void setTokens(int tokens) {
        this.tokens = tokens;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
