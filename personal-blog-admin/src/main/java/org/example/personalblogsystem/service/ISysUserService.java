package org.example.personalblogsystem.service;

import org.example.personalblogsystem.entity.SysUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * <p>
 * 用户表 服务类
 * </p>
 *
 * @author student
 * @since 2026-03-31
 */
public interface ISysUserService extends IService<SysUser> {

    Page<SysUser> pageUsers(long current, long size, String keyword);

}
