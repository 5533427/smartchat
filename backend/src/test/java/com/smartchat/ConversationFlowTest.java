package com.smartchat;

import com.smartchat.dto.AuthDtos;
import com.smartchat.dto.ChatDtos;
import com.smartchat.entity.Conversation;
import com.smartchat.entity.Message;
import com.smartchat.repository.ConversationRepository;
import com.smartchat.repository.MessageRepository;
import com.smartchat.repository.UserRepository;
import com.smartchat.service.AiService;
import com.smartchat.service.ConversationService;
import com.smartchat.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 会话流程测试：CRUD + 发送消息的同步落库与标题自动生成
 * （AI 流式部分依赖外部 API，未配置 Key 时应优雅返回 error 事件）
 */
@SpringBootTest
@Transactional // 每个测试自动回滚，清理代码在事务内执行
class ConversationFlowTest {

    @Autowired
    private UserService userService;
    @Autowired
    private ConversationService conversationService;
    @Autowired
    private AiService aiService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ConversationRepository conversationRepository;
    @Autowired
    private MessageRepository messageRepository;

    private Long userId;

    private void register() {
        String name = "u" + System.nanoTime() % 100000000;
        AuthDtos.LoginResponse resp = userService.register(
                new AuthDtos.RegisterRequest(name, "123456", "会话测试"));
        userId = resp.user().id();
    }

    @AfterEach
    void cleanup() {
        if (userId == null) {
            return;
        }
        conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                .forEach(c -> messageRepository.deleteByConversationId(c.getId()));
        conversationRepository.deleteAll(conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId));
        userRepository.deleteById(userId);
    }

    @Test
    void conversationCrud() {
        register();
        // 创建
        ChatDtos.ConversationInfo created = conversationService.create(userId, "测试会话");
        assertThat(created.title()).isEqualTo("测试会话");
        // 重命名
        ChatDtos.ConversationInfo renamed = conversationService.rename(userId, created.id(), "改名了");
        assertThat(renamed.title()).isEqualTo("改名了");
        // 列表
        List<ChatDtos.ConversationItem> list = conversationService.list(userId, null);
        assertThat(list).hasSize(1);
        // 关键字过滤
        assertThat(conversationService.list(userId, "不存在")).isEmpty();
        // 删除
        conversationService.delete(userId, created.id());
        assertThat(conversationService.list(userId, null)).isEmpty();
    }

    @Test
    void sendMessagePersistsAndAutoTitles() {
        register();
        ChatDtos.ConversationInfo conv = conversationService.create(userId, null);
        // 未配置 API Key：用户消息同步落库 + 标题自动生成，AI 错误通过 SSE error 事件异步返回
        SseEmitter emitter = aiService.sendMessage(userId, conv.id(),
                new ChatDtos.SendMessageRequest("你好呀，这是第一条很长很长的测试消息，用来验证标题截断逻辑", null, null));
        assertThat(emitter).isNotNull();

        Conversation after = conversationRepository.findById(conv.id()).orElseThrow();
        assertThat(after.getTitle()).isNotEqualTo(Conversation.DEFAULT_TITLE);
        assertThat(after.getTitle().length()).isLessThanOrEqualTo(21);

        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conv.id());
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getRole()).isEqualTo(Message.ROLE_USER);
        assertThat(messages.get(0).getContent()).contains("第一条很长很长");
    }

    @Test
    void cannotAccessOthersConversation() {
        register();
        ChatDtos.ConversationInfo conv = conversationService.create(userId, "私有会话");

        // 另一个用户访问 → 403
        String name2 = "v" + System.nanoTime() % 100000000;
        AuthDtos.LoginResponse other = userService.register(
                new AuthDtos.RegisterRequest(name2, "123456", "别人"));

        org.junit.jupiter.api.Assertions.assertThrows(com.smartchat.common.BusinessException.class,
                () -> conversationService.messages(other.user().id(), conv.id()));
    }
}
