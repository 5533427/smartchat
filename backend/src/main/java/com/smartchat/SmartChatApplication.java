package com.smartchat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SmartChat 启动入口
 * <p>
 * 技术栈：Spring Boot 3 + Spring Data JPA + H2/MySQL + JWT
 */
@SpringBootApplication
public class SmartChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartChatApplication.class, args);
        System.out.println("""
                ==========================================================
                  SmartChat 启动成功 ✔
                  前端页面:   http://localhost:8080
                  H2 控制台:  http://localhost:8080/h2-console  (JDBC URL: jdbc:h2:file:./data/smartchat)
                  默认管理员: admin / admin123
                ==========================================================""");
    }
}
