package org.example.personalblogsystem.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * 后台 Web MVC 配置类，负责管理端跨域规则和本地上传资源映射。
 * 依赖 blog.auth.allowed-origins 与上传目录配置，支撑前端访问管理接口和静态上传文件。
 */
@Configuration
@EnableConfigurationProperties({BlogAuthProperties.class, AdminUploadProperties.class})
public class AdminAuthWebMvcConfig implements WebMvcConfigurer {

    private final BlogAuthProperties authProperties;
    private final AdminUploadProperties uploadProperties;

    public AdminAuthWebMvcConfig(BlogAuthProperties authProperties,
                                 AdminUploadProperties uploadProperties) {
        this.authProperties = authProperties;
        this.uploadProperties = uploadProperties;
    }

    /**
     * 配置管理端接口和上传资源的跨域访问规则。
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
        registry.addMapping("/admin/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization");
        registry.addMapping("/uploads/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*");
    }

    /**
     * 将本地上传目录映射为 /uploads/** 静态访问路径。
     *
     * @param registry Spring MVC 静态资源处理器注册器
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/friend-links/**")
                .addResourceLocations(ensureTrailingSlash(toResourceLocation(uploadProperties.getFriendLinkLogoDir())));
        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations(ensureTrailingSlash(toResourceLocation(uploadProperties.getAvatarDir())));
        registry.addResourceHandler("/uploads/article-covers/**")
                .addResourceLocations(ensureTrailingSlash(toResourceLocation(uploadProperties.getArticleCoverDir())));
    }

    private String toResourceLocation(String directory) {
        return Path.of(directory)
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString();
    }

    private String ensureTrailingSlash(String resourceLocation) {
        return resourceLocation.endsWith("/") ? resourceLocation : resourceLocation + "/";
    }
}
