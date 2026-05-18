package org.example.personalblogsystem.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class PasswordHashService {

    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public PasswordHashService(BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    /**
     * Hash new passwords with BCrypt while keeping the existing password_hash column unchanged.
     */
    public String hash(String rawPassword) {
        if (!StringUtils.hasText(rawPassword)) {
            throw new IllegalArgumentException("password must not be blank");
        }
        return bCryptPasswordEncoder.encode(rawPassword);
    }

    /**
     * Match both new BCrypt hashes and legacy SHA-256 hashes already stored in sys_user.
     */
    public boolean matches(String rawPassword, String expectedHash) {
        if (!StringUtils.hasText(rawPassword) || !StringUtils.hasText(expectedHash)) {
            return false;
        }

        String hash = expectedHash.trim();
        if (isBCryptHash(hash)) {
            return bCryptPasswordEncoder.matches(rawPassword, hash);
        }
        return sha256(rawPassword).equalsIgnoreCase(hash);
    }

    private boolean isBCryptHash(String hash) {
        return hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$");
    }

    private String sha256(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                int unsigned = value & 0xff;
                if (unsigned < 0x10) {
                    builder.append('0');
                }
                builder.append(Integer.toHexString(unsigned));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
