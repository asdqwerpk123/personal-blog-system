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

@Service
public class JwtTokenService {

    private static final String CLAIM_USER_NAME = "userName";
    private static final String CLAIM_ROLE_ID = "roleId";
    private static final String CLAIM_ROLE_CODE = "roleCode";

    private final BlogAuthProperties blogAuthProperties;
    private final AtomicReference<SecretKey> secretKeyRef = new AtomicReference<>();

    public JwtTokenService(BlogAuthProperties blogAuthProperties) {
        this.blogAuthProperties = blogAuthProperties;
    }

    public String issueAccessToken(AdminAuthPrincipal principal) {
        return issueAccessToken(principal, calculateAccessTokenExpiresAt());
    }

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

    public Instant calculateAccessTokenExpiresAt() {
        return Instant.now().plus(Duration.ofMinutes(blogAuthProperties.getJwt().getAccessTokenExpireMinutes()));
    }

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

        SecretKey created = Keys.hmacShaKeyFor(blogAuthProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
        if (secretKeyRef.compareAndSet(null, created)) {
            return created;
        }
        return secretKeyRef.get();
    }
}
