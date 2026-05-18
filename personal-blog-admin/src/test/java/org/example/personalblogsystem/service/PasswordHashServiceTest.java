package org.example.personalblogsystem.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordHashServiceTest {

    private final PasswordHashService passwordHashService =
            new PasswordHashService(new BCryptPasswordEncoder());

    @Test
    void shouldHashNewPasswordsWithBCrypt() {
        String hash = passwordHashService.hash("123456");

        assertThat(hash).startsWith("$2");
        assertThat(passwordHashService.matches("123456", hash)).isTrue();
    }

    @Test
    void shouldStillMatchLegacySha256Hashes() {
        String legacySha256 = "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92";

        assertThat(passwordHashService.matches("123456", legacySha256)).isTrue();
        assertThat(passwordHashService.matches("bad-password", legacySha256)).isFalse();
    }
}
