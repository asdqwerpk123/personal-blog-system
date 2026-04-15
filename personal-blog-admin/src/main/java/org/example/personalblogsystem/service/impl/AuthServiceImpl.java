package org.example.personalblogsystem.service.impl;

import org.example.personalblogsystem.dto.LoginRequest;
import org.example.personalblogsystem.dto.LoginUserQueryRow;
import org.example.personalblogsystem.dto.LoginUserResponse;
import org.example.personalblogsystem.mapper.SysUserMapper;
import org.example.personalblogsystem.service.IAuthService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class AuthServiceImpl implements IAuthService {

    private final SysUserMapper sysUserMapper;

    public AuthServiceImpl(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public LoginUserResponse login(LoginRequest request) {
        validateRequest(request);

        String userName = request.getUserName().trim();
        LoginUserQueryRow row = sysUserMapper.selectLoginUserByUserName(userName);
        if (row == null || !isEnabled(row.getUserStatus()) || !matchesPassword(request.getPassword(), row.getPasswordHash())) {
            throw new IllegalArgumentException("username or password is incorrect");
        }

        return toResponse(row);
    }

    private void validateRequest(LoginRequest request) {
        if (request == null || !StringUtils.hasText(request.getUserName())) {
            throw new IllegalArgumentException("userName must not be blank");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new IllegalArgumentException("password must not be blank");
        }
    }

    private boolean isEnabled(String userStatus) {
        return "ENABLED".equalsIgnoreCase(userStatus == null ? null : userStatus.trim());
    }

    private boolean matchesPassword(String rawPassword, String expectedHash) {
        return expectedHash != null && sha256Hex(rawPassword).equalsIgnoreCase(expectedHash.trim());
    }

    private String sha256Hex(String rawPassword) {
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

    private LoginUserResponse toResponse(LoginUserQueryRow row) {
        LoginUserResponse response = new LoginUserResponse();
        response.setId(row.getId());
        response.setUserName(row.getUserName());
        response.setNickName(row.getNickName());
        response.setEmail(row.getEmail());
        response.setPhone(row.getPhone());
        response.setAvatarUrl(row.getAvatarUrl());
        response.setIntroduction(row.getIntroduction());
        response.setRoleId(row.getRoleId());
        response.setRoleCode(row.getRoleCode());
        response.setRoleName(row.getRoleName());
        response.setUserStatus(row.getUserStatus());
        return response;
    }
}
