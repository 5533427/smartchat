package com.smartchat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 通用 Bean 配置
 */
@Configuration
public class BeanConfig {

    /** 密码加密器（仅引入 spring-security-crypto，未启用完整 Spring Security） */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
