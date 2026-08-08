package com.smartchat.controller;

import com.smartchat.common.ApiResponse;
import com.smartchat.dto.AuthDtos;
import com.smartchat.security.AuthContext;
import com.smartchat.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 认证：注册 / 登录 / 当前用户 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthDtos.LoginResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest req) {
        return ApiResponse.ok(userService.register(req));
    }

    @PostMapping("/login")
    public ApiResponse<AuthDtos.LoginResponse> login(@Valid @RequestBody AuthDtos.LoginRequest req) {
        return ApiResponse.ok(userService.login(req));
    }

    @GetMapping("/me")
    public ApiResponse<AuthDtos.UserInfo> me() {
        return ApiResponse.ok(AuthDtos.UserInfo.from(AuthContext.currentUser()));
    }
}
