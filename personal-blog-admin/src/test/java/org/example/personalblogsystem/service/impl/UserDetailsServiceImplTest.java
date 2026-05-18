package org.example.personalblogsystem.service.impl;

import org.example.personalblogsystem.auth.LoginUser;
import org.example.personalblogsystem.entity.SysRole;
import org.example.personalblogsystem.entity.SysUser;
import org.example.personalblogsystem.mapper.SysRoleMapper;
import org.example.personalblogsystem.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserDetailsServiceImplTest {

    private final SysUserMapper sysUserMapper = mock(SysUserMapper.class);
    private final SysRoleMapper sysRoleMapper = mock(SysRoleMapper.class);
    private final UserDetailsServiceImpl userDetailsService =
            new UserDetailsServiceImpl(sysUserMapper, sysRoleMapper);

    @Test
    void shouldLoadEnabledUserAndConvertRoleCodeToAuthority() {
        SysUser user = user(2L, "admin_zhang", 2L);
        SysRole role = role(2L, "ADMIN", "admin");
        when(sysUserMapper.selectOne(any())).thenReturn(user);
        when(sysRoleMapper.selectOne(any())).thenReturn(role);

        UserDetails userDetails = userDetailsService.loadUserByUsername(" admin_zhang ");

        assertThat(userDetails).isInstanceOf(LoginUser.class);
        LoginUser loginUser = (LoginUser) userDetails;
        assertThat(loginUser.getId()).isEqualTo(2L);
        assertThat(loginUser.getUsername()).isEqualTo("admin_zhang");
        assertThat(loginUser.getPassword()).isEqualTo("encoded-password");
        assertThat(loginUser.getRoleCode()).isEqualTo("ADMIN");
        assertThat(loginUser.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
        verify(sysUserMapper).selectOne(any());
        verify(sysRoleMapper).selectOne(any());
    }

    @Test
    void shouldRejectUserWhenRoleIsDeletedOrMissing() {
        when(sysUserMapper.selectOne(any())).thenReturn(user(3L, "admin_li", 2L));
        when(sysRoleMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("admin_li"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("user role is not found");
    }

    private SysUser user(Long id, String userName, Long roleId) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUserName(userName);
        user.setPasswordHash("encoded-password");
        user.setNickName(userName);
        user.setRoleId(roleId);
        user.setUserStatus("ENABLED");
        user.setDeleted(false);
        return user;
    }

    private SysRole role(Long id, String roleCode, String roleName) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleCode(roleCode);
        role.setRoleName(roleName);
        role.setDeleted(false);
        return role;
    }
}
