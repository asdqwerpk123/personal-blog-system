package org.example.personalblogsystem.config;

import org.example.personalblogsystem.PersonalBlogSystemApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestProfileDatabaseEnvValidationIntegrationTest {

    @Test
    void shouldFailFastBeforeDatasourceInitializationWhenTestDatabaseEnvVarsAreMissing() {
        SpringApplication application = new SpringApplication(PersonalBlogSystemApplication.class);
        application.setAdditionalProfiles("test");
        application.addInitializers(context -> context.getEnvironment().getPropertySources().addFirst(
                new MapPropertySource("testProfileDatabaseEnvOverrides", Map.of(
                        "BLOG_DB_TEST_URL", "",
                        "BLOG_DB_TEST_USERNAME", "",
                        "BLOG_DB_TEST_PASSWORD", ""))));
        application.setDefaultProperties(Map.of(
                "spring.main.web-application-type", "none"));

        assertThatThrownBy(() -> application.run())
                .hasMessageContaining("BLOG_DB_TEST_URL")
                .hasMessageContaining("BLOG_DB_TEST_USERNAME")
                .hasMessageContaining("BLOG_DB_TEST_PASSWORD")
                .matches(throwable -> !stackTraceOf(throwable).contains("NullPointerException"))
                .matches(throwable -> !stackTraceOf(throwable).contains("Druid"));
    }

    private static String stackTraceOf(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }
}
