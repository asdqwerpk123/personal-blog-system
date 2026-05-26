package org.example.personalblogsystem.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * 课程登录兼容用 JWT 工具类，负责按用户名签发、解析和校验简化访问令牌。
 * 使用 JJWT 与 HMAC-SHA256 密钥完成签名验证，主要服务于保留的 /login 兼容接口。
 */
@Component
public class JwtUtil {

    /**
     * JWT 签名密钥原文，要求不少于 32 字节以满足 HS256 安全强度。
     */
    private final String secret;
    /**
     * 兼容令牌有效期，单位为毫秒。
     */
    private final long expiration;
    private final SecretKey secretKey;

    /**
     * 初始化 JWT 工具并校验签名密钥强度。
     *
     * @param secret 配置中的 JWT 签名密钥
     * @param expiration 令牌过期时间，单位为毫秒
     * @throws IllegalArgumentException 密钥为空或长度不足 32 字节时抛出
     */
    public JwtUtil(@Value("${jwt.secret:${blog.auth.jwt.secret:change-me-for-real-use-please-32-bytes}}") String secret,
                   @Value("${jwt.expiration:86400000}") long expiration) {
        if (!StringUtils.hasText(secret) || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("jwt.secret must be at least 32 bytes");
        }
        this.secret = secret;
        this.expiration = expiration;
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }


    /**
     * 根据 Spring Security 用户信息签发兼容 JWT。
     *
     * @param userDetails 已认证或已加载的用户详情
     * @return 以用户名为 subject 的 JWT 字符串
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(userDetails.getUsername());
    }


    /**
     * 根据用户名签发兼容 JWT。
     *
     * @param username JWT subject，对应系统用户名
     * @return 带签名和过期时间的 JWT 字符串
     */
    public String generateToken(String username) {
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + expiration);
        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiresAt)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }


    /**
     * 解析并校验 JWT 签名。
     *
     * @param token 客户端提交的 JWT 字符串
     * @return JWT 中的声明载荷
     * @throws JwtException 令牌格式非法、签名错误或过期时抛出
     * @throws IllegalArgumentException token 为空或参数非法时抛出
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


    /**
     * 从 JWT 中读取用户名。
     *
     * @param token 客户端提交的 JWT 字符串
     * @return JWT subject 中保存的用户名
     * @throws JwtException 令牌无法通过解析或校验时抛出
     */
    public String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 兼容旧调用命名，功能等同于 getUsernameFromToken。
     *
     * @param token 客户端提交的 JWT 字符串
     * @return JWT subject 中保存的用户名
     */
    public String extractUsername(String token) {
        return getUsernameFromToken(token);
    }


    /**
     * 校验令牌是否属于指定用户且尚未过期。
     *
     * @param token 客户端提交的 JWT 字符串
     * @param userDetails 数据库加载出的用户详情
     * @return 用户名匹配且令牌有效返回 true，否则返回 false
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            String username = getUsernameFromToken(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public String getSecret() {
        return secret;
    }

    public long getExpiration() {
        return expiration;
    }

    private boolean isTokenExpired(String token) {
        Date expirationDate = parseToken(token).getExpiration();
        return expirationDate.before(new Date());
    }
}
