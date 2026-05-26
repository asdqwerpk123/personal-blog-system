package org.example.personalblogsystem.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.example.personalblogcommon.exception.BlogException;
import org.example.personalblogcommon.result.ResultCodeEnum;
import org.example.personalblogsystem.config.BlogAuthProperties;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 后台统一 JWT 令牌服务，负责签发、解析管理员和用户登录后的访问令牌。
 * 依赖 blog.auth.jwt 配置和 JJWT 组件，令牌中携带用户、角色等授权所需的核心声明。
 */
@Service
public class JwtTokenService {

    /**
     * JWT 中保存用户名的声明名称。
     */
    private static final String CLAIM_USER_NAME = "userName";
    /**
     * JWT 中保存角色主键的声明名称。
     */
    private static final String CLAIM_ROLE_ID = "roleId";
    /**
     * JWT 中保存角色编码的声明名称。
     */
    private static final String CLAIM_ROLE_CODE = "roleCode";

    private final BlogAuthProperties blogAuthProperties;
    private final AtomicReference<SecretKey> secretKeyRef = new AtomicReference<>();

    public JwtTokenService(BlogAuthProperties blogAuthProperties) {
        this.blogAuthProperties = blogAuthProperties;
    }

    /**
     * 按默认过期策略签发访问令牌。
     *
     * @param principal 当前登录主体，包含用户和角色信息
     * @return 可放入 Authorization Bearer 头的 JWT 字符串
     */
    public String issueAccessToken(AdminAuthPrincipal principal) {
        return issueAccessToken(principal, calculateAccessTokenExpiresAt());
    }

    /**
     * 按指定过期时间签发访问令牌。
     *
     * @param principal 当前登录主体，包含用户和角色信息
     * @param expiresAt 令牌失效时间
     * @return 可放入 Authorization Bearer 头的 JWT 字符串
     */
    public String issueAccessToken(AdminAuthPrincipal principal, Instant expiresAt) {
        return Jwts.builder()
                .subject(String.valueOf(principal.getUserId()))
                .claim(CLAIM_USER_NAME, principal.getUserName())
                .claim(CLAIM_ROLE_ID, principal.getRoleId())
                .claim(CLAIM_ROLE_CODE, principal.getRoleCode())
                .issuedAt(new Date())
                .expiration(Date.from(expiresAt))
                .signWith(resolveSecretKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 根据配置计算访问令牌过期时间。
     *
     * @return 当前时间加上 accessTokenExpireMinutes 后的时间点
     */
    public Instant calculateAccessTokenExpiresAt() {
        return Instant.now().plus(Duration.ofMinutes(blogAuthProperties.getJwt().getAccessTokenExpireMinutes()));
    }

    /**
     * 解析访问令牌并还原登录主体。
     *
     * @param token 客户端提交的 JWT 字符串
     * @return 令牌声明中还原出的用户主体信息
     * @throws BlogException 令牌非法、签名无效或已过期时抛出未认证异常
     */
    public AdminAuthPrincipal parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(resolveSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Number roleId = claims.get(CLAIM_ROLE_ID, Number.class);
            return new AdminAuthPrincipal(
                    Long.valueOf(claims.getSubject()),
                    claims.get(CLAIM_USER_NAME, String.class),
                    roleId == null ? null : roleId.longValue(),
                    claims.get(CLAIM_ROLE_CODE, String.class)
            );
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BlogException(ResultCodeEnum.UNAUTHORIZED);
        }
    }

    private SecretKey resolveSecretKey() {
        SecretKey existing = secretKeyRef.get();
        if (existing != null) {
            return existing;
        }

        /**
         * SecretKey 构建后缓存到 AtomicReference，避免每次签发或解析令牌都重复创建密钥对象。
         */
        SecretKey created = Keys.hmacShaKeyFor(blogAuthProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
        if (secretKeyRef.compareAndSet(null, created)) {
            return created;
        }
        return secretKeyRef.get();
    }
}
