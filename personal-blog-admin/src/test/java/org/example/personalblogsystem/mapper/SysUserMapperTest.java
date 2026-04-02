package org.example.personalblogsystem.mapper;

import org.example.personalblogsystem.entity.SysUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = "spring.profiles.active=test")
class SysUserMapperTest {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Test
    void shouldQueryExistingUser() {
        SysUser user = sysUserMapper.selectById(1L);
        assertNotNull(user);
        assertEquals("root", user.getUserName());
    }
}