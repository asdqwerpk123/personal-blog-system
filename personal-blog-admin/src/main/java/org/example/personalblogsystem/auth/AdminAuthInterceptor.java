package org.example.personalblogsystem.auth;

import org.example.personalblogcommon.exception.BlogException;
import org.example.personalblogcommon.result.ResultCodeEnum;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 管理端 MVC 认证拦截器，负责解析后台接口中的 Bearer Token 并校验管理员角色。
 * 通过 JwtTokenService 还原登录主体，并把主体写入 AdminAuthContext 供业务层读取。
 */
@Component
public class AdminAuthInterceptor implements AsyncHandlerInterceptor {

    /**
     * 管理端请求读取访问令牌的请求头名称。
     */
    private static final String AUTHORIZATION_HEADER = "Authorization";
    /**
     * Bearer Token 的固定前缀。
     */
    private static final String BEARER_PREFIX = "Bearer ";
    /**
     * 管理端登录接口路径，拦截器会放行该公开接口。
     */
    private static final String ADMIN_LOGIN_PATH = "/admin/auth/login";

    private final JwtTokenService jwtTokenService;

    public AdminAuthInterceptor(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    /**
     * 在 Controller 执行前完成管理端身份校验。
     *
     * @param request 当前 HTTP 请求
     * @param response 当前 HTTP 响应
     * @param handler 即将执行的处理器
     * @return 校验通过返回 true，允许继续执行
     * @throws BlogException 缺少令牌、令牌非法或角色不是管理员时抛出未认证异常
     */
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

    /**
     * 请求结束后清理管理端 ThreadLocal 身份，避免线程复用导致身份串用。
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AdminAuthContext.clear();
    }

    /**
     * 异步请求开始后立即清理当前线程身份，防止异步调度复用旧上下文。
     */
    @Override
    public void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response, Object handler) {
        AdminAuthContext.clear();
    }

    private boolean isPublicAdminEndpoint(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && ADMIN_LOGIN_PATH.equals(resolvePathWithinApplication(request));
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

    private boolean isAdminRole(String roleCode) {
        return "SUPER_ADMIN".equalsIgnoreCase(roleCode) || "ADMIN".equalsIgnoreCase(roleCode);
    }
}
