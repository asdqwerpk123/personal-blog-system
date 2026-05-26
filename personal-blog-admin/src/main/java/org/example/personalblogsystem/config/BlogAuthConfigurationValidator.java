package org.example.personalblogsystem.config;

import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * 认证配置启动校验器，在应用启动阶段检查 JWT 密钥、过期时间和跨域来源配置。
 * 生产环境下额外禁止默认密钥、占位符、通配来源和本机回环地址，降低误配置风险。
 */
@Component
public class BlogAuthConfigurationValidator {

    /**
     * 默认 JWT 密钥占位值，生产环境不能继续使用。
     */
    private static final String PROD_PLACEHOLDER_SECRET = "change-me-for-real-use-please-32-bytes";
    /**
     * 配置占位符前缀，用于识别未解析的环境变量表达式。
     */
    private static final String PLACEHOLDER_PREFIX = "${";
    /**
     * 跨域通配来源，生产环境禁止使用。
     */
    private static final String WILDCARD_ORIGIN = "*";

    private final BlogAuthProperties blogAuthProperties;
    private final Environment environment;

    public BlogAuthConfigurationValidator(BlogAuthProperties blogAuthProperties, Environment environment) {
        this.blogAuthProperties = blogAuthProperties;
        this.environment = environment;
    }

    /**
     * 校验认证相关配置是否满足运行要求。
     *
     * @throws IllegalStateException JWT 密钥、过期时间或跨域来源配置不合法时抛出
     */
    @PostConstruct
    public void validate() {
        String secret = blogAuthProperties.getJwt().getSecret();
        long expireMinutes = blogAuthProperties.getJwt().getAccessTokenExpireMinutes();
        List<String> allowedOrigins = blogAuthProperties.getAllowedOrigins();

        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("blog.auth.jwt.secret must not be blank");
        }

        if (secret.trim().length() < 32) {
            throw new IllegalStateException("blog.auth.jwt.secret must be at least 32 characters long");
        }

        if (expireMinutes <= 0) {
            throw new IllegalStateException("blog.auth.jwt.access-token-expire-minutes must be greater than 0");
        }

        if (allowedOrigins == null || allowedOrigins.isEmpty() || allowedOrigins.stream().anyMatch(origin -> origin == null || origin.trim().isEmpty())) {
            throw new IllegalStateException("blog.auth.allowed-origins must not be blank or empty");
        }

        if (environment.acceptsProfiles(Profiles.of("prod")) && PROD_PLACEHOLDER_SECRET.equals(secret.trim())) {
            throw new IllegalStateException("blog.auth.jwt.secret must be replaced in production");
        }

        if (environment.acceptsProfiles(Profiles.of("prod")) && allowedOrigins.stream()
                .map(String::trim)
                .anyMatch(origin -> origin.contains(PLACEHOLDER_PREFIX) || origin.contains("}"))) {
            throw new IllegalStateException("blog.auth.allowed-origins must be resolved in production");
        }

        if (environment.acceptsProfiles(Profiles.of("prod")) && allowedOrigins.stream()
                .map(String::trim)
                .anyMatch(WILDCARD_ORIGIN::equals)) {
            throw new IllegalStateException("blog.auth.allowed-origins must not allow wildcard origins in production");
        }

        if (environment.acceptsProfiles(Profiles.of("prod")) && allowedOrigins.stream()
                .map(String::trim)
                .anyMatch(this::isLoopbackOrigin)) {
            throw new IllegalStateException("blog.auth.allowed-origins must not use localhost or loopback origins in production");
        }
    }

    private boolean isLoopbackOrigin(String origin) {
        try {
            URI uri = new URI(origin);
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return false;
            }

            String normalizedHost = host.trim();
            if ("localhost".equalsIgnoreCase(normalizedHost)) {
                return true;
            }

            InetAddress address = InetAddress.getByName(normalizedHost);
            return address.isLoopbackAddress();
        } catch (URISyntaxException | IllegalArgumentException | java.net.UnknownHostException exception) {
            return false;
        }
    }
}
