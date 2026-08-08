package com.smartchat.controller;

import com.smartchat.common.ApiResponse;
import com.smartchat.dto.StatsDtos;
import com.smartchat.security.AuthContext;
import com.smartchat.service.StatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 统计：个人 / 全站 / 趋势 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    /** 我的统计（普通用户可用） */
    @GetMapping("/me")
    public ApiResponse<StatsDtos.Mine> mine() {
        return ApiResponse.ok(statsService.mine(AuthContext.currentUserId()));
    }

    /** 全站概览（管理员） */
    @GetMapping("/overview")
    public ApiResponse<StatsDtos.Overview> overview() {
        return ApiResponse.ok(statsService.overview());
    }

    /** 最近 N 天消息趋势（管理员） */
    @GetMapping("/trend")
    public ApiResponse<StatsDtos.TrendResponse> trend(@RequestParam(defaultValue = "14") int days) {
        return ApiResponse.ok(statsService.trend(days));
    }
}
