package org.example.personalblogsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.personalblogsystem.entity.SysUser;
import org.example.personalblogsystem.mapper.SysUserMapper;
import org.example.personalblogsystem.service.ISysUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author student
 * @since 2026-03-31
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    @Override
    public Page<SysUser> pageUsers(long current, long size, String keyword) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            queryWrapper.and(wrapper -> wrapper.like(SysUser::getUserName, keyword)
                    .or()
                    .like(SysUser::getNickName, keyword)
                    .or()
                    .like(SysUser::getEmail, keyword));
        }
        queryWrapper.orderByAsc(SysUser::getId);
        return page(new Page<>(current, size), queryWrapper);
    }
}
