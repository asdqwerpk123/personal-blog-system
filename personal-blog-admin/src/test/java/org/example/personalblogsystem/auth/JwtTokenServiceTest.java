package org.example.personalblogsystem.auth;

import org.example.personalblogsystem.config.BlogAuthProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class JwtTokenServiceTest {

    @Test
    void constructorShouldNotEagerlyValidateOrBuildTheSigningKey() {
        BlogAuthProperties properties = new BlogAuthProperties();
        properties.getJwt().setSecret("too-short-secret");
        properties.getJwt().setAccessTokenExpireMinutes(60);

        assertThatCode(() -> new JwtTokenService(properties)).doesNotThrowAnyException();
    }

    @Test
    void issueAndParseShouldStillWorkWithValidConfiguration() {
        BlogAuthProperties properties = new BlogAuthProperties();
        properties.getJwt().setSecret("0123456789abcdef0123456789abcdef");
        properties.getJwt().setAccessTokenExpireMinutes(60);
        JwtTokenService service = new JwtTokenService(properties);

        AdminAuthPrincipal principal = new AdminAuthPrincipal(1L, "root", 1L, "SUPER_ADMIN");
        String token = assertDoesNotThrow(() -> service.issueAccessToken(principal));

        assertThatCode(() -> service.parseAccessToken(token)).doesNotThrowAnyException();
    }
}
