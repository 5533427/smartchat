package com.smartchat.dto;

import com.smartchat.entity.User;

import java.time.LocalDateTime;

/** 管理后台 DTO */
public final class AdminDtos {

    private AdminDtos() {
    }

    /** 用户管理列表项（比 UserInfo 多出统计字段） */
    public record AdminUserItem(
            Long id, String username, String nickname, String role,
            boolean enabled, LocalDateTime createdAt, long conversations, long messages
    ) {
        public static AdminUserItem from(User u, long conversations, long messages) {
            return new AdminUserItem(u.getId(), u.getUsername(), u.getNickname(), u.getRole(),
                    u.isEnabled(), u.getCreatedAt(), conversations, messages);
        }
    }

    /** 禁用/启用用户 */
    public record UpdateUserStatusRequest(boolean enabled) {
    }
}
