package org.example.personalblogsystem.service.impl;

import org.example.personalblogsystem.auth.LoginUser;
import org.example.personalblogsystem.dto.LoginUserQueryRow;
import org.example.personalblogsystem.mapper.SysUserMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper sysUserMapper;

    public UserDetailsServiceImpl(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (!StringUtils.hasText(username)) {
            throw new UsernameNotFoundException("username must not be blank");
        }

        LoginUserQueryRow row = sysUserMapper.selectLoginUserByUserName(username.trim());
        if (row == null) {
            throw new UsernameNotFoundException("user is not found");
        }
        if (!StringUtils.hasText(row.getRoleCode())) {
            throw new UsernameNotFoundException("user role is not found");
        }
        return new LoginUser(row);
    }
}
