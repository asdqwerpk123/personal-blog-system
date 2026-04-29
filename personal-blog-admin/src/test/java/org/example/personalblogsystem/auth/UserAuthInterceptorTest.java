package org.example.personalblogsystem.auth;

import org.example.personalblogcommon.exception.BlogException;
import org.example.personalblogcommon.result.ResultCodeEnum;
import org.example.personalblogsystem.config.BlogAuthProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserAuthInterceptorTest {

    private UserAuthInterceptor interceptor;
    private JwtTokenService jwtTokenService;
    private ProtectedController protectedController;
    private RegisterController registerController;
    private Method protectedMethod;
    private Method registerMethod;

    @BeforeEach
    void setUp() throws Exception {
        UserAuthContext.clear();
        BlogAuthProperties properties = new BlogAuthProperties();
        properties.getJwt().setSecret("0123456789abcdef0123456789abcdef");
        properties.getJwt().setAccessTokenExpireMinutes(60);
        jwtTokenService = new JwtTokenService(properties);
        interceptor = new UserAuthInterceptor(jwtTokenService);
        protectedController = new ProtectedController();
        registerController = new RegisterController();
        protectedMethod = ProtectedController.class.getDeclaredMethod("profile");
        registerMethod = RegisterController.class.getDeclaredMethod("register");
    }

    @AfterEach
    void tearDown() {
        UserAuthContext.clear();
    }

    @Test
    void shouldRejectMissingBearerToken() {
        HandlerMethod handlerMethod = new HandlerMethod(protectedController, protectedMethod);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/user/profile");
        MockHttpServletResponse response = new MockHttpServletResponse();

        BlogException exception = assertThrows(BlogException.class, () -> interceptor.preHandle(request, response, handlerMethod));
        assertThat(exception.getCode()).isEqualTo(ResultCodeEnum.UNAUTHORIZED.getCode());
    }

    @Test
    void shouldRejectAdminRoleForUserEndpoint() {
        AdminAuthPrincipal principal = new AdminAuthPrincipal(2L, "admin_zhang", 2L, "ADMIN");
        String token = jwtTokenService.issueAccessToken(principal);

        HandlerMethod handlerMethod = new HandlerMethod(protectedController, protectedMethod);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/user/profile");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        BlogException exception = assertThrows(BlogException.class, () -> interceptor.preHandle(request, response, handlerMethod));
        assertThat(exception.getCode()).isEqualTo(ResultCodeEnum.UNAUTHORIZED.getCode());
    }

    @Test
    void shouldAllowUserRoleAndPopulateContext() throws Exception {
        AdminAuthPrincipal principal = new AdminAuthPrincipal(5L, "jerry", 3L, "USER");
        String token = jwtTokenService.issueAccessToken(principal);

        HandlerMethod handlerMethod = new HandlerMethod(protectedController, protectedMethod);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/user/profile");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, handlerMethod)).isTrue();
        assertThat(UserAuthContext.requireCurrentUser().getUserId()).isEqualTo(5L);
        assertThat(UserAuthContext.requireCurrentUser().getUserName()).isEqualTo("jerry");
        assertThat(UserAuthContext.requireCurrentUser().getRoleCode()).isEqualTo("USER");

        interceptor.afterCompletion(request, response, handlerMethod, null);

        assertThat(UserAuthContext.get()).isNull();
    }

    @Test
    void shouldAllowRegisterWithoutBearerToken() throws Exception {
        HandlerMethod handlerMethod = new HandlerMethod(registerController, registerMethod);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/user/auth/register");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, handlerMethod)).isTrue();
        assertThat(UserAuthContext.get()).isNull();
    }

    @Test
    void shouldAllowRegisterWithoutBearerTokenUnderContextPath() throws Exception {
        HandlerMethod handlerMethod = new HandlerMethod(registerController, registerMethod);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/personal-blog-system/user/auth/register");
        request.setContextPath("/personal-blog-system");
        request.setServletPath("/user/auth/register");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, handlerMethod)).isTrue();
        assertThat(UserAuthContext.get()).isNull();
    }

    @RestController
    @RequestMapping("/user")
    static class ProtectedController {

        @GetMapping("/profile")
        public String profile() {
            return "ok";
        }
    }

    @RestController
    static class RegisterController {

        @PostMapping("/user/auth/register")
        public String register() {
            return "ok";
        }
    }
}
