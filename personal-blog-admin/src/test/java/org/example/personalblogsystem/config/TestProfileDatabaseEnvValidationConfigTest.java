package org.example.personalblogsystem.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TestProfileDatabaseEnvValidationConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestProfileDatabaseEnvValidationConfig.class)
            .withPropertyValues("spring.profiles.active=test");

    @Test
    void shouldFailWhenAllTestDatabaseEnvVarsAreMissing() {
        withEnvironmentOverrides(Map.of(
                "BLOG_DB_TEST_URL", "",
                "BLOG_DB_TEST_USERNAME", "",
                "BLOG_DB_TEST_PASSWORD", ""))
                .run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasMessageContaining("BLOG_DB_TEST_URL")
                    .hasMessageContaining("BLOG_DB_TEST_USERNAME")
                    .hasMessageContaining("BLOG_DB_TEST_PASSWORD");
        });
    }

    @Test
    void shouldFailWhenTestDatabaseEnvVarsArePartiallyMissing() {
        withEnvironmentOverrides(Map.of(
                "BLOG_DB_TEST_URL", "jdbc:mysql://localhost:3306/personal_blog_system_test",
                "BLOG_DB_TEST_USERNAME", "root",
                "BLOG_DB_TEST_PASSWORD", ""))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("BLOG_DB_TEST_PASSWORD")
                            .hasMessageNotContaining("BLOG_DB_TEST_URL, BLOG_DB_TEST_USERNAME, BLOG_DB_TEST_PASSWORD");
                });
    }

    @Test
    void shouldAllowContextWhenAllTestDatabaseEnvVarsArePresent() {
        withEnvironmentOverrides(Map.of(
                "BLOG_DB_TEST_URL", "jdbc:mysql://localhost:3306/personal_blog_system_test",
                "BLOG_DB_TEST_USERNAME", "root",
                "BLOG_DB_TEST_PASSWORD", "123456"))
                .run(context -> assertThat(context).hasNotFailed());
    }

    private ApplicationContextRunner withEnvironmentOverrides(Map<String, Object> overrides) {
        return contextRunner.withInitializer(context ->
                context.getEnvironment().getPropertySources()
                        .addFirst(new MapPropertySource("testProfileDatabaseEnvOverrides", overrides)));
    }
}
