package org.example.personalblogsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.personalblogsystem.auth.LoginUser;
import org.example.personalblogsystem.dto.LoginUserQueryRow;
import org.example.personalblogsystem.entity.SysRole;
import org.example.personalblogsystem.entity.SysUser;
import org.example.personalblogsystem.mapper.SysRoleMapper;
import org.example.personalblogsystem.mapper.SysUserMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;

    public UserDetailsServiceImpl(SysUserMapper sysUserMapper, SysRoleMapper sysRoleMapper) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
    }

    /**
     * Load an active user and its active role for Spring Security authentication.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (!StringUtils.hasText(username)) {
            throw new UsernameNotFoundException("username must not be blank");
        }

        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserName, username.trim())
                .eq(SysUser::getDeleted, false)
                .last("limit 1"));
        if (user == null) {
            throw new UsernameNotFoundException("user is not found");
        }

        SysRole role = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getId, user.getRoleId())
                .eq(SysRole::getDeleted, false)
                .last("limit 1"));
        if (role == null || !StringUtils.hasText(role.getRoleCode())) {
            throw new UsernameNotFoundException("user role is not found");
        }
        LoginUserQueryRow row = toLoginUserQueryRow(user, role);
        return new LoginUser(row);
    }

    private LoginUserQueryRow toLoginUserQueryRow(SysUser user, SysRole role) {
        LoginUserQueryRow row = new LoginUserQueryRow();
        row.setId(user.getId());
        row.setUserName(user.getUserName());
        row.setPasswordHash(user.getPasswordHash());
        row.setNickName(user.getNickName());
        row.setEmail(user.getEmail());
        row.setPhone(user.getPhone());
        row.setAvatarUrl(user.getAvatarUrl());
        row.setIntroduction(user.getIntroduction());
        row.setRoleId(user.getRoleId());
        row.setUserStatus(user.getUserStatus());
        row.setRoleCode(role.getRoleCode());
        row.setRoleName(role.getRoleName());
        return row;
    }
}
