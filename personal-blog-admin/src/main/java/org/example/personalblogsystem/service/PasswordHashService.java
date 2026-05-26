package org.example.personalblogsystem.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 密码散列服务，统一处理新密码 BCrypt 加密和历史 SHA-256 密码兼容校验。
 * 作为 Spring Security PasswordEncoder 的底层实现，避免登录流程直接感知存量密码格式。
 */
@Service
public class PasswordHashService {

    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public PasswordHashService(BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    /**
     * 使用 BCrypt 生成新密码散列。
     *
     * @param rawPassword 用户提交的明文密码
     * @return 可存入 password_hash 字段的 BCrypt 散列值
     * @throws IllegalArgumentException 密码为空时抛出
     */
    public String hash(String rawPassword) {
        if (!StringUtils.hasText(rawPassword)) {
            throw new IllegalArgumentException("password must not be blank");
        }
        return bCryptPasswordEncoder.encode(rawPassword);
    }

    /**
     * 校验明文密码是否匹配 BCrypt 或历史 SHA-256 散列。
     *
     * @param rawPassword 用户提交的明文密码
     * @param expectedHash 数据库保存的密码散列
     * @return 密码匹配返回 true，否则返回 false
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
