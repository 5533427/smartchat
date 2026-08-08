package com.smartchat.repository;

import com.smartchat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    long countByConversationId(Long conversationId);

    long countByUserId(Long userId);

    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime after);

    long countByCreatedAtAfter(LocalDateTime after);

    /** 管理员统计用：统计一段时间内的全部消息 */
    List<Message> findByCreatedAtAfter(LocalDateTime after);

    long deleteByConversationId(Long conversationId);

    /** 统计某用户消息总 token 数 */
    @org.springframework.data.jpa.repository.Query("select coalesce(sum(m.tokens), 0) from Message m where m.userId = :userId")
    long sumTokensByUserId(Long userId);

    /** 统计全站消息总 token 数 */
    @org.springframework.data.jpa.repository.Query("select coalesce(sum(m.tokens), 0) from Message m")
    long sumAllTokens();

    /** 按用户分组统计消息数（管理后台用） */
    @org.springframework.data.jpa.repository.Query("select m.userId, count(m) from Message m group by m.userId")
    java.util.List<Object[]> countGroupByUserId();

    /** 按用户分组统计 token 数（管理后台用） */
    @org.springframework.data.jpa.repository.Query("select m.userId, coalesce(sum(m.tokens), 0) from Message m group by m.userId")
    java.util.List<Object[]> sumTokensGroupByUserId();
}
