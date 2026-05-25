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


@Component
public class JwtUtil {

    private final String secret;
    private final long expiration;
    private final SecretKey secretKey;

    public JwtUtil(@Value("${jwt.secret:${blog.auth.jwt.secret:change-me-for-real-use-please-32-bytes}}") String secret,
                   @Value("${jwt.expiration:86400000}") long expiration) {
        if (!StringUtils.hasText(secret) || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("jwt.secret must be at least 32 bytes");
        }
        this.secret = secret;
        this.expiration = expiration;
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }


    public String generateToken(UserDetails userDetails) {
        return generateToken(userDetails.getUsername());
    }


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


    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


    public String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }

    public String extractUsername(String token) {
        return getUsernameFromToken(token);
    }


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
