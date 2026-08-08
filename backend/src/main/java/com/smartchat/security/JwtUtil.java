package com.smartchat.security;

import com.smartchat.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

/**
 * JWT 工具：签发 / 解析 Token
 * <p>
 * 载荷（claims）：uid（用户 ID）、username、role
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final Duration expire;

    public JwtUtil(@Value("${smartchat.jwt.secret}") String secret,
                   @Value("${smartchat.jwt.expire-days}") long expireDays) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expire = Duration.ofDays(expireDays);
    }

    /** 为用户签发 Token */
    public String generate(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .claim("role", user.getRole())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expire.toMillis()))
                .signWith(key)
                .compact();
    }

    /** 解析 Token，失败（过期/伪造）返回 null */
    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            return null;
        }
    }
}
