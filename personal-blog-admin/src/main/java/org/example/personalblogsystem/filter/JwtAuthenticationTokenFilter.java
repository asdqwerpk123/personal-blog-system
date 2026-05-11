package org.example.personalblogsystem.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.personalblogsystem.auth.AdminAuthContext;
import org.example.personalblogsystem.auth.AdminAuthPrincipal;
import org.example.personalblogsystem.auth.JwtTokenService;
import org.example.personalblogsystem.auth.LoginUser;
import org.example.personalblogsystem.auth.UserAuthContext;
import org.example.personalblogsystem.utils.RedisCache;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;
    private final RedisCache redisCache;

    public JwtAuthenticationTokenFilter(JwtTokenService jwtTokenService, RedisCache redisCache) {
        this.jwtTokenService = jwtTokenService;
        this.redisCache = redisCache;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            authenticateFromBearerToken(request);
            filterChain.doFilter(request, response);
        } finally {
            AdminAuthContext.clear();
            UserAuthContext.clear();
        }
    }

    private void authenticateFromBearerToken(HttpServletRequest request) {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        String token = resolveBearerToken(request);
        if (token == null) {
            return;
        }

        AdminAuthPrincipal principal;
        try {
            principal = jwtTokenService.parseAccessToken(token);
        } catch (RuntimeException exception) {
            SecurityContextHolder.clearContext();
            return;
        }

        LoginUser loginUser = redisCache.getCacheObject(LoginUser.redisKey(principal.getUserId()));
        if (loginUser == null) {
            SecurityContextHolder.clearContext();
            return;
        }

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        if ("USER".equalsIgnoreCase(loginUser.getRoleCode())) {
            UserAuthContext.set(loginUser.toPrincipal());
        } else {
            AdminAuthContext.set(loginUser.toPrincipal());
        }
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (authorization == null || !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
