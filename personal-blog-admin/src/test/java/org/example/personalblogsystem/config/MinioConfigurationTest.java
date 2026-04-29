package org.example.personalblogsystem.config;

import io.minio.MinioClient;
import org.example.personalblogsystem.controller.FileController;
import org.example.personalblogsystem.service.IMinioService;
import org.example.personalblogsystem.service.impl.MinioServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

class MinioConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void shouldNotRegisterMinioUploadChainWhenDisabledOrMissing() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(MinioClient.class);
            assertThat(context).doesNotHaveBean(IMinioService.class);
            assertThat(context).doesNotHaveBean(FileController.class);
        });

        contextRunner
                .withPropertyValues("minio.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(MinioClient.class);
                    assertThat(context).doesNotHaveBean(IMinioService.class);
                    assertThat(context).doesNotHaveBean(FileController.class);
                });
    }

    @Test
    void shouldRegisterMinioUploadChainWhenEnabledAndConfigured() {
        contextRunner
                .withPropertyValues(
                        "minio.enabled=true",
                        "minio.endpoint=http://localhost:9000",
                        "minio.access-key=minioadmin",
                        "minio.secret-key=minioadmin",
                        "minio.bucket-name=personal-blog")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(MinioClient.class);
                    assertThat(context).hasSingleBean(IMinioService.class);
                    assertThat(context).hasSingleBean(FileController.class);
                });
    }

    @Configuration
    @EnableConfigurationProperties(MinioProperties.class)
    @Import({MinioConfig.class, MinioServiceImpl.class, FileController.class})
    static class TestConfig {
    }
}
