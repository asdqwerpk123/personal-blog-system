package org.example.personalblogsystem.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BlogAuthConfigurationValidatorTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void shouldFailWhenJwtSecretIsBlank() {
        withOverrides(Map.of(
                "blog.auth.jwt.secret", "   ",
                "blog.auth.jwt.access-token-expire-minutes", "60"))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage("blog.auth.jwt.secret must not be blank");
                });
    }

    @Test
    void shouldFailWhenJwtSecretIsTooShort() {
        withOverrides(Map.of(
                "blog.auth.jwt.secret", "too-short-secret",
                "blog.auth.jwt.access-token-expire-minutes", "60"))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage("blog.auth.jwt.secret must be at least 32 characters long");
                });
    }

    @Test
    void shouldFailWhenJwtAccessTokenExpireMinutesIsNotPositive() {
        withOverrides(Map.of(
                "blog.auth.jwt.secret", "0123456789abcdef0123456789abcdef",
                "blog.auth.jwt.access-token-expire-minutes", "0"))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage("blog.auth.jwt.access-token-expire-minutes must be greater than 0");
                });
    }

    @Test
    void shouldFailInProductionWhenUsingRepositoryPlaceholderSecret() {
        withOverrides(Map.of(
                "spring.profiles.active", "prod",
                "blog.auth.jwt.secret", "change-me-for-real-use-please-32-bytes",
                "blog.auth.jwt.access-token-expire-minutes", "60"))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage("blog.auth.jwt.secret must be replaced in production");
                });
    }

    @Test
    void shouldFailInProductionWhenAllowedOriginsUsesLocalhostDefault() {
        withOverrides(Map.of(
                "spring.profiles.active", "prod",
                "blog.auth.allowed-origins[0]", "http://localhost:5173",
                "blog.auth.jwt.secret", "0123456789abcdef0123456789abcdef",
                "blog.auth.jwt.access-token-expire-minutes", "60"))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage("blog.auth.allowed-origins must not use localhost or loopback origins in production");
                });
    }

    @Test
    void shouldFailInProductionWhenAllowedOriginsUsesLocalhostOnAnotherPort() {
        withOverrides(Map.of(
                "spring.profiles.active", "prod",
                "blog.auth.allowed-origins[0]", "http://localhost:3000",
                "blog.auth.jwt.secret", "0123456789abcdef0123456789abcdef",
                "blog.auth.jwt.access-token-expire-minutes", "60"))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage("blog.auth.allowed-origins must not use localhost or loopback origins in production");
                });
    }

    @Test
    void shouldFailInProductionWhenAllowedOriginsUsesHttpsLocalhost() {
        withOverrides(Map.of(
                "spring.profiles.active", "prod",
                "blog.auth.allowed-origins[0]", "https://localhost:5173",
                "blog.auth.jwt.secret", "0123456789abcdef0123456789abcdef",
                "blog.auth.jwt.access-token-expire-minutes", "60"))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage("blog.auth.allowed-origins must not use localhost or loopback origins in production");
                });
    }

    @Test
    void shouldFailInProductionWhenAllowedOriginsUsesIpv4Loopback() {
        withOverrides(Map.of(
                "spring.profiles.active", "prod",
                "blog.auth.allowed-origins[0]", "http://127.0.0.1:8080",
                "blog.auth.jwt.secret", "0123456789abcdef0123456789abcdef",
                "blog.auth.jwt.access-token-expire-minutes", "60"))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage("blog.auth.allowed-origins must not use localhost or loopback origins in production");
                });
    }

    @Test
    void shouldFailInProductionWhenAllowedOriginsUsesIpv6Loopback() {
        withOverrides(Map.of(
                "spring.profiles.active", "prod",
                "blog.auth.allowed-origins[0]", "http://[::1]:5173",
                "blog.auth.jwt.secret", "0123456789abcdef0123456789abcdef",
                "blog.auth.jwt.access-token-expire-minutes", "60"))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage("blog.auth.allowed-origins must not use localhost or loopback origins in production");
                });
    }

    @Test
    void shouldFailInProductionWhenAllowedOriginsContainsPlaceholderShape() {
        withOverrides(Map.of(
                "spring.profiles.active", "prod",
                "blog.auth.allowed-origins[0]", "${BLOG_ADMIN_ALLOWED_ORIGINS}",
                "blog.auth.jwt.secret", "0123456789abcdef0123456789abcdef",
                "blog.auth.jwt.access-token-expire-minutes", "60"))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage("blog.auth.allowed-origins must be resolved in production");
                });
    }

    @Test
    void shouldFailWhenAllowedOriginsIsBlankOrWhitespaceOnly() {
        withOverrides(Map.of(
                "blog.auth.allowed-origins[0]", "   ",
                "blog.auth.jwt.secret", "0123456789abcdef0123456789abcdef",
                "blog.auth.jwt.access-token-expire-minutes", "60"))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage("blog.auth.allowed-origins must not be blank or empty");
                });
    }

    @Test
    void shouldFailInProductionWhenAllowedOriginsContainsWildcard() {
        withOverrides(Map.of(
                "spring.profiles.active", "prod",
                "blog.auth.allowed-origins[0]", "*",
                "blog.auth.jwt.secret", "0123456789abcdef0123456789abcdef",
                "blog.auth.jwt.access-token-expire-minutes", "60"))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage("blog.auth.allowed-origins must not allow wildcard origins in production");
                });
    }

    @Test
    void shouldAllowValidAuthConfiguration() {
        withOverrides(Map.of(
                "blog.auth.jwt.secret", "0123456789abcdef0123456789abcdef",
                "blog.auth.jwt.access-token-expire-minutes", "60"))
                .run(context -> assertThat(context).hasNotFailed());
    }

    private ApplicationContextRunner withOverrides(Map<String, Object> overrides) {
        return contextRunner.withInitializer(context ->
                context.getEnvironment().getPropertySources()
                        .addFirst(new MapPropertySource("blogAuthOverrides", overrides)));
    }

    @Configuration
    @EnableConfigurationProperties(BlogAuthProperties.class)
    static class TestConfig {
        @Bean
        BlogAuthConfigurationValidator blogAuthConfigurationValidator(BlogAuthProperties properties,
                                                                      org.springframework.core.env.Environment environment) {
            return new BlogAuthConfigurationValidator(properties, environment);
        }
    }
}
