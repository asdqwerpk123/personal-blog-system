package org.example.personalblogsystem.service;

import io.minio.MinioClient;
import org.example.personalblogsystem.config.MinioProperties;
import org.example.personalblogsystem.service.impl.MinioServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MinioServiceImplTest {

    @Test
    void shouldPreferPublicEndpointWhenReturningUploadedUrl() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);
        when(minioClient.bucketExists(any())).thenReturn(true);
        when(minioClient.putObject(any())).thenReturn(null);

        MinioProperties properties = new MinioProperties();
        properties.setEndpoint("http://minio:9000");
        properties.setPublicEndpoint("http://localhost:1900/");
        properties.setBucketName("personal-blog");

        MinioServiceImpl service = new MinioServiceImpl(minioClient, properties);

        String url = service.upload(new MockMultipartFile(
                "file",
                "cover.png",
                "image/png",
                new byte[]{(byte) 0x89, 'P', 'N', 'G'}));

        assertThat(url).startsWith("http://localhost:1900/personal-blog/");
        assertThat(url).doesNotContain("//personal-blog/");
    }

    @Test
    void shouldFallbackToEndpointWhenPublicEndpointIsBlank() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);
        when(minioClient.bucketExists(any())).thenReturn(true);
        when(minioClient.putObject(any())).thenReturn(null);

        MinioProperties properties = new MinioProperties();
        properties.setEndpoint("http://minio:9000/");
        properties.setPublicEndpoint(" ");
        properties.setBucketName("personal-blog");

        MinioServiceImpl service = new MinioServiceImpl(minioClient, properties);

        String url = service.upload(new MockMultipartFile(
                "file",
                "cover.png",
                "image/png",
                new byte[]{(byte) 0x89, 'P', 'N', 'G'}));

        assertThat(url).startsWith("http://minio:9000/personal-blog/");
        assertThat(url).doesNotContain("//personal-blog/");
    }
}
