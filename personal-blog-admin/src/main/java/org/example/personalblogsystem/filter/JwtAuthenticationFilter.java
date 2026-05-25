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
import org.example.personalblogsystem.util.JwtUtil;
import org.example.personalblogsystem.utils.RedisCache;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;
    private final RedisCache redisCache;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService,
                                   RedisCache redisCache,
                                   JwtUtil jwtUtil,
                                   UserDetailsService userDetailsService) {
        this.jwtTokenService = jwtTokenService;
        this.redisCache = redisCache;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
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

        if (authenticateExistingLoginToken(token, request)) {
            return;
        }
        authenticateCourseToken(token, request);
    }

    private boolean authenticateExistingLoginToken(String token, HttpServletRequest request) {
        AdminAuthPrincipal principal;
        try {
            principal = jwtTokenService.parseAccessToken(token);
        } catch (RuntimeException exception) {
            return false;
        }

        LoginUser loginUser = redisCache.getCacheObject(LoginUser.redisKey(principal.getUserId()));
        if (loginUser == null) {
            SecurityContextHolder.clearContext();
            return true;
        }

        setAuthentication(loginUser, request);
        return true;
    }

    private void authenticateCourseToken(String token, HttpServletRequest request) {
        try {
            String username = jwtUtil.getUsernameFromToken(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (jwtUtil.validateToken(token, userDetails)) {
                setAuthentication(userDetails, request);
            }
        } catch (RuntimeException exception) {
            SecurityContextHolder.clearContext();
        }
    }

    private void setAuthentication(UserDetails userDetails, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        if (userDetails instanceof LoginUser loginUser) {
            if ("USER".equalsIgnoreCase(loginUser.getRoleCode())) {
                UserAuthContext.set(loginUser.toPrincipal());
            } else {
                AdminAuthContext.set(loginUser.toPrincipal());
            }
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
