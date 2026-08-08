package com.smartchat.dto;

import com.smartchat.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 认证相关 DTO */
public final class AuthDtos {

    private AuthDtos() {
    }

    /** 注册请求 */
    public record RegisterRequest(
            @NotBlank(message = "用户名不能为空")
            @Pattern(regexp = "^[a-zA-Z0-9_]{3,20}$", message = "用户名需为 3-20 位字母/数字/下划线")
            String username,

            @NotBlank(message = "密码不能为空")
            @Size(min = 6, max = 32, message = "密码长度需为 6-32 位")
            String password,

            @Size(max = 20, message = "昵称最长 20 个字符")
            String nickname
    ) {
    }

    /** 登录请求 */
    public record LoginRequest(
            @NotBlank(message = "用户名不能为空") String username,
            @NotBlank(message = "密码不能为空") String password
    ) {
    }

    /** 登录响应 */
    public record LoginResponse(String token, UserInfo user) {
    }

    /** 用户信息（对外不暴露密码等敏感字段） */
    public record UserInfo(
            Long id, String username, String nickname, String role,
            boolean enabled, java.time.LocalDateTime createdAt
    ) {
        public static UserInfo from(User u) {
            return new UserInfo(u.getId(), u.getUsername(), u.getNickname(),
                    u.getRole(), u.isEnabled(), u.getCreatedAt());
        }
    }

    /** 修改昵称 */
    public record UpdateNicknameRequest(@Size(max = 20, message = "昵称最长 20 个字符") String nickname) {
    }

    /** 修改密码 */
    public record UpdatePasswordRequest(
            @NotBlank(message = "原密码不能为空") String oldPassword,
            @NotBlank(message = "新密码不能为空")
            @Size(min = 6, max = 32, message = "新密码长度需为 6-32 位") String newPassword
    ) {
    }
}
