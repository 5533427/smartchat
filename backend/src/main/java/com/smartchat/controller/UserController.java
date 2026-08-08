package com.smartchat.controller;

import com.smartchat.common.ApiResponse;
import com.smartchat.dto.AuthDtos;
import com.smartchat.dto.ConfigDtos;
import com.smartchat.security.AuthContext;
import com.smartchat.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 个人中心：资料、密码、AI API 配置 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping("/profile")
    public ApiResponse<AuthDtos.UserInfo> updateProfile(@Valid @RequestBody AuthDtos.UpdateNicknameRequest req) {
        return ApiResponse.ok(userService.updateNickname(AuthContext.currentUserId(), req.nickname()));
    }

    @PutMapping("/password")
    public ApiResponse<Void> updatePassword(@Valid @RequestBody AuthDtos.UpdatePasswordRequest req) {
        userService.updatePassword(AuthContext.currentUserId(), req.oldPassword(), req.newPassword());
        return ApiResponse.ok();
    }

    /** 读取个人 AI 配置（未配置时返回系统默认） */
    @GetMapping("/api-config")
    public ApiResponse<ConfigDtos.ApiConfigInfo> getApiConfig() {
        return ApiResponse.ok(userService.getApiConfig(AuthContext.currentUserId()));
    }

    /** 保存个人 AI 配置 */
    @PutMapping("/api-config")
    public ApiResponse<ConfigDtos.ApiConfigInfo> saveApiConfig(@Valid @RequestBody ConfigDtos.ApiConfigRequest req) {
        return ApiResponse.ok(userService.saveApiConfig(AuthContext.currentUserId(), req));
    }
}
