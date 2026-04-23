package org.example.personalblogsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.personalblogsystem.dto.LoginUserQueryRow;
import org.example.personalblogsystem.entity.SysUser;

public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("""
            select u.id,
                   u.user_name as userName,
                   u.password_hash as passwordHash,
                   u.nick_name as nickName,
                   u.email,
                   u.phone,
                   u.avatar_url as avatarUrl,
                   u.introduction,
                   u.role_id as roleId,
                   u.user_status as userStatus,
                   r.role_code as roleCode,
                   r.role_name as roleName
            from sys_user u
            inner join sys_role r on u.role_id = r.id
            where u.user_name = #{userName}
              and u.deleted = 0
              and r.deleted = 0
            limit 1
            """)
    LoginUserQueryRow selectLoginUserByUserName(@Param("userName") String userName);
}
