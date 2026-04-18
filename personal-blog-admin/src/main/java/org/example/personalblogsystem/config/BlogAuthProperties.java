package org.example.personalblogsystem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "blog.auth")
public class BlogAuthProperties {

    private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:5173"));
    private Jwt jwt = new Jwt();

    @Data
    public static class Jwt {
        private String secret = "change-me-for-real-use-please-32-bytes";
        private long accessTokenExpireMinutes = 120;
    }
}
