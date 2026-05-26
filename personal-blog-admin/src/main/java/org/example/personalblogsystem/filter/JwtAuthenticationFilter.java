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
import org.example.personalblogsystem.utils.JwtUtil;
import org.example.personalblogsystem.utils.RedisCache;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 请求认证过滤器，每个请求执行一次 Bearer Token 解析并写入 Spring Security 上下文。
 * 同时兼容后台登录令牌和课程设计保留的简化 JWT 令牌，依赖 Redis 保存的登录态和 UserDetailsService。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * HTTP 请求头中承载访问令牌的标准字段。
     */
    private static final String AUTHORIZATION_HEADER = "Authorization";
    /**
     * Bearer Token 的协议前缀。
     */
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

    /**
     * 从请求中恢复认证信息，并在请求结束后清理线程上下文。
     *
     * @param request 当前 HTTP 请求
     * @param response 当前 HTTP 响应
     * @param filterChain 后续过滤器链
     * @throws ServletException 过滤器链处理失败时抛出
     * @throws IOException 读写请求或响应失败时抛出
     */
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
