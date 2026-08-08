package com.smartchat.controller;

import com.smartchat.common.ApiResponse;
import com.smartchat.common.PageResult;
import com.smartchat.dto.AdminDtos;
import com.smartchat.security.AuthContext;
import com.smartchat.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 管理后台（/api/admin/** 已由拦截器校验管理员角色） */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /** 用户列表 */
    @GetMapping("/users")
    public ApiResponse<PageResult<AdminDtos.AdminUserItem>> users(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(adminService.listUsers(page, size, keyword));
    }

    /** 禁用 / 启用用户 */
    @PutMapping("/users/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
                                          @Valid @RequestBody AdminDtos.UpdateUserStatusRequest req) {
        adminService.updateStatus(AuthContext.currentUserId(), id, req.enabled());
        return ApiResponse.ok();
    }

    /** 删除用户（连同其会话与消息） */
    @DeleteMapping("/users/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(AuthContext.currentUserId(), id);
        return ApiResponse.ok();
    }
}
