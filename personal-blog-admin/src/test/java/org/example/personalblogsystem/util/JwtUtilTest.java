package org.example.personalblogsystem.util;

import org.example.personalblogsystem.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    @Test
    void shouldGenerateAndValidateTokenForUserDetails() {
        JwtUtil jwtUtil = new JwtUtil("your-32-character-secret-key-12345678", 86_400_000L);
        UserDetails userDetails = User.withUsername("root")
                .password("encoded")
                .authorities("ROLE_SUPER_ADMIN")
                .build();

        String token = jwtUtil.generateToken(userDetails);

        assertThat(token).isNotBlank();
        assertThat(jwtUtil.getUsernameFromToken(token)).isEqualTo("root");
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("root");
        assertThat(jwtUtil.validateToken(token, userDetails)).isTrue();
    }
}
