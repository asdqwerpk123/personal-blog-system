package org.example.personalblogsystem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 博客认证配置属性，绑定 blog.auth 前缀下的跨域来源和 JWT 参数。
 * 供安全配置、JWT 服务和 MVC 跨域配置统一读取认证相关配置。
 */
@Data
@ConfigurationProperties(prefix = "blog.auth")
public class BlogAuthProperties {

    /**
     * 允许访问接口的前端来源列表。
     */
    private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:5173"));
    /**
     * JWT 签发和校验所需配置。
     */
    private Jwt jwt = new Jwt();

    /**
     * JWT 配置项，包含签名密钥和访问令牌有效期。
     */
    @Data
    public static class Jwt {
        /**
         * HMAC 签名密钥，生产环境必须替换默认值。
         */
        private String secret = "change-me-for-real-use-please-32-bytes";
        /**
         * 访问令牌有效期，单位为分钟。
         */
        private long accessTokenExpireMinutes = 120;
    }
}
