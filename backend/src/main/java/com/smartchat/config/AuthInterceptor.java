package com.smartchat.config;

import com.smartchat.common.BusinessException;
import com.smartchat.entity.User;
import com.smartchat.repository.UserRepository;
import com.smartchat.security.AuthContext;
import com.smartchat.security.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 认证拦截器：
 * <p>
 * 1. 放行无需登录的路径（登录/注册、H2 控制台、预检请求）
 * 2. 校验 Authorization: Bearer <token>，并把用户写入 {@link AuthContext}
 * 3. /api/admin/** 额外校验管理员角色
 * 4. 校验用户是否被禁用
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public AuthInterceptor(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 放行预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        // 非 /api 路径（前端静态资源 / SPA 页面）公开放行
        if (!path.startsWith("/api/") && !path.startsWith("/h2-console")) {
            return true;
        }
        // 无需登录的 API（注意：/api/auth/me 需要登录，不放行）
        if (path.equals("/api/auth/register") || path.equals("/api/auth/login")
                || path.equals("/api/health")) {
            return true;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new BusinessException(401, "未登录或登录已过期，请重新登录");
        }

        Claims claims = jwtUtil.parse(header.substring(7));
        if (claims == null) {
            throw new BusinessException(401, "Token 无效或已过期，请重新登录");
        }

        User user = userRepository.findById(Long.valueOf(claims.getSubject())).orElse(null);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        if (!user.isEnabled()) {
            throw new BusinessException(403, "账号已被禁用，请联系管理员");
        }

        // 管理员接口校验
        if (path.startsWith("/api/admin/") && !User.ROLE_ADMIN.equals(user.getRole())) {
            throw new BusinessException(403, "无权限访问，仅管理员可操作");
        }

        AuthContext.set(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        AuthContext.clear();
    }
}
