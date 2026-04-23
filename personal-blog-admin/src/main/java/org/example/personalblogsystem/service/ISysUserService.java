package org.example.personalblogsystem.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.personalblogsystem.dto.SysUserCreateRequest;
import org.example.personalblogsystem.dto.SysUserPasswordResetRequest;
import org.example.personalblogsystem.dto.SysUserResponse;
import org.example.personalblogsystem.dto.SysUserStatusRequest;
import org.example.personalblogsystem.dto.SysUserUpdateRequest;
import org.example.personalblogsystem.entity.SysUser;

/**
 * <p>
 * 鐢ㄦ埛琛?鏈嶅姟绫?
 * </p>
 *
 * @author student
 * @since 2026-03-31
 */
public interface ISysUserService extends IService<SysUser> {

    SysUserResponse getUserById(Long id);

    Page<SysUserResponse> pageUsers(long current, long size, String keyword);

    SysUserResponse createUser(SysUserCreateRequest request);

    SysUserResponse updateUser(Long id, SysUserUpdateRequest request);

    SysUserResponse updateUserStatus(Long id, SysUserStatusRequest request);

    SysUserResponse resetPassword(Long id, SysUserPasswordResetRequest request);
}
