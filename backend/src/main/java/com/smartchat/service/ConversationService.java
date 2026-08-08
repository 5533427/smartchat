package com.smartchat.service;

import com.smartchat.common.BusinessException;
import com.smartchat.dto.ChatDtos;
import com.smartchat.entity.Conversation;
import com.smartchat.entity.Message;
import com.smartchat.repository.ConversationRepository;
import com.smartchat.repository.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话服务：会话与消息的增删改查（AI 流式回复见 {@link AiService}）
 */
@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public ConversationService(ConversationRepository conversationRepository,
                               MessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    public List<ChatDtos.ConversationItem> list(Long userId, String keyword) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .filter(c -> keyword == null || keyword.isBlank() || c.getTitle().contains(keyword))
                .map(c -> {
                    List<Message> msgs = messageRepository.findByConversationIdOrderByCreatedAtAsc(c.getId());
                    String last = msgs.isEmpty() ? null
                            : msgs.get(msgs.size() - 1).getContent().replace("\n", " ").trim();
                    if (last != null && last.length() > 60) {
                        last = last.substring(0, 60) + "…";
                    }
                    return ChatDtos.toItem(c, msgs.size(), last);
                })
                .toList();
    }

    @Transactional
    public ChatDtos.ConversationInfo create(Long userId, String title) {
        Conversation conv = new Conversation();
        conv.setUserId(userId);
        if (title != null && !title.isBlank()) {
            conv.setTitle(title);
        }
        conversationRepository.save(conv);
        return toInfo(conv);
    }

    @Transactional
    public ChatDtos.ConversationInfo rename(Long userId, Long conversationId, String title) {
        Conversation conv = requireOwned(userId, conversationId);
        conv.setTitle(title);
        conv.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conv);
        return toInfo(conv);
    }

    @Transactional
    public void delete(Long userId, Long conversationId) {
        Conversation conv = requireOwned(userId, conversationId);
        messageRepository.deleteByConversationId(conversationId);
        conversationRepository.delete(conv);
    }

    public List<ChatDtos.MessageInfo> messages(Long userId, Long conversationId) {
        requireOwned(userId, conversationId);
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(ChatDtos.MessageInfo::from)
                .toList();
    }

    @Transactional
    public void deleteMessage(Long userId, Long conversationId, Long messageId) {
        requireOwned(userId, conversationId);
        Message msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException("消息不存在"));
        if (!msg.getConversationId().equals(conversationId)) {
            throw new BusinessException(403, "无权操作该消息");
        }
        messageRepository.delete(msg);
    }

    /** 校验会话归属并返回 */
    public Conversation requireOwned(Long userId, Long conversationId) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(404, "会话不存在"));
        if (!conv.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权访问该会话");
        }
        return conv;
    }

    private ChatDtos.ConversationInfo toInfo(Conversation c) {
        return new ChatDtos.ConversationInfo(c.getId(), c.getTitle(), c.getCreatedAt(),
                c.getUpdatedAt(), messageRepository.countByConversationId(c.getId()));
    }
}
