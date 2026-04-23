package org.example.personalblogsystem.auth;

import org.example.personalblogcommon.exception.BlogException;
import org.example.personalblogcommon.result.ResultCodeEnum;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AdminAuthInterceptor implements AsyncHandlerInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ADMIN_LOGIN_PATH = "/admin/auth/login";

    private final JwtTokenService jwtTokenService;

    public AdminAuthInterceptor(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isPublicAdminEndpoint(request)) {
            return true;
        }

        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (authorization == null || !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            throw new BlogException(ResultCodeEnum.UNAUTHORIZED);
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new BlogException(ResultCodeEnum.UNAUTHORIZED);
        }

        AdminAuthPrincipal principal = jwtTokenService.parseAccessToken(token);
        if (!isAdminRole(principal.getRoleCode())) {
            throw new BlogException(ResultCodeEnum.UNAUTHORIZED);
        }
        AdminAuthContext.set(principal);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AdminAuthContext.clear();
    }

    @Override
    public void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response, Object handler) {
        AdminAuthContext.clear();
    }

    private boolean isPublicAdminEndpoint(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod()) && ADMIN_LOGIN_PATH.equals(request.getRequestURI());
    }

    private boolean isAdminRole(String roleCode) {
        return "SUPER_ADMIN".equalsIgnoreCase(roleCode) || "ADMIN".equalsIgnoreCase(roleCode);
    }
}
