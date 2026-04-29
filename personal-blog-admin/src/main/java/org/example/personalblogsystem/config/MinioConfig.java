package org.example.personalblogsystem.config;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "minio", name = "enabled", havingValue = "true")
public class MinioConfig {

    private final MinioProperties minioProperties;

    @Bean
    public MinioClient minioClient() {
        validateMinioProperties();
        return MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
    }

    private void validateMinioProperties() {
        requireText(minioProperties.getEndpoint(), "minio.endpoint must not be blank when minio.enabled=true");
        requireText(minioProperties.getAccessKey(), "minio.accessKey must not be blank when minio.enabled=true");
        requireText(minioProperties.getSecretKey(), "minio.secretKey must not be blank when minio.enabled=true");
        requireText(minioProperties.getBucketName(), "minio.bucketName must not be blank when minio.enabled=true");
    }

    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(message);
        }
    }
}
