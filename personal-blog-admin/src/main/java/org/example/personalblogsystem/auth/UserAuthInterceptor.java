package org.example.personalblogsystem.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.personalblogcommon.exception.BlogException;
import org.example.personalblogcommon.result.ResultCodeEnum;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

/**
 * 用户端 MVC 认证拦截器，负责校验 /user/** 接口的 Bearer Token 和 USER 角色身份。
 * 解析成功后将主体写入 UserAuthContext，供用户端业务流程获取当前登录用户。
 */
@Component
public class UserAuthInterceptor implements AsyncHandlerInterceptor {

    /**
     * 用户端请求读取访问令牌的请求头名称。
     */
    private static final String AUTHORIZATION_HEADER = "Authorization";
    /**
     * Bearer Token 的固定前缀。
     */
    private static final String BEARER_PREFIX = "Bearer ";
    /**
     * 用户注册接口路径，属于公开接口。
     */
    private static final String USER_REGISTER_PATH = "/user/auth/register";
    /**
     * 用户登录接口路径，属于公开接口。
     */
    private static final String USER_LOGIN_PATH = "/user/auth/login";

    private final JwtTokenService jwtTokenService;

    public UserAuthInterceptor(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    /**
     * 在 Controller 执行前完成用户端身份校验。
     *
     * @param request 当前 HTTP 请求
     * @param response 当前 HTTP 响应
     * @param handler 即将执行的处理器
     * @return 校验通过返回 true，允许继续执行
     * @throws BlogException 缺少令牌、令牌非法或角色不是 USER 时抛出未认证异常
     */
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

    /**
     * 请求结束后清理用户端 ThreadLocal 身份，避免线程复用导致身份串用。
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserAuthContext.clear();
    }

    /**
     * 异步请求开始后立即清理当前线程身份，防止异步调度复用旧上下文。
     */
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
