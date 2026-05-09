package org.example.personalblogsystem.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.personalblogcommon.exception.BlogException;
import org.example.personalblogcommon.result.ResultCodeEnum;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

@Component
public class UserAuthInterceptor implements AsyncHandlerInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_REGISTER_PATH = "/user/auth/register";
    private static final String USER_LOGIN_PATH = "/user/auth/login";

    private final JwtTokenService jwtTokenService;

    public UserAuthInterceptor(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isPublicUserEndpoint(request)) {
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
        if (!"USER".equalsIgnoreCase(principal.getRoleCode())) {
            throw new BlogException(ResultCodeEnum.UNAUTHORIZED);
        }
        UserAuthContext.set(principal);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserAuthContext.clear();
    }

    @Override
    public void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response, Object handler) {
        UserAuthContext.clear();
    }

    private boolean isPublicUserEndpoint(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String pathWithinApplication = resolvePathWithinApplication(request);
        return USER_REGISTER_PATH.equals(pathWithinApplication)
                || USER_LOGIN_PATH.equals(pathWithinApplication);
    }

    private String resolvePathWithinApplication(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        if (servletPath != null && !servletPath.isEmpty()) {
            return servletPath;
        }

        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && requestUri != null && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }
}
