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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@ConditionalOnBean(MinioClient.class)
public class MinioServiceImpl implements IMinioService {

    private final MinioClient minioClient;

    private final MinioProperties minioProperties;

    @Override
    public String upload(MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                throw new RuntimeException("上传文件不能为空");
            }

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

            String originalFilename = file.getOriginalFilename();
            String suffix = "";

            if (originalFilename != null && originalFilename.contains(".")) {
                suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String objectName = datePath + "/" + UUID.randomUUID() + suffix;

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
}
