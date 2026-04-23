package org.example.personalblogsystem.auth;

import org.example.personalblogcommon.exception.BlogException;
import org.example.personalblogcommon.result.ResultCodeEnum;
import org.example.personalblogsystem.config.BlogAuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminAuthInterceptorTest {

    private AdminAuthInterceptor interceptor;
    private JwtTokenService jwtTokenService;
    private OpenController openController;
    private TypeAnnotatedController typeAnnotatedController;
    private MethodAnnotatedController methodAnnotatedController;
    private LoginController loginController;
    private Method openMethod;
    private Method typeAnnotatedMethod;
    private Method methodAnnotatedMethod;
    private Method loginMethod;

    @BeforeEach
    void setUp() throws Exception {
        AdminAuthContext.clear();
        BlogAuthProperties properties = new BlogAuthProperties();
        properties.getJwt().setSecret("0123456789abcdef0123456789abcdef");
        properties.getJwt().setAccessTokenExpireMinutes(60);
        jwtTokenService = new JwtTokenService(properties);
        interceptor = new AdminAuthInterceptor(jwtTokenService);
        openController = new OpenController();
        typeAnnotatedController = new TypeAnnotatedController();
        methodAnnotatedController = new MethodAnnotatedController();
        loginController = new LoginController();
        openMethod = OpenController.class.getDeclaredMethod("ping");
        typeAnnotatedMethod = TypeAnnotatedController.class.getDeclaredMethod("ping");
        methodAnnotatedMethod = MethodAnnotatedController.class.getDeclaredMethod("ping");
        loginMethod = LoginController.class.getDeclaredMethod("login");
    }

    @AfterEach
    void tearDown() {
        AdminAuthContext.clear();
    }

    @Test
    void shouldRejectMissingBearerToken() {
        HandlerMethod handlerMethod = new HandlerMethod(typeAnnotatedController, typeAnnotatedMethod);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/ping");
        MockHttpServletResponse response = new MockHttpServletResponse();

        BlogException exception = assertThrows(BlogException.class, () -> interceptor.preHandle(request, response, handlerMethod));
        assertThat(exception.getCode()).isEqualTo(ResultCodeEnum.UNAUTHORIZED.getCode());
    }

    @Test
    void shouldRejectMalformedBearerToken() {
        HandlerMethod handlerMethod = new HandlerMethod(typeAnnotatedController, typeAnnotatedMethod);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/ping");
        request.addHeader("Authorization", "Bearer not-a-jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();

        BlogException exception = assertThrows(BlogException.class, () -> interceptor.preHandle(request, response, handlerMethod));
        assertThat(exception.getCode()).isEqualTo(ResultCodeEnum.UNAUTHORIZED.getCode());
    }

    @Test
    void shouldRejectUserRoleForUserManagementEndpoint() {
        AdminAuthPrincipal principal = new AdminAuthPrincipal(7L, "tom", 3L, "USER");
        String token = jwtTokenService.issueAccessToken(principal);

        HandlerMethod handlerMethod = new HandlerMethod(typeAnnotatedController, typeAnnotatedMethod);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/user/1");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        BlogException exception = assertThrows(BlogException.class, () -> interceptor.preHandle(request, response, handlerMethod));
        assertThat(exception.getCode()).isEqualTo(ResultCodeEnum.UNAUTHORIZED.getCode());
    }

    @Test
    void shouldRejectUserRoleForAnyAdminEndpoint() {
        AdminAuthPrincipal principal = new AdminAuthPrincipal(7L, "tom", 3L, "USER");
        String token = jwtTokenService.issueAccessToken(principal);

        HandlerMethod handlerMethod = new HandlerMethod(typeAnnotatedController, typeAnnotatedMethod);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/tag/page");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        BlogException exception = assertThrows(BlogException.class, () -> interceptor.preHandle(request, response, handlerMethod));
        assertThat(exception.getCode()).isEqualTo(ResultCodeEnum.UNAUTHORIZED.getCode());
    }

    @Test
    void shouldRejectUnknownRoleForUserManagementEndpoint() {
        AdminAuthPrincipal principal = new AdminAuthPrincipal(7L, "ghost", 99L, "GUEST");
        String token = jwtTokenService.issueAccessToken(principal);

        HandlerMethod handlerMethod = new HandlerMethod(typeAnnotatedController, typeAnnotatedMethod);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/user/1");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        BlogException exception = assertThrows(BlogException.class, () -> interceptor.preHandle(request, response, handlerMethod));
        assertThat(exception.getCode()).isEqualTo(ResultCodeEnum.UNAUTHORIZED.getCode());
    }

    @Test
    void shouldAcceptLowercaseBearerSchemeForAnnotatedHandler() throws Exception {
        AdminAuthPrincipal principal = new AdminAuthPrincipal(7L, "root", 1L, "SUPER_ADMIN");
        String token = jwtTokenService.issueAccessToken(principal);

        HandlerMethod handlerMethod = new HandlerMethod(methodAnnotatedController, methodAnnotatedMethod);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/method-ping");
        request.addHeader("Authorization", "bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, handlerMethod)).isTrue();
        assertThat(AdminAuthContext.requireCurrentUser().getUserId()).isEqualTo(7L);
        assertThat(AdminAuthContext.requireCurrentUser().getUserName()).isEqualTo("root");
        interceptor.afterCompletion(request, response, handlerMethod, null);
        assertThat(AdminAuthContext.get()).isNull();
    }

    @Test
    void shouldRejectExpiredBearerToken() {
        AdminAuthPrincipal principal = new AdminAuthPrincipal(7L, "root", 1L, "SUPER_ADMIN");
        String token = jwtTokenService.issueAccessToken(principal, Instant.now().minusSeconds(1));

        HandlerMethod handlerMethod = new HandlerMethod(methodAnnotatedController, methodAnnotatedMethod);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/ping");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        BlogException exception = assertThrows(BlogException.class, () -> interceptor.preHandle(request, response, handlerMethod));
        assertThat(exception.getCode()).isEqualTo(ResultCodeEnum.UNAUTHORIZED.getCode());
    }

    @Test
    void shouldAllowTypeLevelAnnotatedHandlerAndClearPrincipalAfterCompletion() throws Exception {
        AdminAuthPrincipal principal = new AdminAuthPrincipal(7L, "root", 1L, "SUPER_ADMIN");
        String token = jwtTokenService.issueAccessToken(principal);

        HandlerMethod handlerMethod = new HandlerMethod(typeAnnotatedController, typeAnnotatedMethod);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/ping");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, handlerMethod)).isTrue();
        assertThat(AdminAuthContext.get()).isNotNull();
        assertThat(AdminAuthContext.requireCurrentUser().getUserId()).isEqualTo(7L);
        assertThat(AdminAuthContext.requireCurrentUser().getUserName()).isEqualTo("root");
        assertThat(AdminAuthContext.requireCurrentUser().getRoleId()).isEqualTo(1L);
        assertThat(AdminAuthContext.requireCurrentUser().getRoleCode()).isEqualTo("SUPER_ADMIN");

        interceptor.afterCompletion(request, response, handlerMethod, null);

        assertThat(AdminAuthContext.get()).isNull();
        BlogException exception = assertThrows(BlogException.class, AdminAuthContext::requireCurrentUser);
        assertThat(exception.getCode()).isEqualTo(ResultCodeEnum.UNAUTHORIZED.getCode());
    }

    @Test
    void shouldAllowMethodLevelAnnotatedHandler() throws Exception {
        AdminAuthPrincipal principal = new AdminAuthPrincipal(7L, "root", 1L, "SUPER_ADMIN");
        String token = jwtTokenService.issueAccessToken(principal);

        HandlerMethod handlerMethod = new HandlerMethod(methodAnnotatedController, methodAnnotatedMethod);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/method-ping");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, handlerMethod)).isTrue();
        assertThat(AdminAuthContext.requireCurrentUser().getUserId()).isEqualTo(7L);
        interceptor.afterCompletion(request, response, handlerMethod, null);
        assertThat(AdminAuthContext.get()).isNull();
    }

    @Test
    void shouldClearAuthContextWhenAsyncHandlingStarts() throws Exception {
        AdminAuthPrincipal principal = new AdminAuthPrincipal(7L, "root", 1L, "SUPER_ADMIN");
        String token = jwtTokenService.issueAccessToken(principal);

        HandlerMethod handlerMethod = new HandlerMethod(typeAnnotatedController, typeAnnotatedMethod);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/ping");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, handlerMethod)).isTrue();
        assertThat(AdminAuthContext.get()).isNotNull();

        interceptor.afterConcurrentHandlingStarted(request, response, handlerMethod);

        assertThat(AdminAuthContext.get()).isNull();
    }

    @Test
    void shouldRejectUnannotatedAdminHandlerWithoutBearerToken() {
        AdminAuthContext.clear();
        HandlerMethod handlerMethod = new HandlerMethod(openController, openMethod);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/open");
        MockHttpServletResponse response = new MockHttpServletResponse();

        BlogException exception = assertThrows(BlogException.class, () -> interceptor.preHandle(request, response, handlerMethod));
        assertThat(exception.getCode()).isEqualTo(ResultCodeEnum.UNAUTHORIZED.getCode());
        assertThat(AdminAuthContext.get()).isNull();
    }

    @Test
    void shouldAllowPostLoginWithoutBearerToken() {
        HandlerMethod handlerMethod = new HandlerMethod(loginController, loginMethod);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, handlerMethod)).isTrue();
        assertThat(AdminAuthContext.get()).isNull();
    }

    @Test
    void shouldAllowOptionsRequestWithoutBearerToken() {
        HandlerMethod handlerMethod = new HandlerMethod(openController, openMethod);
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/admin/open");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, handlerMethod)).isTrue();
        assertThat(AdminAuthContext.get()).isNull();
    }

    @RestController
    @AdminAuthenticated
    static class TypeAnnotatedController {

        @GetMapping("/admin/ping")
        public String ping() {
            return "pong";
        }
    }

    @RestController
    static class MethodAnnotatedController {

        @AdminAuthenticated
        @GetMapping("/admin/method-ping")
        public String ping() {
            return "pong";
        }
    }

    @RestController
    @RequestMapping("/admin")
    static class OpenController {

        @GetMapping("/open")
        public String ping() {
            return "open";
        }
    }

    @RestController
    static class LoginController {

        @PostMapping("/admin/auth/login")
        public String login() {
            return "ok";
        }
    }
}
