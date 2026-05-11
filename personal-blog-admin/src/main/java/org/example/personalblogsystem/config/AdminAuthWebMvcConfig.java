package org.example.personalblogsystem.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

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
