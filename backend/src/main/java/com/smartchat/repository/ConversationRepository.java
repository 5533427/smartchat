package com.smartchat.repository;

import com.smartchat.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findByUserIdOrderByUpdatedAtDesc(Long userId);

    long countByUserId(Long userId);

    /** 按用户分组统计会话数（管理后台用） */
    @org.springframework.data.jpa.repository.Query("select c.userId, count(c) from Conversation c group by c.userId")
    java.util.List<Object[]> countGroupByUserId();
}
