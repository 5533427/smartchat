package com.smartchat.security;

import com.smartchat.entity.User;

/**
 * 当前登录用户上下文（ThreadLocal）
 * <p>
 * 由 {@link com.smartchat.config.AuthInterceptor} 在请求进入时写入、请求结束时清除。
 * 业务代码通过 {@link #currentUser()} 获取当前用户。
 * 注意：SSE 流式线程不会携带该上下文，跨线程使用前需提前取出 userId。
 */
public final class AuthContext {

    private static final ThreadLocal<User> HOLDER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(User user) {
        HOLDER.set(user);
    }

    /** 当前登录用户（拦截器保证非空） */
    public static User currentUser() {
        User user = HOLDER.get();
        if (user == null) {
            throw new IllegalStateException("未获取到登录用户，请检查是否经过认证拦截器");
        }
        return user;
    }

    public static Long currentUserId() {
        return currentUser().getId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
