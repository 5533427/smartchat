package com.smartchat.dto;

import java.util.List;

/** 统计 DTO */
public final class StatsDtos {

    private StatsDtos() {
    }

    /** 全站概览（管理员） */
    public record Overview(
            long totalUsers, long totalConversations, long totalMessages,
            long todayMessages, long todayTokens, long totalTokens
    ) {
    }

    /** 个人统计 */
    public record Mine(
            long totalConversations, long totalMessages, long totalTokens, long todayMessages
    ) {
    }

    /** 每日趋势点 */
    public record TrendPoint(String date, long messages, long tokens) {
    }

    /** 趋势响应 */
    public record TrendResponse(List<TrendPoint> points) {
    }
}
