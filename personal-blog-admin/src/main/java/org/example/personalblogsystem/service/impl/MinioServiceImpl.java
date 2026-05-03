package org.example.personalblogsystem.service.impl;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.example.personalblogsystem.config.MinioProperties;
import org.example.personalblogsystem.service.IMinioService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@ConditionalOnBean(MinioClient.class)
public class MinioServiceImpl implements IMinioService {

    private static final long MAX_UPLOAD_SIZE = 2L * 1024L * 1024L;
    private static final Map<String, String> ALLOWED_IMAGE_EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif"
    );

    private final MinioClient minioClient;

    private final MinioProperties minioProperties;

    @Override
    public String upload(MultipartFile file) {
        validateFile(file);

        try {
            String bucketName = minioProperties.getBucketName();

            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            if (!bucketExists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName)
                            .build()
                );
            }

            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String objectName = datePath + "/" + UUID.randomUUID()
                    + ALLOWED_IMAGE_EXTENSIONS.get(normalizeContentType(file.getContentType()));

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            return minioProperties.getEndpoint()
                    + "/"
                    + bucketName
                    + "/"
                    + objectName;

        } catch (Exception e) {
            throw new RuntimeException("文件上传 MinIO 失败：" + e.getMessage(), e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择图片文件");
        }
        if (file.getSize() > MAX_UPLOAD_SIZE) {
            throw new IllegalArgumentException("图片大小不能超过 2MB");
        }
        if (!ALLOWED_IMAGE_EXTENSIONS.containsKey(normalizeContentType(file.getContentType()))) {
            throw new IllegalArgumentException("不支持的图片格式");
        }
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
    }
}
