package com.smartchat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Web 配置：CORS、认证拦截器、前端静态资源
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    /** 前端构建产物目录（frontend/dist），存在时由后端直接托管 */
    private final String frontendDist = Path.of("frontend", "dist").toAbsolutePath().toString();

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor).addPathPatterns("/**");
    }

    /** 开发环境放开跨域（前端 vite dev server 端口不同）；生产环境同源部署不需要 */
    /** 根路径直接转发到前端首页 */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 生产模式：后端托管前端构建产物（frontend/dist）
     * <p>
     * 带 SPA fallback：非静态资源路径（如 /admin 刷新）回退到 index.html。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (Files.isDirectory(Path.of(frontendDist))) {
            registry.addResourceHandler("/**")
                    .addResourceLocations("file:" + frontendDist + "/")
                    .resourceChain(true)
                    .addResolver(new org.springframework.web.servlet.resource.PathResourceResolver() {
                        @Override
                        protected org.springframework.core.io.Resource getResource(
                                String resourcePath, org.springframework.core.io.Resource location) throws java.io.IOException {
                            org.springframework.core.io.Resource requested = location.createRelative(resourcePath);
                            // 命中真实文件（排除目录本身）则返回
                            if (requested.exists() && requested.isReadable()
                                    && !(requested instanceof org.springframework.core.io.FileUrlResource f
                                    && f.getFile().isDirectory())) {
                                return requested;
                            }
                            // SPA 路由回退到首页
                            return location.createRelative("index.html");
                        }
                    });
        }
    }
}
