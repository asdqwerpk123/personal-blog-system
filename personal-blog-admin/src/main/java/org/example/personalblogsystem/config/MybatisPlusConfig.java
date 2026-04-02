package org.example.personalblogsystem.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("org.example.personalblogsystem.mapper")
public class MybatisPlusConfig {
}