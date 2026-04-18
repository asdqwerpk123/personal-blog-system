package org.example.personalblogsystem.config;

import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Component
public class BlogAuthConfigurationValidator {

    private static final String PROD_PLACEHOLDER_SECRET = "change-me-for-real-use-please-32-bytes";
    private static final String PLACEHOLDER_PREFIX = "${";
    private static final String WILDCARD_ORIGIN = "*";

    private final BlogAuthProperties blogAuthProperties;
    private final Environment environment;

    public BlogAuthConfigurationValidator(BlogAuthProperties blogAuthProperties, Environment environment) {
        this.blogAuthProperties = blogAuthProperties;
        this.environment = environment;
    }

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
