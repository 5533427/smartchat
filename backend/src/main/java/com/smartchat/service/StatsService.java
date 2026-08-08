package com.smartchat.service;

import com.smartchat.dto.StatsDtos;
import com.smartchat.entity.Message;
import com.smartchat.repository.ConversationRepository;
import com.smartchat.repository.MessageRepository;
import com.smartchat.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计服务：个人统计 + 管理后台全站统计与趋势
 */
@Service
public class StatsService {

    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public StatsService(UserRepository userRepository,
                        ConversationRepository conversationRepository,
                        MessageRepository messageRepository) {
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    /** 个人统计 */
    public StatsDtos.Mine mine(Long userId) {
        LocalDateTime today = LocalDate.now().atStartOfDay();
        return new StatsDtos.Mine(
                conversationRepository.countByUserId(userId),
                messageRepository.countByUserId(userId),
                messageRepository.sumTokensByUserId(userId),
                messageRepository.countByUserIdAndCreatedAtAfter(userId, today));
    }

    /** 全站概览（管理员） */
    public StatsDtos.Overview overview() {
        LocalDateTime today = LocalDate.now().atStartOfDay();
        List<Message> todayMessages = messageRepository.findByCreatedAtAfter(today);
        long todayTokens = todayMessages.stream().mapToLong(Message::getTokens).sum();
        return new StatsDtos.Overview(
                userRepository.count(),
                conversationRepository.count(),
                messageRepository.count(),
                todayMessages.size(),
                todayTokens,
                messageRepository.sumAllTokens());
    }

    /** 最近 N 天消息量 / token 趋势（管理员） */
    public StatsDtos.TrendResponse trend(int days) {
        int d = Math.max(1, Math.min(days, 90));
        LocalDateTime start = LocalDate.now().minusDays(d - 1L).atStartOfDay();
        List<Message> messages = messageRepository.findByCreatedAtAfter(start);

        // 按日期聚合
        Map<LocalDate, long[]> byDate = new LinkedHashMap<>();
        for (int i = 0; i < d; i++) {
            byDate.put(LocalDate.now().minusDays(i), new long[]{0, 0});
        }
        for (Message m : messages) {
            LocalDate date = m.getCreatedAt().toLocalDate();
            long[] v = byDate.computeIfAbsent(date, k -> new long[]{0, 0});
            v[0]++;
            v[1] += m.getTokens();
        }
        List<StatsDtos.TrendPoint> points = new ArrayList<>();
        for (int i = d - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            long[] v = byDate.get(date);
            points.add(new StatsDtos.TrendPoint(date.toString(), v[0], v[1]));
        }
        return new StatsDtos.TrendResponse(points);
    }
}
