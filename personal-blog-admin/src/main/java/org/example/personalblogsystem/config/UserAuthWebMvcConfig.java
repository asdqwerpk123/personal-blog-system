package org.example.personalblogsystem.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 用户端 Web MVC 配置类，负责 /user/** 与 /public/** 接口的跨域规则。
 * 跨域来源统一读取 blog.auth.allowed-origins，保证管理端和用户端安全策略一致。
 */
@Configuration
public class UserAuthWebMvcConfig implements WebMvcConfigurer {

    private final BlogAuthProperties authProperties;

    public UserAuthWebMvcConfig(BlogAuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    /**
     * 配置用户端接口和公开接口的跨域访问规则。
     *
     * @param registry Spring MVC 跨域配置注册器
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] allowedOrigins = authProperties.getAllowedOrigins().stream()
                .filter(origin -> origin != null)
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
        registry.addMapping("/user/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization");
        registry.addMapping("/public/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*");
    }
}
